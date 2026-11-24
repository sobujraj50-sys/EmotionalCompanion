package com.emotionalcompanion.ai.gemini

import android.media.AudioAttributes
import com.emotionalcompanion.ai.personality.PersonalityEngine
import com.emotionalcompanion.core.audio.AudioPlaybackManager
import com.emotionalcompanion.core.security.SecretStore
import com.emotionalcompanion.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class AuditionState(
    val voiceName: String? = null,
    val connecting: Boolean = false,
    val playing: Boolean = false,
    val level: Float = 0f,
    val error: ClassifiedError? = null,
)

/**
 * Plays a voice sample through the same Gemini Live model used for conversations, so the
 * audition sounds like the real thing: a short session with the chosen voice is asked to read
 * the test line (sent as text, so no microphone is used). The time counts toward the daily budget.
 */
class VoiceAuditionPlayer(
    private val client: GeminiLiveClient,
    private val secretStore: SecretStore,
    private val settings: SettingsRepository,
    private val usage: UsageController,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(AuditionState())
    val state: StateFlow<AuditionState> = _state.asStateFlow()
    private var job: Job? = null

    fun play(voiceName: String, text: String) {
        stop()
        _state.value = AuditionState(voiceName = voiceName, connecting = true)
        job = scope.launch { run(voiceName, text) }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.update { it.copy(connecting = false, playing = false, level = 0f) }
    }

    private suspend fun run(voiceName: String, text: String) {
        val key = secretStore.readApiKey()
        if (key == null) {
            finish(ClassifiedError(LiveError.NotConfigured))
            return
        }
        val cfg = settings.settings.first()
        if (!UsageRules.canStart(cfg.usageLimits, usage.usedTodaySeconds())) {
            finish(ClassifiedError(LiveError.BudgetReached))
            return
        }

        val playback = AudioPlaybackManager(usage = AudioAttributes.USAGE_MEDIA)
        if (!playback.start(scope, onLevel = { l -> _state.update { it.copy(level = l) } }, onDrained = {})) {
            finish(ClassifiedError(LiveError.AudioBusy))
            return
        }
        val signals = Channel<LiveSignal>(Channel.UNLIMITED)
        val connection = client.connect(key) { signals.trySend(it) }
        val startedAt = System.currentTimeMillis()
        var error: ClassifiedError? = null
        try {
            withTimeout(AUDITION_TIMEOUT_MS) {
                var turnDone = false
                while (!turnDone) {
                    when (val signal = signals.receive()) {
                        LiveSignal.Open -> connection.send(
                            LiveProtocol.setupMessage(
                                modelId = cfg.liveModelId,
                                voiceName = voiceName,
                                systemInstruction = PersonalityEngine.auditionInstruction(cfg.personality),
                                resumptionHandle = null,
                                conversation = false,
                            ),
                        )
                        is LiveSignal.Message -> for (event in LiveProtocol.parse(signal.text)) {
                            when (event) {
                                LiveEvent.SetupComplete -> connection.send(LiveProtocol.textMessage(text))
                                is LiveEvent.Audio -> {
                                    playback.enqueue(event.pcm)
                                    _state.update { it.copy(connecting = false, playing = true) }
                                }
                                LiveEvent.TurnComplete -> turnDone = true
                                else -> Unit
                            }
                        }
                        is LiveSignal.Closed -> {
                            error = ConnectionErrorHandler.fromClose(signal.code, signal.reason)
                                ?: ClassifiedError(LiveError.ConnectionLost)
                            turnDone = true
                        }
                        is LiveSignal.Failed -> {
                            error = ConnectionErrorHandler.fromFailure(signal.failure)
                            turnDone = true
                        }
                    }
                }
                connection.close()
                while (playback.isPlaying) delay(50)
            }
        } catch (_: TimeoutCancellationException) {
            error = ClassifiedError(LiveError.Timeout)
        } finally {
            connection.cancel()
            playback.stop()
            val seconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt().coerceAtLeast(1)
            scope.launch { usage.add(seconds) }
        }
        finish(error)
    }

    private fun finish(error: ClassifiedError?) {
        _state.update { it.copy(connecting = false, playing = false, level = 0f, error = error) }
    }

    private companion object {
        const val AUDITION_TIMEOUT_MS = 30_000L
    }
}
