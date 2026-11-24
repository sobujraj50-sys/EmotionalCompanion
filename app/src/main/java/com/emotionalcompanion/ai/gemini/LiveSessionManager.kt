package com.emotionalcompanion.ai.gemini

import com.emotionalcompanion.ai.personality.DefaultPersonality
import com.emotionalcompanion.core.audio.AudioCaptureManager
import com.emotionalcompanion.core.audio.AudioFocusManager
import com.emotionalcompanion.core.audio.AudioPlaybackManager
import com.emotionalcompanion.core.security.SecretStore
import com.emotionalcompanion.data.settings.AppSettings
import com.emotionalcompanion.data.settings.SettingsRepository
import com.emotionalcompanion.domain.conversation.ConnectionStatus
import com.emotionalcompanion.domain.conversation.ConversationEvent
import com.emotionalcompanion.domain.conversation.ConversationState
import com.emotionalcompanion.domain.conversation.ConversationStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranscriptLine(val fromUser: Boolean, val text: String)

data class LiveSessionState(
    val active: Boolean = false,
    /** Null while no session runs; the UI then shows whether a key is saved. */
    val connection: ConnectionStatus? = null,
    /** Recent real audio levels (mic while listening, playback while speaking). */
    val levels: List<Float> = emptyList(),
    val speakerOn: Boolean = true,
    val error: ClassifiedError? = null,
    val endReason: SessionEndReason? = null,
    /** Current-session captions, kept in memory only (never stored). */
    val transcript: List<TranscriptLine> = emptyList(),
    val sessionSeconds: Int = 0,
    val usedTodaySeconds: Int = 0,
    val halfDuplex: Boolean = false,
)

/**
 * Runs one Gemini Live voice conversation: audio focus → playback → WebSocket → setup →
 * microphone streaming, plus interruption, reconnection with session resumption, local
 * usage limits and cleanup. Every state shown on the orb comes from a real event here.
 */
