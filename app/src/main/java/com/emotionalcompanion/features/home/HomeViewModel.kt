package com.emotionalcompanion.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.core.security.SecretStore
import com.emotionalcompanion.domain.conversation.ConnectionStatus
import com.emotionalcompanion.domain.conversation.ConversationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime

data class HomeUiState(
    val timeOfDay: TimeOfDay,
    val connection: ConnectionStatus = ConnectionStatus.NotConfigured,
    val conversation: ConversationState = ConversationState.Idle,
    /** Real audio levels, 0..1. Empty until capture/playback exist (Milestone D). */
    val audioLevels: List<Float> = emptyList(),
    /** False until the live session exists (Milestone D). */
    val voiceAvailable: Boolean = false,
)

/**
 * Milestone C: the connection chip reflects whether an API key is saved.
 * No live session exists yet, so the orb stays Idle and the mic stays disabled.
 */
class HomeViewModel(secretStore: SecretStore) : ViewModel() {

    private val timeOfDay = MutableStateFlow(TimeOfDay.fromHour(LocalTime.now().hour))

    val uiState: StateFlow<HomeUiState> = combine(timeOfDay, secretStore.hasApiKey) { time, hasKey ->
        HomeUiState(
            timeOfDay = time,
            connection = if (hasKey) ConnectionStatus.Configured else ConnectionStatus.NotConfigured,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(timeOfDay = timeOfDay.value))

    /** Called when Home resumes, so the greeting stays right if the app stays open for hours. */
    fun refreshTimeOfDay(hour: Int = LocalTime.now().hour) {
        timeOfDay.value = TimeOfDay.fromHour(hour)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(container.secretStore) }
        }
    }
}
