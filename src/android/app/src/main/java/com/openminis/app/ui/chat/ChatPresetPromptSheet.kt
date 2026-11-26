package com.openminis.app.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.ui.theme.ChatColors

/**
 * [T-chat-preset-prompts] Bottom sheet opened from the composer "+" menu.
 *
 * Picking a row inserts the FULL prompt into the composer WITHOUT sending.
 * "Manage" switches to edit mode: built-ins and custom entries can all be
 * edited or deleted, new ones added, order changed by long-press drag (with
 * arrow buttons as a reliable fallback for gesture/scroll conflicts), and
 * everything reverted with "Restore defaults". Persistence + backup coverage
 * live in [PresetPromptStore].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPresetPromptSheet(
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf(PresetPromptStore.list(context)) }
    var editor by remember { mutableStateOf<PresetPrompt?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }

    fun reload() {
        items = PresetPromptStore.list(context)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.preset_picker_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChatColors.primaryText,
                    )
                    Text(
                        text = stringResource(
                            if (editing) R.string.preset_edit_hint else R.string.preset_picker_hint,
                        ),
                        fontSize = 12.sp,
                        color = ChatColors.secondaryText,
                    )
                }
                TextButton(onClick = {
                    editing = !editing
                    reload()
                }) {
                    Text(
                        stringResource(
                            if (editing) R.string.preset_done else R.string.preset_manage,
                        ),
                    )
                }
            }
            HorizontalDivider(color = ChatColors.toolBorder, thickness = 0.5.dp)

            val rowHeightPx = with(LocalDensity.current) { 72.dp.toPx() }

            fun move(id: String, delta: Int) {
                val from = items.indexOfFirst { it.id == id }
                if (from < 0) return
                val to = (from + delta).coerceIn(0, items.size - 1)
                if (to == from) return
                val next = items.toMutableList()
                val moved = next.removeAt(from)
                next.add(to, moved)
                items = next
                PresetPromptStore.reorder(context, next.map { it.id })
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (items.isEmpty()) {
                    Text(
                        text = stringResource(R.string.preset_empty),
                        fontSize = 13.sp,
                        color = ChatColors.secondaryText,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    )
                }
                items.forEach { preset ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (editing) {
                                    Modifier
                                        .height(72.dp)
                                        .pointerInput(preset.id) {
                                            var totalY = 0f
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { totalY = 0f },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    totalY += dragAmount.y
                                                },
                                                onDragEnd = {
                                                    val steps = (totalY / rowHeightPx).toInt()
                                                    if (steps != 0) move(preset.id, steps)
                                                },
                                                onDragCancel = { totalY = 0f },
                                            )
                                        }
                                } else {
                                    Modifier.clickable {
                                        onPick(preset.prompt)
                                        onDismiss()
                                    }
                                },
                            )
                            .padding(horizontal = 20.dp, vertical = if (editing) 8.dp else 12.dp),
                    ) {
                        Text(
                            text = preset.emoji,
                            fontSize = 20.sp,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = ChatColors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.size(2.dp))
                            Text(
                                text = preset.subtitle,
                                fontSize = 12.sp,
                                color = ChatColors.secondaryText,
                                maxLines = if (editing) 1 else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (editing) {
                            IconButton(
                                onClick = { move(preset.id, -1) },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.cd_move_up),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(
                                onClick = { move(preset.id, 1) },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.cd_move_down),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(
                                onClick = { editor = preset },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.cd_edit),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    PresetPromptStore.delete(context, preset.id)
                                    reload()
                                },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.cd_delete),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        color = ChatColors.toolBorder.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                    )
                }
            }

            if (editing) {
                HorizontalDivider(color = ChatColors.toolBorder, thickness = 0.5.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp),
                ) {
                    TextButton(onClick = {
                        editor = PresetPrompt(
                            emoji = "\uD83D\uDCA1", title = "", subtitle = "",
                            prompt = "", id = "", isBuiltIn = false,
                        )
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.preset_add))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { confirmRestore = true }) {
                        Text(stringResource(R.string.preset_restore_defaults))
                    }
                }
            }
        }
    }

    editor?.let { initial ->
        PresetEditorDialog(
            initial = initial,
            onDismiss = { editor = null },
            onSave = { entry ->
                PresetPromptStore.save(context, entry)
                reload()
                editor = null
            },
        )
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(stringResource(R.string.preset_restore_title)) },
            text = { Text(stringResource(R.string.preset_restore_message)) },
            confirmButton = {
                TextButton(onClick = {
                    PresetPromptStore.restoreDefaults(context)
                    reload()
                    confirmRestore = false
                }) { Text(stringResource(R.string.preset_restore_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun PresetEditorDialog(
    initial: PresetPrompt,
    onSave: (PresetPrompt) -> Unit,
    onDismiss: () -> Unit,
) {
    var emoji by remember { mutableStateOf(initial.emoji) }
    var title by remember { mutableStateOf(initial.title) }
    var subtitle by remember { mutableStateOf(initial.subtitle) }
    var prompt by remember { mutableStateOf(initial.prompt) }
    val untitled = stringResource(R.string.preset_untitled)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial.id.isBlank()) R.string.preset_editor_new
                    else R.string.preset_editor_edit,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text(stringResource(R.string.preset_field_emoji)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.preset_field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text(stringResource(R.string.preset_field_subtitle)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(stringResource(R.string.preset_field_prompt)) },
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    initial.copy(
                        emoji = emoji.ifBlank { "\uD83D\uDCA1" },
                        title = title.ifBlank { untitled },
                        subtitle = subtitle,
                        prompt = prompt,
                    ),
                )
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
