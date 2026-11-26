package com.openminis.app.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.agent.PersonaStore
import com.openminis.app.ui.theme.ChatColors

/**
 * [T-chat-persona-switch] Bottom sheet for switching the active chat's persona
 * (Soul). Lists every persona from [PersonaStore.list] — the default (SOUL.md)
 * first, then user-created ones — with the current selection ticked. Picking a
 * row calls [ChatViewModel.setPersonaId] (which persists the choice and repoints
 * the chat header) and dismisses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPersonaPickerSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentId by viewModel.personaId.collectAsState()
    val personas = remember {
        runCatching { PersonaStore.list(context) }.getOrDefault(emptyList())
    }
    // The DB stores the default persona as NULL; resolve() maps it to the stable
    // default id, so comparing against DEFAULT_PERSONA_ID matches both shapes.
    val resolvedCurrent = currentId ?: PersonaStore.DEFAULT_PERSONA_ID

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.persona_picker_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChatColors.primaryText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = ChatColors.toolBorder, thickness = 0.5.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                personas.forEach { persona ->
                    val isSelected = persona.id == resolvedCurrent
                    val emoji = if (persona.icon.isNotEmpty() && !persona.icon.startsWith("data:")) {
                        persona.icon
                    } else {
                        null
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setPersonaId(persona.id)
                                onDismiss()
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        if (emoji != null) {
                            Text(
                                text = emoji,
                                fontSize = 18.sp,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = ChatColors.primaryText.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = persona.name.ifBlank { "Minis" },
                                fontSize = 15.sp,
                                color = ChatColors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (persona.isDefault) {
                                Text(
                                    text = stringResource(R.string.soul_persona_default_badge),
                                    fontSize = 12.sp,
                                    color = ChatColors.secondaryText,
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = ChatColors.primaryText,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
