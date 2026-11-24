package com.emotionalcompanion.features.voicestudio

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emotionalcompanion.R
import com.emotionalcompanion.ai.gemini.LiveError
import com.emotionalcompanion.ai.gemini.TestDialogue
import com.emotionalcompanion.ai.gemini.VoiceConfiguration
import com.emotionalcompanion.ai.gemini.VoiceOption
import com.emotionalcompanion.core.audio.OfflineSpeech
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.core.designsystem.components.LevelSlider

@Composable
fun VoiceStudioScreen(
    container: AppContainer,
    onOpenPersonality: () -> Unit,
    viewModel: VoiceStudioViewModel = viewModel(factory = VoiceStudioViewModel.factory(container)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voices = if (state.showAllVoices) VoiceConfiguration.all else VoiceConfiguration.candidates

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.voice_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Test line
        Section(R.string.voice_section_test_line) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TestDialogue.entries.forEach { d ->
                    FilterChip(
                        selected = d == state.dialogue,
                        onClick = { viewModel.setDialogue(d) },
                        label = { Text(stringResource(d.labelRes())) },
                    )
                }
            }
            Text(
                text = state.dialogue.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Voices
        Section(R.string.voice_section_voices) {
            if (state.conversationActive) {
                Text(
                    text = stringResource(R.string.voice_busy_conversation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Column(Modifier.selectableGroup()) {
                voices.forEach { voice ->
                    VoiceRow(
                        voice = voice,
                        selected = voice.name == state.selectedVoice,
                        connecting = state.audition.voiceName == voice.name && state.audition.connecting,
                        playing = state.audition.voiceName == voice.name && state.audition.playing,
                        canAudition = !state.conversationActive,
                        onSelect = { viewModel.selectVoice(voice.name) },
                        onListen = { viewModel.audition(voice.name) },
                        onStop = viewModel::stopAudition,
                    )
                }
            }
            TextButton(onClick = viewModel::toggleShowAll) {
                Text(stringResource(if (state.showAllVoices) R.string.voice_show_fewer else R.string.voice_show_all))
            }
            state.audition.error?.let { err ->
                Text(
                    text = stringResource(err.error.auditionErrorRes()) + (err.detail?.let { "\n$it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = stringResource(R.string.voice_quota_note),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Delivery preferences
        Section(R.string.voice_section_delivery) {
            val p = state.personality
            LevelSlider(R.string.pref_warmth, p.warmth, { v -> viewModel.updateDelivery { it.copy(warmth = v) } })
            LevelSlider(R.string.pref_softness, p.softness, { v -> viewModel.updateDelivery { it.copy(softness = v) } })
            LevelSlider(R.string.pref_energy, p.energy, { v -> viewModel.updateDelivery { it.copy(energy = v) } })
            LevelSlider(R.string.pref_pace, p.pace, { v -> viewModel.updateDelivery { it.copy(pace = v) } })
            Text(
                text = stringResource(R.string.voice_delivery_note),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Personality
        Section(R.string.voice_section_personality) {
            Text(
                text = stringResource(R.string.voice_personality_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onOpenPersonality,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.voice_open_personality)) }
        }

        // Offline phone voice
        Section(R.string.voice_section_offline) {
            Text(
                text = stringResource(R.string.voice_offline_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = viewModel::playOffline,
                enabled = state.offline != OfflineSpeech.Availability.Unavailable,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.voice_offline_play)) }
            val note = when {
                state.offline == OfflineSpeech.Availability.Unavailable -> R.string.voice_offline_unavailable
                state.offline == OfflineSpeech.Availability.NoBangla -> R.string.voice_offline_no_bangla
                state.offlineFailed -> R.string.voice_offline_no_bangla
                else -> null
            }
            note?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VoiceRow(
    voice: VoiceOption,
    selected: Boolean,
    connecting: Boolean,
    playing: Boolean,
    canAudition: Boolean,
    onSelect: () -> Unit,
    onListen: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(voice.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = voice.descriptor + if (voice.name == VoiceConfiguration.DEFAULT_VOICE) " · " + stringResource(R.string.voice_default) else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            connecting -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            playing -> TextButton(onClick = onStop) { Text(stringResource(R.string.voice_stop)) }
            else -> TextButton(onClick = onListen, enabled = canAudition) { Text(stringResource(R.string.voice_listen)) }
        }
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
private fun TestDialogue.labelRes(): Int = when (this) {
    TestDialogue.Bangla -> R.string.dialogue_bangla
    TestDialogue.English -> R.string.dialogue_english
    TestDialogue.Mixed -> R.string.dialogue_mixed
}

@StringRes
private fun LiveError.auditionErrorRes(): Int = when (this) {
    LiveError.NotConfigured -> R.string.live_error_not_configured
    LiveError.BudgetReached -> R.string.end_daily_budget
    LiveError.NoInternet -> R.string.api_result_no_internet
    LiveError.Timeout -> R.string.api_result_timeout
    LiveError.KeyRejected -> R.string.api_result_key_rejected
    LiveError.QuotaExceeded -> R.string.api_result_quota
    LiveError.InvalidSetup -> R.string.voice_error_rejected
    LiveError.AudioBusy -> R.string.live_error_audio_busy
    else -> R.string.voice_error_generic
}
