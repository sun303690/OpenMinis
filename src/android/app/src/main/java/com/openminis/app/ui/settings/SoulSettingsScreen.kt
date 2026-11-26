package com.openminis.app.ui.settings

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.ui.components.MinisButton
import com.openminis.app.ui.components.MinisOutlinedButton
import com.openminis.app.ui.components.MinisTextButton
import com.openminis.app.ui.components.safeLaunchPicker
import com.openminis.app.agent.PersonaStore
import com.openminis.app.agent.SoulBodyStats
import com.openminis.app.agent.SoulIcon
import com.openminis.app.agent.SoulFile
import com.openminis.app.ui.components.SettingsRowDivider
import com.openminis.app.agent.SoulMDParser
import com.openminis.app.agent.SoulMetadata
import com.openminis.app.agent.SoulStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.withContext

/**
 * [T-soul-md] Settings page for editing SOUL.md. Mirrors iOS
 * `SoulSettingsView` (commit 6370d5a):
 *   - Header preview card showing `✨ [name]` + `[style]` (emoji is
 *     locked to ✨; the editable emoji field was removed to keep
 *     identity surface consistent across the app)
 *   - Identity fields: name, style, lang (Auto / Chinese / English)
 *   - Personality prompt (multiline) with a plain, non-blocking length
 *     counter (see [SoulStore.measure] — there is no cap any more)
 *   - [T-android-multi-persona] A "Personas" section: add / rename / delete /
 *     set-default over the whole set, with the file being edited chosen at the
 *     top of the page
 *   - "Restore Default" with confirmation dialog
 *   - "Save" writes the chosen persona via [SoulStore.save] and refreshes the
 *     in-memory metadata cache so chat bubble headers update without a
 *     restart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoulSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(SoulMetadata.DEFAULT.name) }
    var style by remember { mutableStateOf(SoulMetadata.DEFAULT.style) }
    var lang by remember { mutableStateOf(SoulMetadata.DEFAULT.lang) }
    var body by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    // Preserve the raw `emoji` field exactly as it appears in SOUL.md so
    // Save round-trips without clobbering a value the user may have set
    // on another device (or an older build). Not surfaced in the UI —
    // the identity emoji shown to the user is locked to ✨ everywhere
    // (see [SoulMetadata.displayEmoji]).
    var preservedEmoji by remember { mutableStateOf(SoulMetadata.DEFAULT.emoji) }
    // [T-android-soul-custom-icon] The user-settable identity icon: an emoji
    // literal, a data:image/png;base64 URI, or empty for the default sparkle.
    var icon by remember { mutableStateOf(SoulMetadata.DEFAULT.icon) }
    var showIconMenu by remember { mutableStateOf(false) }
    var showEmojiSheet by remember { mutableStateOf(false) }
    var iconError by remember { mutableStateOf<String?>(null) }

    // [T-android-multi-persona] Which persona the page is editing, plus the
    // set itself. `personaId` is the persisted stable id (never a display
    // name), so renaming the persona does not change what a chat session
    // points at.
    var personaId by remember { mutableStateOf(PersonaStore.DEFAULT_PERSONA_ID) }
    var personas by remember { mutableStateOf(listOf<PersonaStore.Persona>()) }
    var personaToRename by remember { mutableStateOf<String?>(null) }
    var personaToDelete by remember { mutableStateOf<PersonaStore.Persona?>(null) }
    var personaError by remember { mutableStateOf<String?>(null) }
    // A switch asked for while the current persona has unsaved edits: named,
    // so the confirm dialog can name what would be lost and then run it.
    var pendingPersonaSwitch by remember { mutableStateOf<String?>(null) }

    // [T-android-soul-save-in-appbar] What was loaded from disk, kept so
    // "has the user changed anything" is a comparison rather than a flag.
    //
    // A boolean set by every onValueChange would report dirty after a round
    // trip that lands back on the original text (type a character, delete it),
    // and would need a reset at each of the several places state is
    // reassigned — the load, the restore-default, and the save itself. A
    // snapshot compared by value cannot drift out of sync with the fields.
    var baseline by remember { mutableStateOf<SoulFile?>(null) }

    // [T-android-soul-custom-icon] Image import: decode → reject opaque →
    // centre-crop → 96px → PNG → base64 data URI. Runs off the main thread
    // because a full-resolution camera photo is expensive to decode and scan.
    val iconUnreadableMsg = stringResource(R.string.soul_icon_error_unreadable)
    val iconTooLargeMsg = stringResource(R.string.soul_icon_error_too_large)
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val bmp = runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
                if (bmp == null) {
                    SoulIcon.EncodeResult.Failure(SoulIcon.Rejection.UNREADABLE)
                } else {
                    SoulIcon.encode(bmp)
                }
            }
            when (result) {
                is SoulIcon.EncodeResult.Success -> icon = result.dataUri
                is SoulIcon.EncodeResult.Failure -> iconError = when (result.reason) {
                    SoulIcon.Rejection.TOO_LARGE -> iconTooLargeMsg
                    SoulIcon.Rejection.UNREADABLE -> iconUnreadableMsg
                }
            }
        }
    }

    // Initial load + (defensive) ensureExists. The Application-level call
    // already seeded on first run, but loading from a freshly cleared app
    // shouldn't crash.
    //
    // [T-android-multi-persona] Opening the page starts on the DEFAULT persona,
    // which is where an upgrade from the single-SOUL.md build lands: the file
    // the user already had, unchanged, now simply the default of a set of one.
    LaunchedEffect(Unit) {
        val (startId, set, parsed) = withContext(Dispatchers.IO) {
            SoulStore.ensureExists(context)
            val id = PersonaStore.defaultId(context)
            Triple(id, PersonaStore.list(context), SoulStore.load(context, id))
        }
        val effective = parsed ?: SoulMDParser.parse(SoulStore.DEFAULT_CONTENT)
        personaId = startId
        personas = set
        name = effective.metadata.name
        preservedEmoji = effective.metadata.emoji
        icon = effective.metadata.icon
        style = effective.metadata.style
        lang = effective.metadata.lang
        body = effective.body
        // [T-android-soul-save-in-appbar] Snapshot what disk holds, so the
        // dirty check starts from the loaded state rather than from the
        // pre-load defaults (which would read as "changed" immediately).
        //
        // Rebuilt through the same normalisation Save applies (ifBlank
        // fallbacks) rather than stored raw: a file whose `lang` is empty on
        // disk would otherwise compare unequal to the field state the instant
        // it loaded, and the screen would open already dirty.
        baseline = SoulFile(
            metadata = SoulMetadata(
                name = effective.metadata.name.ifBlank { SoulMetadata.DEFAULT.name },
                emoji = effective.metadata.emoji.ifBlank { SoulMetadata.DEFAULT.emoji },
                icon = effective.metadata.icon,
                style = effective.metadata.style,
                lang = effective.metadata.lang.ifBlank { SoulMetadata.DEFAULT.lang },
            ),
            body = effective.body,
        )
        loaded = true
    }

    // [T-android-soul-no-limit] A plain measurement for the counter under the
    // editor. It gates NOTHING: the body has no cap, so there is nothing for
    // Save to refuse and nothing for the prompt to drop.
    val bodyStats by remember(body) { derivedStateOf { SoulStore.measure(body) } }

    /**
     * Point every field at persona [id]. Reads the file on IO and reseeds the
     * dirty baseline from it, so switching never leaves the page believing the
     * freshly-loaded content is a user edit.
     */
    val applyPersonaSwitch: (String) -> Unit = { id ->
        personaId = id
        scope.launch {
            val parsed = withContext(Dispatchers.IO) {
                SoulStore.load(context, id) ?: SoulMDParser.parse(SoulStore.DEFAULT_CONTENT)
            }
            name = parsed.metadata.name
            preservedEmoji = parsed.metadata.emoji
            icon = parsed.metadata.icon
            style = parsed.metadata.style
            lang = parsed.metadata.lang
            body = parsed.body
            baseline = SoulFile(
                metadata = SoulMetadata(
                    name = parsed.metadata.name.ifBlank { SoulMetadata.DEFAULT.name },
                    emoji = parsed.metadata.emoji.ifBlank { SoulMetadata.DEFAULT.emoji },
                    icon = parsed.metadata.icon,
                    style = parsed.metadata.style,
                    lang = parsed.metadata.lang.ifBlank { SoulMetadata.DEFAULT.lang },
                ),
                body = parsed.body,
            )
        }
    }

    val currentFile = SoulFile(
        metadata = SoulMetadata(
            name = name.ifBlank { SoulMetadata.DEFAULT.name },
            emoji = preservedEmoji.ifBlank { SoulMetadata.DEFAULT.emoji },
            icon = icon,
            style = style,
            lang = lang.ifBlank { SoulMetadata.DEFAULT.lang },
        ),
        body = body,
    )
    val isDirty = loaded && baseline != null && currentFile != baseline
    var showDiscardDialog by remember { mutableStateOf(false) }

    val save: () -> Unit = {
        val id = personaId
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    SoulStore.save(context, id, currentFile)
                    // [T-android-multi-persona] Renaming is a save of the name
                    // field, so the set has to be re-read or the list would
                    // keep showing the old name.
                    personas = PersonaStore.list(context)
                    if (id == PersonaStore.DEFAULT_PERSONA_ID) SoulStore.refreshCache(context, id)
                }
                onBack()
            } catch (t: Throwable) {
                saveError = t.message ?: "save failed"
            }
        }
        Unit
    }

    // [T-android-soul-save-in-appbar] Leaving with unsaved edits asks first.
    // Routed through one lambda so the app bar's back arrow and the system
    // back gesture cannot disagree about whether the guard applies.
    val attemptBack: () -> Unit = {
        if (isDirty) showDiscardDialog = true else onBack()
    }
    BackHandler(enabled = isDirty) { showDiscardDialog = true }

    SettingsScaffold(
        title = stringResource(R.string.soul_settings_title),
        onBack = attemptBack,
        actions = {
            // Save lives in the app bar, where a top-level commit action
            // belongs and where it stays reachable without scrolling the
            // (long) prompt editor to the bottom.
            // [T-android-soul-no-limit] Enabled purely on "something changed".
            // The previous `&& !bodyLimitCheck.isOverLimit` disabled Save for a
            // long body, which left the user with edits they could neither
            // commit nor understand why they couldn't.
            MinisTextButton(
                onClick = save,
                enabled = loaded && isDirty,
            ) { Text(stringResource(R.string.soul_save)) }
        },
    ) {
        SettingsSection(
            header = stringResource(R.string.soul_section_preview),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // [T-android-soul-custom-icon] A bare glyph does not read as
                // tappable — reported on iOS review — so the icon sits on a
                // filled circle with a hairline border and a pencil badge.
                // The circle doubles as the backdrop for transparent PNGs,
                // which by definition have no background of their own.
                Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            )
                            .clickable { showIconMenu = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        SoulIconGlyph(icon = icon, sizeDp = 30.dp, emojiSp = 24.sp)
                        // Badge kept INSIDE the circle's bounds: on iOS an
                        // overhanging badge was clipped to a sliver by the
                        // enclosing button label.
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(2.5.dp),
                        )
                    }
                    SoulIconMenu(
                        expanded = showIconMenu,
                        hasIcon = icon.isNotEmpty(),
                        onDismiss = { showIconMenu = false },
                        onChooseEmoji = { showIconMenu = false; showEmojiSheet = true },
                        onChooseImage = {
                            showIconMenu = false
                            imagePicker.safeLaunchPicker(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                                context,
                                "SoulSettings",
                            )
                        },
                        onUseDefault = { showIconMenu = false; icon = "" },
                    )
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = name.ifBlank { "Minis" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (style.isNotBlank()) {
                        Text(
                            text = style,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // [T-android-multi-persona] The set itself. Placed between the preview
        // and the field editors: which persona is being edited decides what
        // every section below shows, so it has to be visible before them.
        PersonaSection(
            personas = personas,
            currentId = personaId,
            onSwitch = { id ->
                if (id == personaId) return@PersonaSection
                if (isDirty) {
                    // Switching personas would replace every field, which is a
                    // silent discard. Ask first — same guard as leaving.
                    pendingPersonaSwitch = id
                } else {
                    applyPersonaSwitch(id)
                }
            },
            onAdd = {
                scope.launch {
                    val newId = withContext(Dispatchers.IO) { PersonaStore.create(context) }
                    withContext(Dispatchers.IO) { PersonaStore.list(context) }.let { personas = it }
                    applyPersonaSwitch(newId)
                }
            },
            onRename = { personaToRename = it },
            onDelete = { personaToDelete = it },
            onSetDefault = { id ->
                scope.launch {
                    withContext(Dispatchers.IO) { PersonaStore.setDefault(context, id) }
                    personas = withContext(Dispatchers.IO) { PersonaStore.list(context) }
                }
            },
        )

        SettingsSection(
            header = stringResource(R.string.soul_section_identity),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.soul_field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                // Emoji field intentionally removed — identity emoji is
                // locked to ✨; only name / style / lang are editable.
                OutlinedTextField(
                    value = style,
                    onValueChange = { style = it },
                    label = { Text(stringResource(R.string.soul_field_style)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LangPicker(lang = lang, onLangChange = { lang = it })
            }
        }

        SettingsSection(
            header = stringResource(R.string.soul_section_personality),
            footer = stringResource(R.string.soul_personality_footer),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // [T-android-editor-keyboard-overlap] The page scrolls and
                // pads for the keyboard (SettingsScaffold); this field asks
                // it to keep the caret's line in view, not just the field.
                com.openminis.app.ui.components.DocumentEditorField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    placeholder = stringResource(R.string.soul_body_placeholder),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    outlined = true,
                    ownScroll = false,
                    minHeight = 240.dp,
                )
                Spacer(Modifier.height(6.dp))
                // [T-android-soul-no-limit] One neutral colour and one short
                // count. There is no "over limit" state to warn about, so the
                // red branch (and the "N / max M" text it carried) is gone —
                // leaving it would keep telling the user there is a ceiling.
                val warnColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
                val indicatorText: String = soulBodyCountTextAndroid(bodyStats)
                Text(
                    text = indicatorText,
                    fontSize = 12.sp,
                    color = warnColor,
                )
            }
        }

        SettingsSection {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MinisOutlinedButton(
                    onClick = { showRestoreDialog = true },
                    // [T-android-soul-save-in-appbar] Full width now that Save
                    // has moved to the app bar and this is the only button left
                    // in the row.
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.soul_restore_default)) }
            }
        }
    }

    // [T-android-soul-save-in-appbar] Unsaved-changes guard.
    //
    // Each button leads with the verdict and then names the consequence
    // ("OK, discard" / "Cancel, keep editing"), so the row scans correctly
    // whether the user reads the leading word or the trailing one — a bare
    // "Discard"/"Keep editing" pair reads fine but gives no cue about which
    // side is the safe one.
    //
    // Dismissing the dialog by tapping outside keeps the edits — the
    // conservative reading of an ambiguous gesture.
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.soul_discard_confirm_title)) },
            text = { Text(stringResource(R.string.soul_discard_confirm_body)) },
            confirmButton = {
                MinisTextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) { Text(stringResource(R.string.soul_discard_confirm)) }
            },
            dismissButton = {
                MinisTextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.soul_discard_cancel))
                }
            },
        )
    }

    // [T-android-multi-persona] Switching personas while the current one has
    // unsaved edits is a discard, so it asks — the same guard as leaving the
    // page, for the same reason.
    pendingPersonaSwitch?.let { target ->
        val label = personas.firstOrNull { it.id == target }?.name ?: target
        AlertDialog(
            onDismissRequest = { pendingPersonaSwitch = null },
            title = { Text(stringResource(R.string.soul_persona_switch_confirm_title)) },
            text = { Text(stringResource(R.string.soul_persona_switch_confirm_body, label)) },
            confirmButton = {
                MinisTextButton(onClick = {
                    pendingPersonaSwitch = null
                    applyPersonaSwitch(target)
                }) { Text(stringResource(R.string.soul_persona_switch_confirm)) }
            },
            dismissButton = {
                MinisTextButton(onClick = { pendingPersonaSwitch = null }) {
                    Text(stringResource(R.string.soul_discard_cancel))
                }
            },
        )
    }

    personaToRename?.let { id ->
        var draft by remember(id) {
            mutableStateOf(personas.firstOrNull { it.id == id }?.name.orEmpty())
        }
        AlertDialog(
            onDismissRequest = { personaToRename = null },
            title = { Text(stringResource(R.string.soul_persona_rename_title)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text(stringResource(R.string.soul_persona_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                MinisButton(onClick = {
                    val chosen = draft.trim()
                    val target = id
                    personaToRename = null
                    if (chosen.isEmpty()) return@MinisButton
                    scope.launch {
                        withContext(Dispatchers.IO) { PersonaStore.rename(context, target, chosen) }
                        personas = withContext(Dispatchers.IO) { PersonaStore.list(context) }
                        if (target == personaId) name = chosen
                    }
                }) { Text(stringResource(R.string.soul_save)) }
            },
            dismissButton = {
                MinisOutlinedButton(onClick = { personaToRename = null }) {
                    Text(stringResource(R.string.soul_cancel))
                }
            },
        )
    }

    personaToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { personaToDelete = null },
            title = { Text(stringResource(R.string.soul_persona_delete_title)) },
            text = { Text(stringResource(R.string.soul_persona_delete_body, p.name)) },
            confirmButton = {
                MinisButton(
                    onClick = {
                        val target = p.id
                        personaToDelete = null
                        scope.launch {
                            val removed = withContext(Dispatchers.IO) {
                                PersonaStore.delete(context, target)
                            }
                            if (!removed) {
                                // Only ever "the last persona" — say so rather
                                // than failing silently after a confirm tap.
                                personaError = context.getString(
                                    R.string.soul_persona_delete_last_body,
                                )
                                return@launch
                            }
                            val set = withContext(Dispatchers.IO) { PersonaStore.list(context) }
                            personas = set
                            if (target == personaId) {
                                applyPersonaSwitch(
                                    withContext(Dispatchers.IO) { PersonaStore.defaultId(context) },
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                MinisOutlinedButton(onClick = { personaToDelete = null }) {
                    Text(stringResource(R.string.soul_cancel))
                }
            },
        )
    }

    personaError?.let { err ->
        AlertDialog(
            onDismissRequest = { personaError = null },
            title = { Text(stringResource(R.string.soul_persona_delete_last_title)) },
            text = { Text(err) },
            confirmButton = {
                MinisButton(onClick = { personaError = null }) { Text(stringResource(R.string.soul_ok)) }
            },
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(stringResource(R.string.soul_restore_confirm_title)) },
            text = { Text(stringResource(R.string.soul_restore_confirm_body)) },
            confirmButton = {
                MinisButton(onClick = {
                    val parsed = SoulMDParser.parse(SoulStore.DEFAULT_CONTENT)
                    name = parsed.metadata.name
                    preservedEmoji = parsed.metadata.emoji
                    icon = parsed.metadata.icon
                    style = parsed.metadata.style
                    lang = parsed.metadata.lang
                    body = parsed.body
                    showRestoreDialog = false
                }) { Text(stringResource(R.string.soul_restore_default)) }
            },
            dismissButton = {
                MinisOutlinedButton(onClick = { showRestoreDialog = false }) {
                    Text(stringResource(R.string.soul_cancel))
                }
            },
        )
    }

    saveError?.let { err ->
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text(stringResource(R.string.soul_save_error_title)) },
            text = { Text(err) },
            confirmButton = {
                MinisButton(onClick = { saveError = null }) { Text(stringResource(R.string.soul_ok)) }
            },
        )
    }

    // [T-android-soul-custom-icon] Image rejection is explained after the
    // fact here. There is no longer a transparency requirement to state up
    // front — any image is accepted (see soul_icon_image_hint) — so the only
    // remaining rejections are "couldn't be read" and "too large", neither of
    // which can be predicted before the user picks a file.
    iconError?.let { err ->
        AlertDialog(
            onDismissRequest = { iconError = null },
            title = { Text(stringResource(R.string.soul_icon_error_title)) },
            text = { Text(err) },
            confirmButton = {
                MinisButton(onClick = { iconError = null }) { Text(stringResource(R.string.soul_ok)) }
            },
        )
    }

    if (showEmojiSheet) {
        SoulEmojiPickerSheet(
            current = if (SoulIcon.isDataUri(icon)) "" else icon,
            onDismiss = { showEmojiSheet = false },
            onPick = { chosen -> icon = chosen; showEmojiSheet = false },
        )
    }
}

/**
 * [T-android-soul-custom-icon] The icon itself, rendered the same way
 * everywhere it appears: a decoded bitmap, an emoji, or the default sparkle.
 *
 * The bitmap decode is `remember`ed on the icon string. Without that, the
 * chat header would re-decode base64 on every recomposition — and headers
 * recompose on every streaming tick.
 */
@Composable
internal fun SoulIconGlyph(
    icon: String,
    sizeDp: Dp,
    emojiSp: TextUnit,
    sparkleTint: Brush? = null,
) {
    val bitmap = remember(icon) { SoulIcon.decode(icon) }
    when {
        // Rounded rectangle, NOT a circle: at 18dp in the chat header a circle
        // eats the corners of a small avatar. Clipping here is also what makes
        // an opaque image acceptable at all — the refusal that used to guard
        // against "opaque rectangle reads as a broken tile" was a presentation
        // concern, and this is the presentation fix.
        bitmap != null -> Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(sizeDp)
                .clip(RoundedCornerShape(sizeDp * SoulIcon.CORNER_RADIUS_FRACTION)),
        )
        icon.isNotEmpty() -> Text(text = icon, fontSize = emojiSp)
        // Unset: byte-for-byte the previous sparkle, so a user who never
        // touches this sees no visual change at all.
        sparkleTint != null -> Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(sizeDp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(brush = sparkleTint, blendMode = BlendMode.SrcIn)
                },
        )
        else -> Text(text = SoulMetadata.DISPLAY_EMOJI, fontSize = emojiSp)
    }
}

