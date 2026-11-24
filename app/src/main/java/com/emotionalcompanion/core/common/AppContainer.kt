package com.emotionalcompanion.core.common

import android.content.Context
import com.emotionalcompanion.ai.gemini.GeminiConnectionTester
import com.emotionalcompanion.ai.gemini.GeminiLiveClient
import com.emotionalcompanion.ai.gemini.HttpGeminiConnectionTester
import com.emotionalcompanion.ai.gemini.LiveSessionManager
import com.emotionalcompanion.ai.gemini.SessionEndReason
import com.emotionalcompanion.ai.gemini.UsageController
import com.emotionalcompanion.ai.gemini.VoiceAuditionPlayer
import com.emotionalcompanion.core.audio.AudioCaptureManager
import com.emotionalcompanion.core.audio.AudioFocusManager
import com.emotionalcompanion.core.audio.AudioPlaybackManager
import com.emotionalcompanion.core.audio.OfflineSpeech
import com.emotionalcompanion.core.security.KeystoreSecretStore
import com.emotionalcompanion.core.security.SecretStore
import com.emotionalcompanion.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual DI container. Each milestone adds its dependencies here as lazily
 * created singletons, so providers and audio engines stay replaceable.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    /** Lives as long as the process; used for work that must outlive a screen. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val secretStore: SecretStore by lazy { KeystoreSecretStore(appContext) }

    val connectionTester: GeminiConnectionTester by lazy { HttpGeminiConnectionTester() }

    private val liveSessionLazy = lazy {
        LiveSessionManager(
            secretStore = secretStore,
            settings = settingsRepository,
            client = GeminiLiveClient(),
            capture = AudioCaptureManager(appContext),
            playback = AudioPlaybackManager(),
            focus = AudioFocusManager(appContext),
            usage = UsageController(settingsRepository),
            scope = applicationScope,
        )
    }
    val liveSessionManager: LiveSessionManager by liveSessionLazy

    val voiceAuditionPlayer: VoiceAuditionPlayer by lazy {
        VoiceAuditionPlayer(
            client = GeminiLiveClient(),
            secretStore = secretStore,
            settings = settingsRepository,
            usage = UsageController(settingsRepository),
            scope = applicationScope,
        )
    }

    val offlineSpeech: OfflineSpeech by lazy { OfflineSpeech(appContext) }

    /** Ends a running conversation without creating the manager if it was never used. */
    fun stopLiveSession(reason: SessionEndReason) {
        if (liveSessionLazy.isInitialized()) liveSessionManager.stop(reason)
    }
}
