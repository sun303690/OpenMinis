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
        val marker = File(t.dest.parentFile, ".${t.dest.name}.extracted")
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
