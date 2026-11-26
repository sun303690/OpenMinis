package com.openminis.app.agent

import android.content.Context
import com.openminis.app.logging.AppLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * [T-android-multi-persona] More than one personality.
 *
 * Historically there was exactly ONE personality file, `SOUL.md`, so every
 * chat shared a single identity. This store turns that single file into a
 * SET of personas while keeping the old file working unchanged.
 *
 * ## Where things live
 *
 * ```
 * <filesDir>/minis-global/memory/SOUL.md          <- the DEFAULT persona (unchanged path)
 * <filesDir>/minis-global/memory/SOUL.icon.png    <- its icon sidecar (unchanged path)
 * <filesDir>/minis-global/memory/souls/<id>.md        <- every additional persona
 * <filesDir>/minis-global/memory/souls/<id>.icon.png <- its icon sidecar
 * <filesDir>/minis-global/memory/souls/index.json    <- order + which one is default
 * ```
 *
 * ## Why the default persona IS the old SOUL.md
 *
 * Compatibility, not nostalgia. `SOUL.md` is read and written by code paths
 * that know nothing about personas — `minis-config soul.*`, backup/restore of
 * the memory directory, users who open the file in the file browser, iOS's
 * sync. Making "default persona" a *second* file would have meant migrating
 * (and possibly losing) the user's existing body and icon at upgrade time,
 * and would leave two files that both claim to be the personality.
 *
 * So: the default persona keeps `SOUL.md` verbatim, additional personas get
 * their own files, and [SoulStore.load] / [SoulStore.save] with no persona id
 * still address `SOUL.md`. An upgrade is therefore a no-op for existing users
 * — nothing moves, nothing is rewritten, and a downgrade is equally safe.
 *
 * ## Backup / restore coverage
 *
 * `BackupExporter.exportMemory` walks `minis-global/memory` recursively and
 * `BackupImporter.importMemory` copies the whole `data/memory` tree back, so
 * the persona `.md` files under `souls/`, the icon sidecars and `index.json`
 * are all covered by the existing MEMORY category with no change to either
 * side.
 *
 * `MemoryRepository.getMemory` only lists TOP-LEVEL `*.md` files
 * (`memoryDir.listFiles()`), never subdirectories, so persona files never leak
 * into `memory_get` results.
 */
object PersonaStore {

    private const val TAG = "PersonaStore"

    /**
     * The id of the persona backed by `SOUL.md`. Stable forever: it is what a
     * session's persisted `persona_id` resolves to before personas existed,
     * and what every old build would have meant.
     */
    const val DEFAULT_PERSONA_ID = "default"

    /** Sub-directory of the memory dir holding the non-default personas. */
    const val SOULS_SUBDIR = "souls"
    private const val INDEX_FILE = "index.json"

    /** [T-bundled-personas] APK asset folder holding personas seeded on first launch. */
    private const val ASSET_DIR = "bundled_personas"

    /** Marker written after the bundled-persona seed runs, so a deleted
     *  bundled persona is never resurrected on the next launch. */
    private const val MARKER_FILE = ".bundled_personas_seeded"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Persisted shape of `souls/index.json`. */
    @Serializable
    data class PersonaIndex(
        val version: Int = 1,
        val defaultId: String = DEFAULT_PERSONA_ID,
        val order: List<String> = listOf(DEFAULT_PERSONA_ID),
    )

    /**
     * One entry of the persona set. [name] / [icon] are read from the persona
     * file's frontmatter rather than cached in the index, so a file edited by
     * hand (or restored from a backup) shows up correctly without a re-index.
     */
    data class Persona(
        val id: String,
        val name: String,
        /** data URI, emoji, or "" for the default sparkle. */
        val icon: String,
        val isDefault: Boolean,
    )

    fun soulsDir(context: Context): File =
        File(File(context.filesDir, SoulStore.MEMORY_SUBDIR), SOULS_SUBDIR)

    private fun indexFile(context: Context): File = File(soulsDir(context), INDEX_FILE)

    /**
     * File holding persona [id]'s content. The default persona is `SOUL.md`;
     * anything else is `souls/<id>.md`.
     *
     * [id] is used verbatim as a file name, so it is validated on the way in
     * ([sanitizeId]) — a persona id reaches here from persisted session rows
     * and from a restored backup, and an id like `../../etc/passwd` must not
     * be able to escape the directory.
     */
    fun personaFile(context: Context, id: String): File {
        val safe = sanitizeId(id)
        if (safe == DEFAULT_PERSONA_ID) return SoulStore.fileLocation(context)
        return File(soulsDir(context), "$safe.md")
    }

