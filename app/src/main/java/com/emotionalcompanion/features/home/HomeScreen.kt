package com.emotionalcompanion.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emotionalcompanion.R
import com.emotionalcompanion.core.designsystem.AppIcons
import com.emotionalcompanion.core.designsystem.LocalReducedMotion
import com.emotionalcompanion.core.designsystem.components.AmplitudeWaveform
import com.emotionalcompanion.core.designsystem.components.VoiceOrb
import com.emotionalcompanion.core.designsystem.components.labelRes

@Composable
fun HomeScreen(
    onOpenVoiceStudio: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshTimeOfDay()
        onPauseOrDispose { }
    }
    HomeContent(state = state, onOpenVoiceStudio = onOpenVoiceStudio, onOpenSettings = onOpenSettings)
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onOpenVoiceStudio: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    val stateLabel = stringResource(state.conversation.labelRes())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header: app name + settings shortcut
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(state.timeOfDay.greetingRes),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            AssistChip(
                onClick = onOpenSettings,
                label = { Text(stringResource(state.connection.labelRes())) },
            )
        }

        // Orb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            VoiceOrb(
                state = state.conversation,
                amplitude = state.audioLevels.lastOrNull() ?: 0f,
                reducedMotion = reducedMotion,
                contentDescription = stringResource(R.string.orb_description, stateLabel),
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(0.85f),
            )
        }

        Text(
            text = stateLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        AmplitudeWaveform(
            levels = state.audioLevels,
            contentDescription = stringResource(
                if (state.audioLevels.isEmpty()) R.string.waveform_silent else R.string.waveform_active,
            ),
        )

        // Controls: speaker · mic · voice
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = { },
                enabled = state.voiceAvailable,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(AppIcons.VolumeUp, contentDescription = stringResource(R.string.control_speaker))
            }
            FilledIconButton(
                onClick = { },
                enabled = state.voiceAvailable,
                shape = CircleShape,
                modifier = Modifier.size(80.dp),
            ) {
                Icon(
                    AppIcons.Mic,
                    contentDescription = stringResource(R.string.control_mic_start),
                    modifier = Modifier.size(34.dp),
                )
            }
            FilledTonalIconButton(
                onClick = onOpenVoiceStudio,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(AppIcons.GraphicEq, contentDescription = stringResource(R.string.control_voice))
            }
        }
        if (!state.voiceAvailable) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.home_voice_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Recent conversation preview
        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.home_recent_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.home_recent_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.ai_notice),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
