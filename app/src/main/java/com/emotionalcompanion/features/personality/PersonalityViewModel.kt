package com.emotionalcompanion.features.personality

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.emotionalcompanion.ai.personality.PersonalitySettings
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PersonalityViewModel(private val settings: SettingsRepository) : ViewModel() {

    val personality: StateFlow<PersonalitySettings> = settings.settings
        .map { it.personality }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonalitySettings())

    fun update(transform: (PersonalitySettings) -> PersonalitySettings) {
        viewModelScope.launch {
            val current = settings.settings.first().personality
            settings.setPersonality(transform(current))
        }
    }

    /** Resets the character traits; voice-delivery settings stay as they are. */
    fun resetCharacter() = update { current ->
        val d = PersonalitySettings()
        d.copy(warmth = current.warmth, softness = current.softness, energy = current.energy, pace = current.pace)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { PersonalityViewModel(container.settingsRepository) }
        }
    }
}
