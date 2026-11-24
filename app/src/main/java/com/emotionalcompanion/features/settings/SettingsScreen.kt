package com.emotionalcompanion.features.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emotionalcompanion.BuildConfig
import com.emotionalcompanion.R
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.core.designsystem.LocalReducedMotion
import com.emotionalcompanion.core.designsystem.components.VoiceOrb
import com.emotionalcompanion.core.designsystem.components.labelRes
import com.emotionalcompanion.data.settings.ThemeMode
import com.emotionalcompanion.domain.conversation.ConversationState

@Composable
fun SettingsScreen(
    container: AppContainer,
    onOpenApiConfig: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container)),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasApiKey by container.secretStore.hasApiKey.collectAsStateWithLifecycle()
    var showOrbPreview by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        SettingsSection(R.string.settings_section_appearance) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = settings.themeMode == mode, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(mode.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(
                        value = settings.reducedMotion,
                        onValueChange = viewModel::setReducedMotion,
                        role = Role.Switch,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_reduced_motion),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_reduced_motion_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.reducedMotion, onCheckedChange = null)
            }
        }

        SettingsSection(R.string.settings_section_gemini) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClick = onOpenApiConfig)
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_api_key),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        if (hasApiKey) R.string.settings_api_key_saved else R.string.settings_api_key_missing,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingsSection(R.string.settings_section_conversation) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(
                        value = settings.voiceInterruption,
                        onValueChange = viewModel::setVoiceInterruption,
                        role = Role.Switch,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_voice_interruption),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_voice_interruption_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.voiceInterruption, onCheckedChange = null)
            }
            Spacer(Modifier.height(8.dp))
            LimitChooser(
                title = R.string.settings_session_length,
                options = listOf(5, 10, 15, 30),
                selected = settings.usageLimits.sessionMaxMinutes,
                label = { stringResource(R.string.unit_minutes, it) },
                onSelect = viewModel::setSessionMaxMinutes,
            )
            LimitChooser(
                title = R.string.settings_daily_budget,
                options = listOf(15, 30, 60, 120),
                selected = settings.usageLimits.dailyBudgetMinutes,
                label = { stringResource(R.string.unit_minutes, it) },
                onSelect = viewModel::setDailyBudgetMinutes,
            )
            LimitChooser(
                title = R.string.settings_idle_timeout,
                options = listOf(30, 60, 120),
                selected = settings.usageLimits.idleTimeoutSeconds,
                label = { stringResource(R.string.unit_seconds, it) },
                onSelect = viewModel::setIdleTimeoutSeconds,
            )
            Text(
                text = stringResource(R.string.settings_limits_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsSection(R.string.settings_section_testing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { showOrbPreview = true }
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_orb_preview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_orb_preview_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingsSection(R.string.settings_section_about) {
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.ai_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showOrbPreview) {
        OrbPreviewDialog(onDismiss = { showOrbPreview = false })
    }
}

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.Dark -> R.string.theme_dark
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.System -> R.string.theme_system
}

@Composable
private fun SettingsSection(@StringRes title: Int, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

/**
 * Testing tool: shows each orb state on demand. The level slider is set by hand
 * here and clearly labeled; the Home Screen orb only ever uses real state and audio.
 */
@Composable
private fun OrbPreviewDialog(onDismiss: () -> Unit) {
    var previewState by remember { mutableStateOf(ConversationState.Idle) }
    var level by remember { mutableFloatStateOf(0.4f) }
    val reducedMotion = LocalReducedMotion.current
    val label = stringResource(previewState.labelRes())

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
        title = { Text(stringResource(R.string.settings_orb_preview)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VoiceOrb(
                    state = previewState,
                    amplitude = level,
                    reducedMotion = reducedMotion,
                    contentDescription = stringResource(R.string.orb_description, label),
                    modifier = Modifier.width(180.dp),
                )
                Text(label, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowChips(selected = previewState, onSelect = { previewState = it })
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.orb_preview_level),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(value = level, onValueChange = { level = it })
            }
        },
    )
}

@Composable
private fun FlowChips(selected: ConversationState, onSelect: (ConversationState) -> Unit) {
    // Three rows of three, so nothing depends on FlowRow's experimental API.
    ConversationState.entries.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { state ->
                FilterChip(
                    selected = state == selected,
                    onClick = { onSelect(state) },
                    label = { Text(stringResource(state.labelRes()), style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}

@Composable
private fun LimitChooser(
    @StringRes title: Int,
    options: List<Int>,
    selected: Int,
    label: @Composable (Int) -> String,
    onSelect: (Int) -> Unit,
) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label(value)) },
            )
        }
    }
}