@Composable
private fun SoulIconMenu(
    expanded: Boolean,
    hasIcon: Boolean,
    onDismiss: () -> Unit,
    onChooseEmoji: () -> Unit,
    onChooseImage: () -> Unit,
    onUseDefault: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.soul_icon_choose_emoji)) },
            onClick = onChooseEmoji,
        )
        DropdownMenuItem(
            text = {
                Column {
                    Text(stringResource(R.string.soul_icon_choose_image))
                    // Stated up front so the square crop isn't a surprise after
                    // picking. (There is no alpha requirement: opaque images are
                    // accepted and the rounded clip is what makes them look right.)
                    Text(
                        text = stringResource(R.string.soul_icon_image_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            onClick = onChooseImage,
        )
        // Only offered when there is something to clear.
        if (hasIcon) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.soul_icon_use_default)) },
                onClick = onUseDefault,
            )
        }
    }
}

/**
 * Two rows of suggestions, tap-to-fill, a live preview at render size, and a
 * free-form field that normalizes per keystroke.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoulEmojiPickerSheet(
    current: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var draft by remember { mutableStateOf(current) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.soul_icon_emoji_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            // Preview at the size the icon is actually drawn on the card.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = draft.ifEmpty { SoulMetadata.DISPLAY_EMOJI },
                    fontSize = 30.sp,
                )
            }
            Spacer(Modifier.height(20.dp))

            SoulIcon.SUGGESTED_EMOJI.chunked(8).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    row.forEach { e ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (e == draft) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                )
                                .clickable { draft = e },
                            contentAlignment = Alignment.Center,
                        ) { Text(text = e, fontSize = 22.sp) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = draft,
                // Normalized PER KEYSTROKE rather than validated on submit:
                // a second emoji replaces the first, and non-emoji input is
                // dropped silently instead of being accepted then rejected.
                onValueChange = { draft = SoulIcon.normalizeEmojiInput(it) },
                label = { Text(stringResource(R.string.soul_icon_emoji_field)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MinisOutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.soul_cancel))
                }
                MinisButton(
                    onClick = { onPick(draft) },
                    enabled = draft.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.soul_icon_set)) }
            }
        }
    }
}

/// [T-android-soul-no-limit] Render the counter — "812 words" or "1 240 chars"
/// depending on which unit [SoulStore.measure] picked for this body.
///
/// There is no second number any more: the old text was "N / max M", and the
/// `M` was the cap. Keeping the format while removing the cap would keep
/// asserting a ceiling that no longer exists, so the strings carry only the
/// measured amount.
@Composable
private fun soulBodyCountTextAndroid(stats: SoulBodyStats): String {
    if (stats.amount <= 0) return stringResource(R.string.soul_count_zero)
    return when (stats.unit) {
        SoulBodyStats.Unit.CHARS -> stringResource(R.string.soul_count_chars, stats.amount)
        SoulBodyStats.Unit.WORDS -> stringResource(R.string.soul_count_words, stats.amount)
    }
}

/**
 * [T-android-multi-persona] The persona set itself.
 *
 * One row per persona: tap to make it the one being edited, plus rename /
 * delete / set-default. The row the page is editing is marked by the filled
 * container rather than only by a checkmark — a checkmark alone is easy to
 * misread as "default", which is a different thing (a persona can be edited
 * without being the default, and be the default without being open here).
 *
 * Delete is offered on every row including the last one; refusing the last is
 * the store's job ([PersonaStore.delete] returns false), which surfaces as a
 * dialog saying why rather than as a disabled button the user has to guess
 * about.
 */
