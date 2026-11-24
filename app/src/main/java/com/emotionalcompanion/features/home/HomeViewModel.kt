package com.emotionalcompanion.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.emotionalcompanion.ai.gemini.ClassifiedError
import com.emotionalcompanion.ai.gemini.LiveError
import com.emotionalcompanion.ai.gemini.LiveSessionManager
import com.emotionalcompanion.ai.gemini.SessionEndReason
import com.emotionalcompanion.ai.gemini.TranscriptLine
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.core.security.SecretStore
import com.emotionalcompanion.data.settings.SettingsRepository
import com.emotionalcompanion.domain.conversation.ConnectionStatus
import com.emotionalcompanion.domain.conversation.ConversationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class HomeUiState(
    val timeOfDay: TimeOfDay,
    val connection: ConnectionStatus = ConnectionStatus.NotConfigured,
    val conversation: ConversationState = ConversationState.Idle,
    /** Real audio levels, 0..1 (mic while listening, playback while speaking). */
    val audioLevels: List<Float> = emptyList(),
    /** True once an API key is saved. */
    val voiceAvailable: Boolean = false,
    val active: Boolean = false,
    val speakerOn: Boolean = true,
    val halfDuplex: Boolean = false,
    val error: ClassifiedError? = null,
    val endReason: SessionEndReason? = null,
    val transcript: List<TranscriptLine> = emptyList(),
    val sessionSeconds: Int = 0,
    val sessionMaxMinutes: Int = 10,
    val usedTodaySeconds: Int = 0,
    val dailyBudgetMinutes: Int = 30,
    val disclosureAccepted: Boolean = false,
)

class HomeViewModel(
    secretStore: SecretStore,
    private val settings: SettingsRepository,
    private val live: LiveSessionManager,
) : ViewModel() {

    private val timeOfDay = MutableStateFlow(TimeOfDay.fromHour(LocalTime.now().hour))

    val uiState: StateFlow<HomeUiState> = combine(
        timeOfDay,
        secretStore.hasApiKey,
        live.state,
        live.conversationState,
        settings.settings,
    ) { time, hasKey, session, conversation, s ->
        HomeUiState(
            timeOfDay = time,
            connection = session.connection
                ?: if (hasKey) ConnectionStatus.Configured else ConnectionStatus.NotConfigured,
            conversation = conversation,
            audioLevels = session.levels,
            voiceAvailable = hasKey,
            active = session.active,
            speakerOn = session.speakerOn,
            halfDuplex = session.halfDuplex,
            error = session.error,
            endReason = session.endReason,
            transcript = session.transcript,
            sessionSeconds = session.sessionSeconds,
            sessionMaxMinutes = s.usageLimits.sessionMaxMinutes,
            usedTodaySeconds = session.usedTodaySeconds,
            dailyBudgetMinutes = s.usageLimits.dailyBudgetMinutes,
            disclosureAccepted = s.audioDisclosureAccepted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(timeOfDay = timeOfDay.value))

    /** Called when Home resumes. */
    fun onResume(hour: Int = LocalTime.now().hour) {
        timeOfDay.value = TimeOfDay.fromHour(hour)
        live.refreshUsage()
    }

    fun startConversation() {
        live.clearError()
        live.start()
    }

    fun stopConversation() = live.stop()

    fun toggleSpeaker() = live.setSpeakerOn(!live.state.value.speakerOn)

    fun interrupt() = live.interruptPlayback()

    fun dismissError() = live.clearError()

    fun onMicPermissionDenied() = live.reportError(ClassifiedError(LiveError.MicPermission))

    fun acceptDisclosure() {
        viewModelScope.launch { settings.setAudioDisclosureAccepted() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    secretStore = container.secretStore,
                    settings = container.settingsRepository,
                    live = container.liveSessionManager,
                )
            }
        }
    }
}