    /**
     * Reduce [raw] to a bare file-name-safe token. Anything that is not
     * `[A-Za-z0-9._-]` is dropped, and the result is capped, which is enough
     * to rule out `..`, `/` and control characters in one step.
     */
    fun sanitizeId(raw: String?): String {
        val cleaned = raw.orEmpty()
            .asSequence()
            .filter { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
            .joinToString("")
        return cleaned.take(64)
    }

    // -- Index ------------------------------------------------------------

    private fun readIndex(context: Context): PersonaIndex {
        val f = indexFile(context)
        if (f.isFile) {
            val parsed = runCatching {
                json.decodeFromString(PersonaIndex.serializer(), f.readText())
            }.getOrNull()
            if (parsed != null) return parsed
            AppLogger.warning(TAG, "persona index unreadable — rebuilding")
        }
        return PersonaIndex()
    }

    private fun writeIndex(context: Context, index: PersonaIndex) {
        runCatching {
            indexFile(context).parentFile?.mkdirs()
            indexFile(context).writeText(json.encodeToString(index))
        }.onFailure {
            AppLogger.warning(TAG, "persona index write failed: ${it.message}")
        }
    }

    /**
     * Create `souls/` and its index on first use. Safe on every launch: an
     * existing index is never overwritten.
     *
     * This is ALSO the legacy migration, and it is deliberately a no-op for
     * content: an install upgrading from the single-SOUL.md build already has
     * its personality in `SOUL.md`, which is where the default persona reads
     * it from. There is nothing to copy and therefore nothing to lose.
     */
    fun ensureReady(context: Context) {
        val dir = soulsDir(context)
        if (!indexFile(context).isFile) {
            // An install that already has persona files but no index (index
            // lost / partially restored) keeps them: seed the order from disk
            // instead of resetting to "default only", which would hide them.
            val onDisk = dir.listFiles()
                ?.filter { it.isFile && it.extension == "md" }
                ?.map { it.nameWithoutExtension }
                ?.map(::sanitizeId)
                ?.filter { it.isNotEmpty() }
                .orEmpty()
            val order = listOf(DEFAULT_PERSONA_ID) + onDisk.filter { it != DEFAULT_PERSONA_ID }
            writeIndex(context, PersonaIndex(order = order))
        }
        seedBundledPersonas(context)
    }

    /**
     * [T-bundled-personas] Seed the personas shipped inside the APK.
     *
     * Any `assets/bundled_personas/<id>.md` is copied to `souls/<id>.md` and
     * appended to the index on first launch after install/upgrade — the same
     * build-time mechanism `bundled_skills` uses. No Kotlin string editing:
     * drop a file into `src/android/app/src/main/assets/bundled_personas/` and
     * rebuild.
     *
     * Runs ONCE per install, guarded by a marker file, so deleting a bundled
     * persona in Settings is permanent — it is not resurrected on the next
     * launch. A bundled id that already has a file on disk (user edit, restore
     * from backup) is never overwritten.
     */
    private fun seedBundledPersonas(context: Context) {
        val dir = soulsDir(context)
        val marker = File(dir, MARKER_FILE)
        if (marker.isFile) return
        val entries = runCatching { context.assets.list(ASSET_DIR)?.toList().orEmpty() }
            .getOrDefault(emptyList())
        if (entries.isEmpty()) {
            // Nothing bundled — mark done so we do not scan assets every launch.
            runCatching { dir.mkdirs(); marker.writeText("1") }
            return
        }
        val idx = readIndex(context)
        val order = idx.order.toMutableList()
        var changed = false
        for (entry in entries) {
            if (!entry.endsWith(".md")) continue
            val id = sanitizeId(entry.removeSuffix(".md"))
            if (id.isEmpty() || id == DEFAULT_PERSONA_ID) continue
            val target = personaFile(context, id)
            if (!target.isFile) {
                val text = runCatching {
                    context.assets.open("$ASSET_DIR/$entry").bufferedReader().use { it.readText() }
                }.getOrNull()
                if (text.isNullOrBlank()) {
                    AppLogger.warning(TAG, "bundled persona '$entry' unreadable — skipped")
                    continue
                }
                val ok = runCatching {
                    target.parentFile?.mkdirs()
                    target.writeText(text)
                }.isSuccess
                if (!ok) {
                    AppLogger.warning(TAG, "bundled persona '$id' write failed")
                    continue
                }
            }
            if (id !in order) {
                order.add(id)
                changed = true
            }
        }
        if (changed) writeIndex(context, idx.copy(order = order))
        runCatching { dir.mkdirs(); marker.writeText("1") }
            .onFailure { AppLogger.warning(TAG, "persona seed marker write failed: ${it.message}") }
    }

    /**
     * The id used when no persona is chosen or the chosen one is gone.
     * Falls back to [DEFAULT_PERSONA_ID] rather than null so every caller gets
     * a usable identity — a session bound to a deleted persona behaves exactly
     * like one that never chose.
     */
    fun defaultId(context: Context): String {
        val idx = readIndex(context)
        val id = sanitizeId(idx.defaultId).ifEmpty { DEFAULT_PERSONA_ID }
        // A default pointing at a persona that no longer has a file (deleted
        // on another device, partially restored) must not leave every chat
        // identity-less: fall back to the built-in default.
        return if (id == DEFAULT_PERSONA_ID || personaFile(context, id).isFile) id
        else DEFAULT_PERSONA_ID
    }

    /**
     * Resolve a persisted persona id to one that can actually be loaded.
     * `null`, blank, unknown and deleted ids all resolve to the default.
     */
    fun resolve(context: Context, id: String?): String {
        val safe = sanitizeId(id)
        if (safe.isEmpty() || safe == DEFAULT_PERSONA_ID) return DEFAULT_PERSONA_ID
        return if (personaFile(context, safe).isFile) safe else defaultId(context)
    }

    /**
     * Every persona, default first. Files present on disk but missing from the
     * index are appended rather than dropped, so a persona whose index entry
     * was lost never disappears from the UI.
     */
    fun list(context: Context): List<Persona> {
        ensureReady(context)
        val idx = readIndex(context)
        val def = defaultId(context)
        val ordered = LinkedHashSet<String>()
        ordered.add(def)
        for (id in idx.order) {
            val safe = sanitizeId(id)
            if (safe.isNotEmpty()) ordered.add(safe)
        }
        // Anything on disk the index forgot about.
        soulsDir(context).listFiles()
            ?.filter { it.isFile && it.extension == "md" }
            ?.map { sanitizeId(it.nameWithoutExtension) }
            ?.filter { it.isNotEmpty() }
            ?.forEach { ordered.add(it) }
        return ordered.mapNotNull { id ->
            val file = SoulStore.load(context, id) ?: return@mapNotNull null
            Persona(
                id = id,
                name = file.metadata.name.ifBlank { SoulMetadata.DEFAULT.name },
                icon = file.metadata.icon,
                isDefault = id == def,
            )
        }
    }

    /**
     * Add a persona seeded with [SoulStore.DEFAULT_CONTENT] and return its new
     * stable id. The id is a UUID, so it survives renames, reorders and
     * restores — a session bound to it keeps pointing at the same persona.
     */
    fun create(context: Context, name: String = ""): String {
        ensureReady(context)
        val id = "p_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val seed = SoulMDParser.parse(SoulStore.DEFAULT_CONTENT)
        val chosen = name.trim().ifBlank { seed.metadata.name }
        SoulStore.save(context, id, seed.copy(metadata = seed.metadata.copy(name = chosen)))
        val idx = readIndex(context)
        writeIndex(context, idx.copy(order = idx.order + id))
        AppLogger.info(TAG, "created persona $id ('$chosen')")
        return id
    }

    /** Rename [id] (frontmatter `name` only — the body is untouched). */
    fun rename(context: Context, id: String, name: String) {
        val safe = sanitizeId(id)
        val file = SoulStore.load(context, safe) ?: return
        val trimmed = name.trim()
        SoulStore.save(
            context,
            safe,
            file.copy(metadata = file.metadata.copy(name = trimmed.ifBlank { SoulMetadata.DEFAULT.name })),
        )
    }

    /**
     * Delete [id]. The LAST remaining persona is never deleted — there must
     * always be an identity for a session to fall back to — and deleting the
     * default moves the default to [DEFAULT_PERSONA_ID] (or the first survivor
     * if `SOUL.md` itself went away).
     *
     * Returns false when the delete was refused.
     */
    fun delete(context: Context, id: String): Boolean {
        ensureReady(context)
        val safe = sanitizeId(id)
        val all = list(context)
        if (all.size <= 1) {
            AppLogger.info(TAG, "refusing to delete the last persona ($safe)")
            return false
        }
        val target = all.firstOrNull { it.id == safe } ?: return false
        if (safe == DEFAULT_PERSONA_ID) {
            // Deleting `SOUL.md` would break every consumer that still reads
            // that path (minis-config soul.*, an older build, a file the user
            // may have bookmarked). Reset it to the canonical default instead
            // of removing it, and hand the default flag to a survivor.
            SoulStore.save(context, DEFAULT_PERSONA_ID, SoulMDParser.parse(SoulStore.DEFAULT_CONTENT))
            val survivor = all.firstOrNull { it.id != DEFAULT_PERSONA_ID }?.id ?: DEFAULT_PERSONA_ID
            setDefault(context, survivor)
        } else {
            runCatching { personaFile(context, safe).delete() }
            runCatching { SoulStore.iconFileLocation(context, safe).delete() }
            val idx = readIndex(context)
            writeIndex(context, idx.copy(order = idx.order.filter { it != safe }))
            if (defaultId(context) == safe) setDefault(context, DEFAULT_PERSONA_ID)
        }
        AppLogger.info(TAG, "deleted persona $safe ('${target.name}')")
        return true
    }

    /** Make [id] the persona new and unbound sessions use. */
    fun setDefault(context: Context, id: String) {
        ensureReady(context)
        val safe = sanitizeId(id).ifEmpty { DEFAULT_PERSONA_ID }
        val idx = readIndex(context)
        val order = if (safe in idx.order) idx.order else idx.order + safe
        writeIndex(context, idx.copy(defaultId = safe, order = order))
        AppLogger.info(TAG, "default persona -> $safe")
    }
}
