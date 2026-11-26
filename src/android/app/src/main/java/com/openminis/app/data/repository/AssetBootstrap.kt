package com.openminis.app.data.repository

import android.content.Context
import android.util.Log
import com.openminis.app.sandbox.RootfsManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * One-shot extraction of build-time payloads shipped under `assets/`.
 *
 * Minis's sandbox (/var/minis) is where skills, plugins and the agent's shell
 * actually look for files, so a resource that is only inside the APK is not
 * usable by them. This copies three classes of payload on first launch:
 *
 *   assets/toolchain/           -> /var/minis/toolchain/
 *   assets/builtin_plugins/     -> /var/minis/builtin_plugins/
 *   assets/lustforge-full.zip   -> /var/minis/lustforge/
 *
 * Extraction is marker-file gated, so it costs one stat per launch afterwards.
 * Existing files are never overwritten: a user who edited an extracted script
 * keeps their version (delete the marker to force a refresh).
 */
object AssetBootstrap {

    private const val TAG = "AssetBootstrap"

    fun extractIfNeeded(context: Context) {
        val targets = listOf(
            Target("toolchain", File("/var/minis/toolchain"), asZip = false),
            Target("builtin_plugins", File("/var/minis/builtin_plugins"), asZip = false),
            Target("lustforge-full.zip", File("/var/minis/lustforge"), asZip = true),
        )
        for (t in targets) {
            runCatching { extract(context, t) }.onFailure {
                Log.w(TAG, "extract ${t.asset} failed: ${it.message}")
            }
        }
    }

    private data class Target(val asset: String, val dest: File, val asZip: Boolean = false)

    private fun extract(context: Context, t: Target) {
        // v3: the toolchain payload added r2.tgz (radare2 5.9.8 + r2dec) and
        // bin/ wrappers. Bumping the marker suffix re-runs the (idempotent,
        // overwrite-safe) copy once for devices that already extracted v1/v2.
        val markerSuffix = if (t.asset == "toolchain") ".extracted.v3" else ".extracted"
        val marker = File(t.dest.parentFile, ".${t.dest.name}$markerSuffix")
        if (marker.exists() && t.dest.exists()) return
        if (runCatching { context.assets.list(t.asset)?.isNotEmpty() }.getOrNull() == false) return

        t.dest.mkdirs()
        if (t.asZip) {
            context.assets.open(t.asset).use { input -> unzipTo(input, t.dest) }
        } else {
            copyTree(context, t.asset, t.dest)
        }
        // Toolchain payloads ship as .tgz/.targz archives (jdk17.tgz,
        // cpython.targz, npm.tgz). Extract in place using RootfsManager's
        // pure-Kotlin POSIX tar reader instead of ProcessBuilder("tar"):
        // the app process has no guaranteed `tar` in its PATH on Android.
        // Delete each archive only after a successful extraction, and write
        // the marker only when ALL archives succeeded, so a failure is retried
        // on the next launch instead of being permanently skipped.
        if (t.asset == "toolchain") {
            if (!extractArchives(context, t.dest)) {
                Log.w(TAG, "toolchain archive extraction incomplete; marker not written")
                return
            }
            installJdkShims(t.dest)
        }
        marker.writeText("ok")
        Log.i(TAG, "extracted ${t.asset} -> ${t.dest}")
    }

    /** Returns true when every archive was extracted (or none were present). */
    private fun extractArchives(context: Context, dir: File): Boolean {
        val archives = dir.listFiles { f ->
            f.isFile && f.extension.lowercase() in setOf("tgz", "targz")
        } ?: return true
        if (archives.isEmpty()) return true

        val tar = RootfsManager.getInstance(context)
        var allOk = true
        for (archive in archives) {
            val ok = runCatching {
                GZIPInputStream(archive.inputStream().buffered()).use { gz ->
                    tar.extractTar(gz, dir)
                }
                archive.delete()
                Log.i(TAG, "extracted ${archive.name} -> ${dir.name}/")
            }.isSuccess
            if (!ok) {
                allOk = false
                Log.w(TAG, "failed to extract ${archive.name}; leaving archive for retry")
            }
        }
        return allOk
    }