class LiveSessionManager(
    private val secretStore: SecretStore,
    private val settings: SettingsRepository,
    private val client: GeminiLiveClient,
    private val capture: AudioCaptureManager,
    private val playback: AudioPlaybackManager,
    private val focus: AudioFocusManager,
    private val usage: UsageController,
    private val scope: CoroutineScope,
) {
    private val conversation = ConversationStateManager()
    val conversationState: StateFlow<ConversationState> = conversation.state

    private val _state = MutableStateFlow(LiveSessionState())
    val state: StateFlow<LiveSessionState> = _state.asStateFlow()

    private var signals = Channel<Pair<Int, LiveSignal>>(Channel.UNLIMITED)
    private var sessionJob: Job? = null

    @Volatile private var connection: LiveConnection? = null
    @Volatile private var connectionId = 0
    @Volatile private var setupDone = false
    @Volatile private var everReady = false
    @Volatile private var resumptionHandle: String? = null
    @Volatile private var turnComplete = true
    @Volatile private var ignoreModelAudio = false
    @Volatile private var halfDuplex = false
    @Volatile private var idleSeconds = 0
    @Volatile private var unsavedSeconds = 0
    private var apiKey: String? = null
    private var config: AppSettings = AppSettings()
    private var reconnectAttempts = 0

    fun start() {
        if (_state.value.active) return
        _state.update {
            LiveSessionState(active = true, speakerOn = it.speakerOn, usedTodaySeconds = it.usedTodaySeconds)
        }
        conversation.on(ConversationEvent.StartRequested)
        signals = Channel(Channel.UNLIMITED)
        sessionJob = scope.launch { runSession() }
    }

    private suspend fun runSession() {
        val cfg = settings.settings.first()
        config = cfg
        val key = secretStore.readApiKey()
        if (key == null) {
            fail(ClassifiedError(LiveError.NotConfigured))
            return
        }
        if (!capture.hasPermission()) {
            fail(ClassifiedError(LiveError.MicPermission))
            return
        }
        val used = usage.usedTodaySeconds()
        _state.update { it.copy(usedTodaySeconds = used) }
        if (!UsageRules.canStart(cfg.usageLimits, used)) {
            stop(SessionEndReason.DailyBudget)
            return
        }

        halfDuplex = !cfg.voiceInterruption || !capture.echoCancellationAvailable
        _state.update { it.copy(halfDuplex = halfDuplex, connection = ConnectionStatus.Connecting) }

        if (!focus.acquire { scope.launch { stop(SessionEndReason.AudioFocusLost) } }) {
            fail(ClassifiedError(LiveError.AudioBusy))
            return
        }
        focus.setSpeakerOn(_state.value.speakerOn)
        if (!playback.start(scope, onLevel = ::onPlaybackLevel, onDrained = ::onPlaybackDrained)) {
            fail(ClassifiedError(LiveError.AudioBusy))
            return
        }

        apiKey = key
        everReady = false
        reconnectAttempts = 0
        resumptionHandle = null
        turnComplete = true
        ignoreModelAudio = false
        idleSeconds = 0
        unsavedSeconds = 0
        openConnection()

        coroutineScope {
            launch { tick() }
            for ((id, signal) in signals) {
                if (id == connectionId) handle(signal)
            }
        }
    }

    private fun openConnection() {
        val key = apiKey ?: return
        setupDone = false
        val id = ++connectionId
        val localSignals = signals
        connection = client.connect(key) { signal -> localSignals.trySend(id to signal) }
    }

    private suspend fun handle(signal: LiveSignal) {
        when (signal) {
            LiveSignal.Open -> connection?.send(
                LiveProtocol.setupMessage(
                    modelId = config.liveModelId,
                    voiceName = VoiceConfiguration.DEFAULT_VOICE,
                    systemInstruction = DefaultPersonality.systemInstruction,
                    resumptionHandle = resumptionHandle,
                ),
            )
            is LiveSignal.Message -> LiveProtocol.parse(signal.text).forEach { handleEvent(it) }
            is LiveSignal.Closed -> {
                if (!_state.value.active) return
                val error = ConnectionErrorHandler.fromClose(signal.code, signal.reason)
                    ?: ClassifiedError(LiveError.ConnectionLost)
                reconnectOrFail(error)
            }
            is LiveSignal.Failed -> {
                if (!_state.value.active) return
                reconnectOrFail(ConnectionErrorHandler.fromFailure(signal.failure))
            }
        }
    }

    private suspend fun handleEvent(event: LiveEvent) {
        when (event) {
            LiveEvent.SetupComplete -> {
                setupDone = true
                reconnectAttempts = 0
                conversation.on(if (everReady) ConversationEvent.Reconnected else ConversationEvent.Ready)
                everReady = true
                _state.update { it.copy(connection = ConnectionStatus.Connected, error = null) }
                if (!capture.isRunning && !capture.start(scope, ::onMicChunk)) {
                    fail(ClassifiedError(LiveError.MicUnavailable))
                }
            }
            is LiveEvent.Audio -> {
                if (ignoreModelAudio) return
                turnComplete = false
                idleSeconds = 0
                playback.enqueue(event.pcm)
                conversation.on(ConversationEvent.ModelAudioArrived)
            }
            is LiveEvent.InputTranscript -> {
                idleSeconds = 0
                appendTranscript(fromUser = true, text = event.text)
                conversation.on(ConversationEvent.UserSpeechHeard)
            }
            is LiveEvent.OutputTranscript -> appendTranscript(fromUser = false, text = event.text)
            LiveEvent.Interrupted -> {
                playback.clear()
                ignoreModelAudio = false
                conversation.on(ConversationEvent.Interrupted)
                scope.launch {
                    delay(INTERRUPTED_DISPLAY_MS)
                    conversation.on(ConversationEvent.InterruptionSettled)
                }
            }
            LiveEvent.TurnComplete -> {
                turnComplete = true
                ignoreModelAudio = false
                if (!playback.isPlaying) conversation.on(ConversationEvent.PlaybackFinished)
            }
            is LiveEvent.GoAway -> if (resumptionHandle != null) reconnect()
            is LiveEvent.ResumptionUpdate -> if (event.resumable && event.handle != null) resumptionHandle = event.handle
            LiveEvent.GenerationComplete, is LiveEvent.Usage -> Unit
        }
    }

    private suspend fun reconnectOrFail(error: ClassifiedError) {
        if (error.retryable && resumptionHandle != null && reconnectAttempts < MAX_RECONNECTS) {
            reconnect()
        } else {
            fail(error)
        }
    }

    private suspend fun reconnect() {
        reconnectAttempts++
        setupDone = false
        conversation.on(ConversationEvent.ConnectionDropped)
        _state.update { it.copy(connection = ConnectionStatus.Reconnecting) }
        connection?.cancel()
        connection = null
        playback.clear()
        delay(1_000L shl (reconnectAttempts - 1).coerceAtMost(3))
        if (_state.value.active) openConnection()
    }

    private suspend fun tick() {
        while (_state.value.active) {
            delay(1_000)
            if (!setupDone) continue
            idleSeconds++
            unsavedSeconds++
            _state.update { it.copy(sessionSeconds = it.sessionSeconds + 1, usedTodaySeconds = it.usedTodaySeconds + 1) }
            if (unsavedSeconds >= 10) {
                val n = unsavedSeconds
                unsavedSeconds = 0
                usage.add(n)
            }
            val s = _state.value
            UsageRules.endReason(config.usageLimits, s.sessionSeconds, idleSeconds, s.usedTodaySeconds)?.let {
                stop(it)
                return
            }
        }
    }

    // --- audio callbacks (background threads) ---

    private fun onMicChunk(pcm: ByteArray, level: Float) {
        val playing = playback.isPlaying
        if (!playing) pushLevel(level)
        if (level > SPEECH_LEVEL) idleSeconds = 0
        if (!setupDone) return
        // Half duplex: the mic is muted while the AI speaks, so its voice can't loop back.
        if (halfDuplex && playing) return
        connection?.send(LiveProtocol.audioMessage(pcm))
    }

    private fun onPlaybackLevel(level: Float) {
        if (level > 0f) idleSeconds = 0
        pushLevel(level)
    }

    private fun onPlaybackDrained() {
        if (turnComplete) conversation.on(ConversationEvent.PlaybackFinished)
    }

    private fun pushLevel(level: Float) {
        _state.update { it.copy(levels = (it.levels + level).takeLast(LEVEL_HISTORY)) }
    }

    private fun appendTranscript(fromUser: Boolean, text: String) {
        _state.update { s ->
            val lines = s.transcript.toMutableList()
            val last = lines.lastOrNull()
            if (last != null && last.fromUser == fromUser) {
                lines[lines.lastIndex] = last.copy(text = (last.text + text).replace(Regex("\\s+"), " "))
            } else {
                lines += TranscriptLine(fromUser, text.trim())
            }
            s.copy(transcript = lines.takeLast(MAX_TRANSCRIPT_LINES))
        }
    }

    // --- public controls ---

    /** Stops everything at once: mic first, then network and playback. */
    fun stop(reason: SessionEndReason = SessionEndReason.UserStopped) {
        if (!_state.value.active) return
        _state.update { it.copy(active = false) }
        capture.stop()
        connection?.close()
        connection = null
        setupDone = false
        playback.stop()
        focus.release()
        val toSave = unsavedSeconds
        unsavedSeconds = 0
        if (toSave > 0) scope.launch { usage.add(toSave) }
        conversation.on(ConversationEvent.Stopped)
        _state.update { it.copy(levels = emptyList(), connection = null, endReason = reason) }
        val job = sessionJob
        sessionJob = null
        signals.close()
        job?.cancel()
    }

    fun setSpeakerOn(on: Boolean) {
        _state.update { it.copy(speakerOn = on) }
        if (_state.value.active) focus.setSpeakerOn(on)
    }

    /** User tapped the orb while the AI was speaking: silence it now. */
    fun interruptPlayback() {
        if (!playback.isPlaying) return
        playback.clear()
        ignoreModelAudio = !turnComplete
        conversation.on(ConversationEvent.Interrupted)
        scope.launch {
            delay(INTERRUPTED_DISPLAY_MS)
            conversation.on(ConversationEvent.InterruptionSettled)
        }
    }

    /** Shows an error that happened before a session could start (e.g. mic permission denied). */
    fun reportError(error: ClassifiedError) {
        if (_state.value.active) return
        conversation.on(ConversationEvent.Failed)
        _state.update { it.copy(error = error) }
    }

    fun clearError() {
        _state.update { it.copy(error = null, endReason = null) }
        if (!_state.value.active) conversation.on(ConversationEvent.Stopped)
    }

    fun refreshUsage() {
        scope.launch {
            val used = usage.usedTodaySeconds()
            if (!_state.value.active) _state.update { it.copy(usedTodaySeconds = used) }
        }
    }

    private fun fail(error: ClassifiedError) {
        stop(SessionEndReason.Error)
        conversation.on(
            if (error.error == LiveError.NoInternet) ConversationEvent.NetworkUnavailable else ConversationEvent.Failed,
        )
        _state.update { it.copy(error = error) }
    }

    private companion object {
        const val MAX_RECONNECTS = 3
        const val INTERRUPTED_DISPLAY_MS = 600L
        const val SPEECH_LEVEL = 0.35f
        const val LEVEL_HISTORY = 36
        const val MAX_TRANSCRIPT_LINES = 6
    }
}