@Composable
private fun PersonaSection(
    personas: List<PersonaStore.Persona>,
    currentId: String,
    onSwitch: (String) -> Unit,
    onAdd: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: (PersonaStore.Persona) -> Unit,
    onSetDefault: (String) -> Unit,
) {
    SettingsSection(
        header = stringResource(R.string.soul_persona_section),
        footer = stringResource(R.string.soul_persona_section_footer),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            personas.forEachIndexed { index, persona ->
                val isCurrent = persona.id == currentId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Selecting a persona is the row's whole point; the
                        // trailing buttons are separate targets inside it.
                        .clickable { onSwitch(persona.id) }
                        .then(
                            if (isCurrent) {
                                Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SoulIconGlyph(icon = persona.icon, sizeDp = 28.dp, emojiSp = 20.sp)
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            text = persona.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (persona.isDefault) {
                            Text(
                                text = stringResource(R.string.soul_persona_default_badge),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (!persona.isDefault) {
                        MinisTextButton(onClick = { onSetDefault(persona.id) }) {
                            Text(stringResource(R.string.soul_persona_make_default))
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.soul_persona_rename_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onRename(persona.id) }
                            .padding(6.dp),
                    )
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDelete(persona) }
                            .padding(6.dp),
                    )
                }
                if (index < personas.lastIndex) SettingsRowDivider()
            }
            if (personas.isNotEmpty()) SettingsRowDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                MinisOutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.soul_persona_add))
                }
            }
        }
    }
}

