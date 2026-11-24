package com.emotionalcompanion.domain.conversation

import com.emotionalcompanion.domain.conversation.ConversationEvent as E
import com.emotionalcompanion.domain.conversation.ConversationState as S
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationTransitionsTest {

    private fun run(vararg events: E): S = events.fold(S.Idle) { s, e -> ConversationTransitions.next(s, e) }

    @Test
    fun normalTurn() {
        assertEquals(S.Connecting, run(E.StartRequested))
        assertEquals(S.Listening, run(E.StartRequested, E.Ready))
        assertEquals(S.Thinking, run(E.StartRequested, E.Ready, E.UserSpeechHeard))
        assertEquals(S.Speaking, run(E.StartRequested, E.Ready, E.UserSpeechHeard, E.ModelAudioArrived))
        assertEquals(S.Listening, run(E.StartRequested, E.Ready, E.ModelAudioArrived, E.PlaybackFinished))
    }

    @Test
    fun interruptionReturnsToListening() {
        val speaking = run(E.StartRequested, E.Ready, E.ModelAudioArrived)
        val interrupted = ConversationTransitions.next(speaking, E.Interrupted)
        assertEquals(S.Interrupted, interrupted)
        assertEquals(S.Listening, ConversationTransitions.next(interrupted, E.InterruptionSettled))
    }

    @Test
    fun interruptionIgnoredWhileJustListening() {
        assertEquals(S.Listening, run(E.StartRequested, E.Ready, E.Interrupted))
    }

    @Test
    fun noModelAudioBeforeReady() {
        assertEquals(S.Connecting, run(E.StartRequested, E.ModelAudioArrived))
    }

    @Test
    fun reconnectAndFailures() {
        assertEquals(S.Reconnecting, run(E.StartRequested, E.Ready, E.ConnectionDropped))
        assertEquals(S.Listening, run(E.StartRequested, E.Ready, E.ConnectionDropped, E.Reconnected))
        assertEquals(S.Offline, run(E.StartRequested, E.NetworkUnavailable))
        assertEquals(S.Error, run(E.StartRequested, E.Failed))
        assertEquals(S.Idle, run(E.StartRequested, E.Ready, E.Stopped))
    }
}
