package com.emotionalcompanion.features.voicestudio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.emotionalcompanion.ai.gemini.AuditionState
import com.emotionalcompanion.ai.gemini.LiveSessionManager
import com.emotionalcompanion.ai.gemini.TestDialogue
import com.emotionalcompanion.ai.gemini.VoiceAuditionPlayer
import com.emotionalcompanion.ai.personality.PersonalitySettings
import com.emotionalcompanion.core.audio.OfflineSpeech
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VoiceStudioUiState(
    val selectedVoice: String = "",
    val showAllVoices: Boolean = false,
    val dialogue: TestDialogue = TestDialogue.Bangla,
    val audition: AuditionState = AuditionState(),
    val conversationActive: Boolean = false,
    val personality: PersonalitySettings = PersonalitySettings(),
    val offline: OfflineSpeech.Availability = OfflineSpeech.Availability.Unknown,
    val offlineFailed: Boolean = false,
)

class VoiceStudioViewModel(
    private val settings: SettingsRepository,
    private val player: VoiceAuditionPlayer,
    private val live: LiveSessionManager,
    private val offlineSpeech: OfflineSpeech,
) : ViewModel() {

    private val local = MutableStateFlow(VoiceStudioUiState())

    val uiState: StateFlow<VoiceStudioUiState> = combine(
        local,
        settings.settings,
        player.state,
        live.state,
        offlineSpeech.availability,
    ) { l, s, audition, session, offline ->
        l.copy(
            selectedVoice = s.voiceName,
            audition = audition,
            conversationActive = session.active,
            personality = s.personality,
            offline = offline,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VoiceStudioUiState())

    fun selectVoice(name: String) {
        viewModelScope.launch { settings.setVoiceName(name) }
    }

    fun audition(name: String) {
        if (live.state.value.active) return
        offlineSpeech.stop()
        player.play(name, local.value.dialogue.text)
    }

    fun stopAudition() = player.stop()

    fun setDialogue(dialogue: TestDialogue) {
        local.value = local.value.copy(dialogue = dialogue)
    }

    fun toggleShowAll() {
        local.value = local.value.copy(showAllVoices = !local.value.showAllVoices)
    }

    fun updateDelivery(transform: (PersonalitySettings) -> PersonalitySettings) {
        viewModelScope.launch {
            val current = settings.settings.first().personality
            settings.setPersonality(transform(current))
        }
    }

    fun playOffline() {
        player.stop()
        val dialogue = local.value.dialogue
        val ok = offlineSpeech.speak(dialogue.text, bangla = dialogue != TestDialogue.English)
        local.value = local.value.copy(offlineFailed = !ok)
    }

    override fun onCleared() {
        player.stop()
        offlineSpeech.stop()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                VoiceStudioViewModel(
                    settings = container.settingsRepository,
                    player = container.voiceAuditionPlayer,
                    live = container.liveSessionManager,
                    offlineSpeech = container.offlineSpeech,
                )
            }
        }
    }
}
