package com.emotionalcompanion.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.data.settings.AppSettings
import com.emotionalcompanion.data.settings.SettingsRepository
import com.emotionalcompanion.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings(onboardingDone = true))

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { repository.setReducedMotion(enabled) }
    }

    fun setVoiceInterruption(enabled: Boolean) {
        viewModelScope.launch { repository.setVoiceInterruption(enabled) }
    }

    fun setSessionMaxMinutes(minutes: Int) {
        viewModelScope.launch { repository.setSessionMaxMinutes(minutes) }
    }

    fun setDailyBudgetMinutes(minutes: Int) {
        viewModelScope.launch { repository.setDailyBudgetMinutes(minutes) }
    }

    fun setIdleTimeoutSeconds(seconds: Int) {
        viewModelScope.launch { repository.setIdleTimeoutSeconds(seconds) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container.settingsRepository) }
        }
    }
}
