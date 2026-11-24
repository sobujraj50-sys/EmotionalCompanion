package com.emotionalcompanion.features.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emotionalcompanion.R
import com.emotionalcompanion.ai.gemini.ClassifiedError
import com.emotionalcompanion.ai.gemini.LiveError
import com.emotionalcompanion.ai.gemini.SessionEndReason
import com.emotionalcompanion.ai.gemini.TranscriptLine
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.core.designsystem.AppIcons
import com.emotionalcompanion.core.designsystem.LocalReducedMotion
import com.emotionalcompanion.core.designsystem.components.AmplitudeWaveform
import com.emotionalcompanion.core.designsystem.components.VoiceOrb
import com.emotionalcompanion.core.designsystem.components.labelRes
import com.emotionalcompanion.domain.conversation.ConnectionStatus
import com.emotionalcompanion.domain.conversation.ConversationState

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenVoiceStudio: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenApiConfig: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDisclosure by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startConversation() else viewModel.onMicPermissionDenied()
    }

    val startWithPermission: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startConversation() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    HomeContent(
        state = state,
        onMicClick = {
            when {
                state.active -> viewModel.stopConversation()
                !state.voiceAvailable -> onOpenApiConfig()
                !state.disclosureAccepted -> showDisclosure = true
                else -> startWithPermission()
            }
        },
        onSpeakerClick = viewModel::toggleSpeaker,
        onOrbClick = viewModel::interrupt,
        onDismissError = viewModel::dismissError,
        onOpenVoiceStudio = onOpenVoiceStudio,
        onOpenSettings = onOpenSettings,
        onOpenApiConfig = onOpenApiConfig,
    )

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text(stringResource(R.string.disclosure_title)) },
            text = { Text(stringResource(R.string.disclosure_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    viewModel.acceptDisclosure()
                    startWithPermission()
                }) { Text(stringResource(R.string.disclosure_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onMicClick: () -> Unit,
    onSpeakerClick: () -> Unit,
    onOrbClick: () -> Unit,
    onDismissError: () -> Unit,
    onOpenVoiceStudio: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenApiConfig: () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    val stateLabel = stringResource(state.conversation.labelRes())
    val speaking = state.conversation == ConversationState.Speaking

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
            AssistChip(onClick = onOpenApiConfig, label = { Text(stringResource(state.connection.labelRes())) })
        }

        // Orb: tap while the AI speaks to silence it.
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
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(0.85f)
                    .clickable(
                        enabled = speaking,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.orb_tap_to_interrupt),
                        onClick = onOrbClick,
                    ),
            )
        }

        Text(stateLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        if (state.active) {
            Text(
                text = stringResource(R.string.session_timer, formatClock(state.sessionSeconds), state.sessionMaxMinutes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (speaking) {
            Text(
                text = stringResource(R.string.orb_tap_to_interrupt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        AmplitudeWaveform(
            levels = state.audioLevels,
            contentDescription = stringResource(
                if (state.audioLevels.isEmpty()) R.string.waveform_silent else R.string.waveform_active,
            ),
        )

        // Controls: speaker · mic · voice
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconToggleButton(
                checked = state.speakerOn,
                onCheckedChange = { onSpeakerClick() },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    AppIcons.VolumeUp,
                    contentDescription = stringResource(
                        if (state.speakerOn) R.string.control_speaker_on else R.string.control_speaker_off,
                    ),
                )
            }
            FilledIconButton(
                onClick = onMicClick,
                shape = CircleShape,
                colors = if (state.active) {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    IconButtonDefaults.filledIconButtonColors()
                },
                modifier = Modifier.size(80.dp),
            ) {
                Icon(
                    imageVector = if (state.active) Icons.Filled.Close else AppIcons.Mic,
                    contentDescription = stringResource(
                        if (state.active) R.string.control_mic_stop else R.string.control_mic_start,
                    ),
                    modifier = Modifier.size(34.dp),
                )
            }
            FilledTonalIconButton(onClick = onOpenVoiceStudio, modifier = Modifier.size(56.dp)) {
                Icon(AppIcons.GraphicEq, contentDescription = stringResource(R.string.control_voice))
            }
        }

        Spacer(Modifier.height(12.dp))
        val hint = when {
            state.connection == ConnectionStatus.NotConfigured -> R.string.home_voice_needs_key
            state.active && state.halfDuplex -> R.string.home_half_duplex
            state.active -> R.string.home_mic_live
            else -> R.string.home_tap_to_talk
        }
        Text(
            text = stringResource(hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.usage_today, state.usedTodaySeconds / 60, state.dailyBudgetMinutes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )

        state.error?.let { error ->
            Spacer(Modifier.height(16.dp))
            ErrorCard(error = error, onDismiss = onDismissError)
        }
        if (state.error == null && !state.active) {
            state.endReason?.endReasonRes()?.let { res ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(res),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        TranscriptCard(lines = state.transcript)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.ai_notice),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TranscriptCard(lines: List<TranscriptLine>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.home_recent_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (lines.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_recent_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                lines.forEach { line ->
                    Text(
                        text = stringResource(
                            if (line.fromUser) R.string.transcript_you else R.string.transcript_companion,
                            line.text,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (line.fromUser) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
            Text(
                text = stringResource(R.string.transcript_not_saved),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorCard(error: ClassifiedError, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(error.error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            error.detail?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.error_detail, it),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.action_ok))
            }
        }
    }
}

private fun formatClock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

@StringRes
private fun LiveError.messageRes(): Int = when (this) {
    LiveError.NotConfigured -> R.string.live_error_not_configured
    LiveError.MicPermission -> R.string.live_error_mic_permission
    LiveError.MicUnavailable -> R.string.live_error_mic_unavailable
    LiveError.AudioBusy -> R.string.live_error_audio_busy
    LiveError.NoInternet -> R.string.api_result_no_internet
    LiveError.Timeout -> R.string.api_result_timeout
    LiveError.KeyRejected -> R.string.api_result_key_rejected
    LiveError.PermissionDenied -> R.string.api_result_permission
    LiveError.QuotaExceeded -> R.string.api_result_quota
    LiveError.ModelUnavailable -> R.string.api_result_model
    LiveError.InvalidSetup -> R.string.live_error_invalid_setup
    LiveError.ServerError -> R.string.api_result_server
    LiveError.ConnectionLost -> R.string.live_error_connection_lost
    LiveError.Unknown -> R.string.live_error_unknown
}

@StringRes
private fun SessionEndReason.endReasonRes(): Int? = when (this) {
    SessionEndReason.SessionLimit -> R.string.end_session_limit
    SessionEndReason.DailyBudget -> R.string.end_daily_budget
    SessionEndReason.Idle -> R.string.end_idle
    SessionEndReason.AudioFocusLost -> R.string.end_focus_lost
    SessionEndReason.AppInBackground -> R.string.end_background
    SessionEndReason.UserStopped, SessionEndReason.Error -> null
}