/** [T-soul-lang-button-wrap] Side padding of the language buttons. */
internal val LANG_BUTTON_SIDE_PADDING = 8.dp

@Composable
private fun LangButtonLabel(label: String) {
    Text(label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LangPicker(lang: String, onLangChange: (String) -> Unit) {
    val options = listOf(
        "auto" to stringResource(R.string.soul_lang_auto),
        "zh" to stringResource(R.string.soul_lang_zh),
        "en" to stringResource(R.string.soul_lang_en),
    )
    val current = options.firstOrNull { it.first == lang } ?: options.first()
    // Render the picker as a simple labeled row of buttons. Three options
    // (auto / Chinese / English) fit comfortably without a dropdown — avoids
    // depending on material3 ExposedDropdownMenu, which has a fragile
    // alignment story across compose versions.
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.soul_field_lang),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        // [T-soul-lang-button-wrap] Three equal-width buttons leave little room
        // once the default 24dp side padding is taken: on narrow phones
        // "English" broke mid-word ("Englis" / "h"). Keep labels on one line
        // and use a tighter side padding.
        val padding = PaddingValues(horizontal = LANG_BUTTON_SIDE_PADDING, vertical = 8.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (key, label) ->
                if (key == current.first) {
                    MinisButton(
                        onClick = { onLangChange(key) },
                        modifier = Modifier.weight(1f),
                        contentPadding = padding,
                    ) { LangButtonLabel(label) }
                } else {
                    MinisOutlinedButton(
                        onClick = { onLangChange(key) },
                        modifier = Modifier.weight(1f),
                        contentPadding = padding,
                    ) { LangButtonLabel(label) }
                }
            }
        }
    }
}
