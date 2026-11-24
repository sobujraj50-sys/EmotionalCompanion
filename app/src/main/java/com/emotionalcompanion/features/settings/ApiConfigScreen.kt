package com.emotionalcompanion.features.settings

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emotionalcompanion.R
import com.emotionalcompanion.ai.gemini.ConnectionTestResult
import com.emotionalcompanion.ai.gemini.LiveModel
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.core.security.ApiKeyValidator

@Composable
fun ApiConfigScreen(
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: ApiConfigViewModel = viewModel(factory = ApiConfigViewModel.factory(container)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmRemove by remember { mutableStateOf(false) }

    // Block screenshots and screen recording while the key could be on screen.
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

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
                text = stringResource(R.string.api_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Key status + input
        Section(R.string.api_section_key) {
            Text(
                text = stringResource(if (state.hasSavedKey) R.string.api_key_saved else R.string.api_key_none),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.keyInput,
                onValueChange = viewModel::onKeyInputChange,
                label = {
                    Text(stringResource(if (state.hasSavedKey) R.string.api_key_replace_label else R.string.api_key_label))
                },
                singleLine = true,
                visualTransformation = if (state.showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                ),
                trailingIcon = {
                    TextButton(onClick = viewModel::toggleShowKey) {
                        Text(stringResource(if (state.showKey) R.string.api_key_hide else R.string.api_key_show))
                    }
                },
                isError = state.inputProblem != null,
                supportingText = state.inputProblem?.let { problem -> { Text(stringResource(problem.messageRes())) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::saveAndTest,
                enabled = state.keyInput.isNotEmpty() && !state.testing,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.api_save_and_test))
            }
            if (state.hasSavedKey) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = viewModel::testConnection,
                        enabled = !state.testing,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.api_test))
                    }
                    TextButton(
                        onClick = { confirmRemove = true },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.api_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            TestResultRow(testing = state.testing, result = state.testResult)
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.api_key_howto),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Model selection
        Section(R.string.api_section_model) {
            Column(Modifier.selectableGroup()) {
                state.models.forEach { model ->
                    ModelRow(
                        model = model,
                        selected = model.id == state.selectedModelId,
                        onSelect = { viewModel.selectModel(model.id) },
                    )
                }
            }
            if (state.freeOnly) {
                Text(
                    text = stringResource(R.string.api_models_hidden),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Free-only
        Section(R.string.api_section_cost) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(value = state.freeOnly, onValueChange = viewModel::setFreeOnly, role = Role.Switch),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.api_free_only),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = state.freeOnly, onCheckedChange = null)
            }
            Text(
                text = stringResource(R.string.api_free_only_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Privacy disclosure
        Section(R.string.api_section_privacy) {
            Text(
                text = stringResource(R.string.api_privacy_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.api_remove_title)) },
            text = { Text(stringResource(R.string.api_remove_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    viewModel.removeKey()
                }) { Text(stringResource(R.string.api_remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable () -> Unit) {
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

@Composable
private fun ModelRow(model: LiveModel, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = model.id,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(model.status.labelRes()) + " · " + stringResource(model.freeTier.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TestResultRow(testing: Boolean, result: ConnectionTestResult?) {
    if (!testing && result == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        if (testing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.api_testing), style = MaterialTheme.typography.bodyMedium)
        } else if (result != null) {
            val ok = result == ConnectionTestResult.Success
            Text(
                text = resultMessage(result),
                style = MaterialTheme.typography.bodyMedium,
                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun resultMessage(result: ConnectionTestResult): String = when (result) {
    ConnectionTestResult.Success -> stringResource(R.string.api_result_success)
    ConnectionTestResult.KeyRejected -> stringResource(R.string.api_result_key_rejected)
    ConnectionTestResult.PermissionDenied -> stringResource(R.string.api_result_permission)
    ConnectionTestResult.ModelUnavailable -> stringResource(R.string.api_result_model)
    ConnectionTestResult.QuotaExceeded -> stringResource(R.string.api_result_quota)
    ConnectionTestResult.ServerError -> stringResource(R.string.api_result_server)
    ConnectionTestResult.NoInternet -> stringResource(R.string.api_result_no_internet)
    ConnectionTestResult.Timeout -> stringResource(R.string.api_result_timeout)
    is ConnectionTestResult.Unexpected -> stringResource(
        R.string.api_result_unexpected,
        result.httpCode?.toString() ?: "—",
    )
}

@StringRes
private fun ApiKeyValidator.Problem.messageRes(): Int = when (this) {
    ApiKeyValidator.Problem.Empty -> R.string.api_key_error_empty
    ApiKeyValidator.Problem.ContainsWhitespace -> R.string.api_key_error_space
    ApiKeyValidator.Problem.TooShort -> R.string.api_key_error_short
}

@StringRes
private fun LiveModel.Status.labelRes(): Int = when (this) {
    LiveModel.Status.Stable -> R.string.model_status_stable
    LiveModel.Status.Preview -> R.string.model_status_preview
    LiveModel.Status.Legacy -> R.string.model_status_legacy
}

@StringRes
private fun LiveModel.FreeTier.labelRes(): Int = when (this) {
    LiveModel.FreeTier.Confirmed -> R.string.model_free_confirmed
    LiveModel.FreeTier.NotListed -> R.string.model_free_not_listed
}
