package com.openminis.app.ui.chat

import android.content.Context
import com.openminis.app.agent.SoulStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * [T-chat-preset-prompts] User-editable preset catalogue.
 *
 * The JSON lives in `minis-global/memory/preset_prompts.json` ON PURPOSE:
 * `BackupExporter.exportMemory` walks that directory recursively and
 * `BackupImporter.importMemory` copies the whole tree back verbatim, so custom
 * presets, edits, deletions and order are all covered by the existing MEMORY
 * category with ZERO changes to either side (the same mechanism PersonaStore
 * relies on for `souls/`). `MemoryRepository.getMemory` only lists top-level
 * `*.md` files, so this JSON never leaks into memory_get results.
 *
 * Built-in seeds are never rewritten: edits are stored as [State.overrides]
 * keyed by `builtin_<index>`, deletions as hidden ids, so "restore defaults"
 * is simply deleting this file and an app upgrade that adds new built-ins
 * keeps working for existing users.
 */
object PresetPromptStore {

    private const val FILE_NAME = "preset_prompts.json"
    private const val BUILTIN_PREFIX = "builtin_"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class StoredPreset(
        val id: String,
        val emoji: String = "\uD83D\uDCA1",
        val title: String = "",
        val subtitle: String = "",
        val prompt: String = "",
        val builtIn: Boolean = false,
    )

    @Serializable
    private data class State(
        val version: Int = 1,
        /** Display order; ids missing from here are appended in seed order. */
        val order: List<String> = emptyList(),
        /** User-created entries. */
        val custom: List<StoredPreset> = emptyList(),
        /** Edited built-ins, keyed by builtin id. */
        val overrides: Map<String, StoredPreset> = emptyMap(),
        /** Built-in ids the user deleted (recoverable via restoreDefaults). */
        val hidden: Set<String> = emptySet(),
    )

    private fun file(context: Context): File =
        File(File(context.filesDir, SoulStore.MEMORY_SUBDIR), FILE_NAME)

    private fun read(context: Context): State {
        val f = file(context)
        if (!f.isFile) return State()
        return runCatching { json.decodeFromString(State.serializer(), f.readText()) }
            .getOrDefault(State())
    }

    private fun write(context: Context, state: State) {
        runCatching {
            val f = file(context)
            f.parentFile?.mkdirs()
            f.writeText(json.encodeToString(state))
        }
    }

    /** The built-in seeds with stable ids. */
    fun builtIns(): List<PresetPrompt> =
        ChatPresetPrompts.all.mapIndexed { i, p ->
            p.copy(id = "$BUILTIN_PREFIX$i", isBuiltIn = true)
        }

    private fun StoredPreset.toEntry() = PresetPrompt(
        emoji = emoji,
        title = title,
        subtitle = subtitle,
        prompt = prompt,
        id = id,
        isBuiltIn = builtIn,
    )

    /** Effective catalogue in display order, with overrides / hidden applied. */
    fun list(context: Context): List<PresetPrompt> {
        val state = read(context)
        val biById = builtIns().associateBy { it.id }
        val customById = state.custom.associateBy { it.id }
        val known = biById.keys + customById.keys
        // Preserve persisted order, then append anything new (e.g. built-ins
        // added by an app upgrade) so nothing silently disappears.
        val ordered = state.order.filter { it in known } +
            known.filter { it !in state.order }
        return ordered.mapNotNull { id ->
            if (id in state.hidden) return@mapNotNull null
            val stored = state.overrides[id] ?: customById[id] ?: run {
                val b = biById[id] ?: return@mapNotNull null
                StoredPreset(b.id, b.emoji, b.title, b.subtitle, b.prompt, true)
            }
            stored.toEntry()
        }
    }

    fun isCustomized(context: Context): Boolean = file(context).isFile

    /** Insert (blank id) or update an entry. */
    fun save(context: Context, entry: PresetPrompt) {
        val state = read(context)
        if (entry.id.isBlank()) {
            val id = "p_" + UUID.randomUUID().toString().replace("-", "").take(12)
            val added = StoredPreset(
                id, entry.emoji, entry.title, entry.subtitle, entry.prompt, false,
            )
            write(
                context, state.copy(
                    custom = state.custom + added,
                    order = (state.order + id).distinct(),
                ),
            )
            return
        }
        val stored = StoredPreset(
            entry.id, entry.emoji, entry.title, entry.subtitle, entry.prompt,
            entry.id.startsWith(BUILTIN_PREFIX),
        )
        if (entry.id.startsWith(BUILTIN_PREFIX)) {
            write(
                context, state.copy(
                    overrides = state.overrides + (entry.id to stored),
                    hidden = state.hidden - entry.id,
                ),
            )
        } else {
            write(
                context, state.copy(
                    custom = state.custom.map { if (it.id == entry.id) stored else it },
                    order = (state.order + entry.id).distinct(),
                ),
            )
        }
    }

    /** Built-ins are hidden (recoverable); user presets are removed outright. */
    fun delete(context: Context, id: String) {
        val state = read(context)
        write(
            context, if (id.startsWith(BUILTIN_PREFIX)) {
                state.copy(hidden = state.hidden + id)
            } else {
                state.copy(
                    custom = state.custom.filter { it.id != id },
                    order = state.order - id,
                )
            },
        )
    }

    fun reorder(context: Context, ids: List<String>) {
        write(context, read(context).copy(order = ids))
    }

    /** Drop every customization; back to the built-in seeds. */
    fun restoreDefaults(context: Context) {
        runCatching { file(context).delete() }
    }
}