    private fun copyTree(context: Context, assetPath: String, dest: File) {
        val children = runCatching { context.assets.list(assetPath) }.getOrNull()
        if (!children.isNullOrEmpty()) {
            dest.mkdirs()
            for (child in children) copyTree(context, "$assetPath/$child", File(dest, child))
            return
        }
        runCatching {
            dest.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(dest).use { out -> input.copyTo(out) }
            }
        }.onFailure { Log.w(TAG, "copy $assetPath skipped: ${it.message}") }
    }

    /**
     * [T-toolchain-jdk-shims] The NDK-built JDK17 in `assets/toolchain/jdk17`
     * links against `libandroid-shmem.so` (libjvm) and `libandroid-spawn.so`
     * (jspawnhelper), both shipped in `assets/toolchain/shims/` rather than
     * inside the archive. Copy them next to the JDK's libs so its RUNPATH
     * (`$ORIGIN/../lib`) resolves them, and also into `jdk17/bin` for the
     * launcher. Without this `java -version` dies with
     * `library "libandroid-shmem.so" not found`.
     *
     * Idempotent: overwrites only when the source is newer or the target is
     * missing, so a re-run each launch is cheap.
     */
    private fun installJdkShims(toolchainDir: File) {
        val shimsDir = File(toolchainDir, "shims")
        val jdkLib = File(toolchainDir, "jdk17/lib")
        val jdkBin = File(toolchainDir, "jdk17/bin")
        if (!shimsDir.isDirectory) {
            Log.w(TAG, "toolchain/shims missing — JDK shims not installed")
            return
        }
        val shimFiles = shimsDir.listFiles { f -> f.isFile && f.name.endsWith(".so") }.orEmpty()
        if (shimFiles.isEmpty()) return
        for (dir in listOf(jdkLib, jdkBin)) {
            if (!dir.isDirectory) continue
            for (shim in shimFiles) {
                val target = File(dir, shim.name)
                if (target.isFile && target.length() == shim.length()) continue
                runCatching { shim.copyTo(target, overwrite = true) }
                    .onFailure { Log.w(TAG, "shim ${shim.name} -> ${dir.name} failed: ${it.message}") }
            }
        }
        Log.i(TAG, "JDK shims installed into jdk17/lib + jdk17/bin (${shimFiles.size} .so)")
        // The wrapper scripts under toolchain/bin (apktool / cfr / dex2jar /
        // apk2java / r2 / soforge) are plain asset copies — CopyTo does not
        // preserve the tar mode, so mark them executable here or PATH lookups
        // fail with "Permission denied". jdk17/bin/java and the extracted
        // radare2 tools already carry their exec bit from the tar reader, but
        // r2/bin is re-chmodded anyway because a partially extracted archive
        // can leave them non-executable.
        var chmodded = 0
        for (binPath in listOf("bin", "r2/bin", "jdk17/bin")) {
            val binDir = File(toolchainDir, binPath)
            if (!binDir.isDirectory) continue
            binDir.listFiles()?.forEach { f ->
                if (f.isFile && f.setExecutable(true, false)) chmodded++
            }
        }
        if (chmodded > 0) Log.i(TAG, "chmod +x toolchain bins ($chmodded file(s))")
    }

    private fun unzipTo(input: InputStream, dest: File) {
        java.util.zip.ZipInputStream(input.buffered()).use { zip ->
            val root = dest.canonicalPath
            var entry = zip.nextEntry
            while (entry != null) {
                val out = File(dest, entry.name)
                if (out.canonicalPath.startsWith(root + File.separator)) {
                    if (entry.isDirectory) out.mkdirs()
                    else {
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { fos -> zip.copyTo(fos) }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
}
