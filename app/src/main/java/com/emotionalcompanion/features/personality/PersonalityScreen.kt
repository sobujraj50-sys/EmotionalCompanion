package com.emotionalcompanion.features.personality

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emotionalcompanion.R
import com.emotionalcompanion.ai.personality.LanguagePreference
import com.emotionalcompanion.ai.personality.PersonalityEngine
import com.emotionalcompanion.ai.personality.PersonalitySettings
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.core.designsystem.components.LevelSlider

@Composable
fun PersonalityScreen(
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: PersonalityViewModel = viewModel(factory = PersonalityViewModel.factory(container)),
) {
    val p by viewModel.personality.collectAsStateWithLifecycle()
    var nickname by rememberSaveable { mutableStateOf<String?>(null) }
    var showInstruction by rememberSaveable { mutableStateOf(false) }

    // Load the saved nickname once into the text field.
    LaunchedEffect(p.nickname) { if (nickname == null) nickname = p.nickname }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(
                text = stringResource(R.string.personality_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Section(R.string.personality_section_address) {
            OutlinedTextField(
                value = nickname ?: "",
                onValueChange = { v ->
                    val clean = v.take(PersonalitySettings.NICKNAME_MAX)
                    nickname = clean
                    viewModel.update { it.copy(nickname = clean) }
                },
                label = { Text(stringResource(R.string.personality_nickname)) },
                placeholder = { Text(stringResource(R.string.personality_nickname_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.personality_nickname_note),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Section(R.string.personality_section_language) {
            Column(Modifier.selectableGroup()) {
                LanguagePreference.entries.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = p.language == lang,
                                onClick = { viewModel.update { it.copy(language = lang) } },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = p.language == lang, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(lang.labelRes()), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Section(R.string.personality_section_character) {
            LevelSlider(R.string.pref_affection, p.affection, { v -> viewModel.update { it.copy(affection = v) } })
            LevelSlider(R.string.pref_care, p.care, { v -> viewModel.update { it.copy(care = v) } })
            LevelSlider(R.string.pref_humor, p.humor, { v -> viewModel.update { it.copy(humor = v) } })
            LevelSlider(R.string.pref_romance, p.romance, { v -> viewModel.update { it.copy(romance = v) } })
            LevelSlider(R.string.pref_expressiveness, p.expressiveness, { v -> viewModel.update { it.copy(expressiveness = v) } })
            LevelSlider(R.string.pref_talkativeness, p.talkativeness, { v -> viewModel.update { it.copy(talkativeness = v) } })
            LevelSlider(R.string.pref_initiative, p.initiative, { v -> viewModel.update { it.copy(initiative = v) } })
            TextButton(onClick = {
                viewModel.resetCharacter()
                nickname = ""
            }) { Text(stringResource(R.string.personality_reset)) }
        }

        Section(R.string.personality_section_honesty) {
            Text(
                text = stringResource(R.string.personality_honesty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { showInstruction = !showInstruction }) {
                Text(stringResource(if (showInstruction) R.string.personality_hide_instruction else R.string.personality_show_instruction))
            }
            if (showInstruction) {
                Text(
                    text = PersonalityEngine.build(p),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(R.string.personality_applies_next),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@StringRes
private fun LanguagePreference.labelRes(): Int = when (this) {
    LanguagePreference.BanglaFirst -> R.string.language_bangla_first
    LanguagePreference.Mixed -> R.string.language_mixed
    LanguagePreference.EnglishFirst -> R.string.language_english_first
}
