package com.openminis.app.data.repository

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * One-shot extraction of build-time payloads shipped under `assets/`.
 *
 * Minis's sandbox (/var/minis) is where skills, plugins and the agent's shell
 * actually look for files, so a resource that is only inside the APK is not
 * usable by them. This copies two classes of payload on first launch:
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

    private data class Target(val asset: String, val dest: File, val asZip: Boolean)

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
        marker.writeText("ok")
        Log.i(TAG, "extracted ${t.asset} -> ${t.dest}")
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

    private fun unzipTo(input: java.io.InputStream, dest: File) {
        java.util.zip.ZipInputStream(input.buffered()).use { zip ->
            val root = dest.canonicalPath
            var entry = zip.nextEntry
            while (entry != null) {
                val out = File(dest, entry.name)
                // Zip-slip guard.
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
