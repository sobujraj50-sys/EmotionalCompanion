package com.emotionalcompanion.features.home

import androidx.lifecycle.ViewModel
import com.emotionalcompanion.domain.conversation.ConnectionStatus
import com.emotionalcompanion.domain.conversation.ConversationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime

data class HomeUiState(
    val timeOfDay: TimeOfDay,
    val connection: ConnectionStatus = ConnectionStatus.NotConfigured,
    val conversation: ConversationState = ConversationState.Idle,
    /** Real audio levels, 0..1. Empty until capture/playback exist (Milestone D). */
    val audioLevels: List<Float> = emptyList(),
    /** False until an API key is saved (C) and the live session exists (D). */
    val voiceAvailable: Boolean = false,
)

/**
 * Milestone B: no API key or live session exists yet, so the honest state is
 * "not configured" + Idle. Milestones C/D will feed this from LiveSessionManager.
 */
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(timeOfDay = TimeOfDay.fromHour(LocalTime.now().hour)))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Called when Home resumes, so the greeting stays right if the app stays open for hours. */
    fun refreshTimeOfDay(hour: Int = LocalTime.now().hour) {
        _uiState.update { it.copy(timeOfDay = TimeOfDay.fromHour(hour)) }
    }
}
