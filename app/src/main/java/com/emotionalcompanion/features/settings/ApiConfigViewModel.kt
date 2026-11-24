package com.emotionalcompanion.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.emotionalcompanion.ai.gemini.ConnectionTestResult
import com.emotionalcompanion.ai.gemini.GeminiConnectionTester
import com.emotionalcompanion.ai.gemini.GeminiModelCatalog
import com.emotionalcompanion.ai.gemini.LiveModel
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.core.security.ApiKeyValidator
import com.emotionalcompanion.core.security.SecretStore
import com.emotionalcompanion.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApiConfigUiState(
    val keyInput: String = "",
    val showKey: Boolean = false,
    val inputProblem: ApiKeyValidator.Problem? = null,
    val hasSavedKey: Boolean = false,
    val testing: Boolean = false,
    val testResult: ConnectionTestResult? = null,
    val freeOnly: Boolean = true,
    val selectedModelId: String = GeminiModelCatalog.defaultModelId,
    val models: List<LiveModel> = GeminiModelCatalog.available(freeOnly = true),
)

class ApiConfigViewModel(
    private val secretStore: SecretStore,
    private val settingsRepository: SettingsRepository,
    private val tester: GeminiConnectionTester,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiConfigUiState(hasSavedKey = secretStore.hasApiKey.value))
    val uiState: StateFlow<ApiConfigUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            secretStore.hasApiKey.collect { has -> _uiState.update { it.copy(hasSavedKey = has) } }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _uiState.update {
                    it.copy(
                        freeOnly = s.freeOnly,
                        selectedModelId = s.liveModelId,
                        models = GeminiModelCatalog.available(s.freeOnly),
                    )
                }
            }
        }
    }

    fun onKeyInputChange(value: String) {
        _uiState.update { it.copy(keyInput = value.trim(), inputProblem = null) }
    }

    fun toggleShowKey() {
        _uiState.update { it.copy(showKey = !it.showKey) }
    }

    /** Saves the typed key (encrypted), clears the field, then tests the connection. */
    fun saveAndTest() {
        val input = _uiState.value.keyInput
        val problem = ApiKeyValidator.validate(input)
        if (problem != null) {
            _uiState.update { it.copy(inputProblem = problem) }
            return
        }
        viewModelScope.launch {
            secretStore.saveApiKey(input)
            _uiState.update { it.copy(keyInput = "", showKey = false, testResult = null) }
            runTest()
        }
    }

    fun testConnection() {
        viewModelScope.launch { runTest() }
    }

    private suspend fun runTest() {
        if (_uiState.value.testing) return
        val key = secretStore.readApiKey() ?: run {
            _uiState.update { it.copy(testResult = null) }
            return
        }
        _uiState.update { it.copy(testing = true, testResult = null) }
        val result = tester.test(key, _uiState.value.selectedModelId)
        _uiState.update { it.copy(testing = false, testResult = result) }
    }

    fun removeKey() {
        viewModelScope.launch {
            secretStore.clearApiKey()
            _uiState.update { it.copy(testResult = null, keyInput = "") }
        }
    }

    fun setFreeOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFreeOnly(enabled)
            _uiState.update { it.copy(testResult = null) }
        }
    }

    fun selectModel(id: String) {
        viewModelScope.launch {
            settingsRepository.setLiveModelId(id)
            _uiState.update { it.copy(testResult = null) }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ApiConfigViewModel(
                    secretStore = container.secretStore,
                    settingsRepository = container.settingsRepository,
                    tester = container.connectionTester,
                )
            }
        }
    }
}
