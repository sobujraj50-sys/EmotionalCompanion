package com.emotionalcompanion.domain.conversation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Things that happen in a live conversation. Each one comes from a real audio or server event. */
enum class ConversationEvent {
    StartRequested,
    Ready,
    UserSpeechHeard,
    ModelAudioArrived,
    PlaybackFinished,
    Interrupted,
    InterruptionSettled,
    ConnectionDropped,
    Reconnected,
    NetworkUnavailable,
    Failed,
    Stopped,
}

/** Pure transition rules, so the orb never shows a state that didn't happen. */
object ConversationTransitions {
    fun next(current: ConversationState, event: ConversationEvent): ConversationState = when (event) {
        ConversationEvent.StartRequested -> ConversationState.Connecting
        ConversationEvent.Ready, ConversationEvent.Reconnected -> ConversationState.Listening
        ConversationEvent.UserSpeechHeard ->
            if (current == ConversationState.Listening) ConversationState.Thinking else current
        ConversationEvent.ModelAudioArrived -> when (current) {
            ConversationState.Listening, ConversationState.Thinking, ConversationState.Speaking,
            ConversationState.Interrupted -> ConversationState.Speaking
            else -> current
        }
        ConversationEvent.PlaybackFinished ->
            if (current == ConversationState.Speaking) ConversationState.Listening else current
        ConversationEvent.Interrupted ->
            if (current == ConversationState.Speaking || current == ConversationState.Thinking) {
                ConversationState.Interrupted
            } else current
        ConversationEvent.InterruptionSettled ->
            if (current == ConversationState.Interrupted) ConversationState.Listening else current
        ConversationEvent.ConnectionDropped -> ConversationState.Reconnecting
        ConversationEvent.NetworkUnavailable -> ConversationState.Offline
        ConversationEvent.Failed -> ConversationState.Error
        ConversationEvent.Stopped -> ConversationState.Idle
    }
}

class ConversationStateManager {
    private val _state = MutableStateFlow(ConversationState.Idle)
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    fun on(event: ConversationEvent) {
        _state.update { ConversationTransitions.next(it, event) }
    }
}
