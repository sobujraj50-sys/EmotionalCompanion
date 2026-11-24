package com.emotionalcompanion

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.emotionalcompanion.core.audio.AudioCaptureManager
import com.emotionalcompanion.core.audio.AudioPlaybackManager
import com.emotionalcompanion.core.audio.PcmAudioFormat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real AudioRecord / AudioTrack code paths. CI emulators may have no audio
 * device; then the test is skipped (reported as such), never faked as a pass.
 */
@RunWith(AndroidJUnit4::class)
class AudioPipelineTest {

    @get:Rule
    val micPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun captureDeliversCorrectlySizedChunksAndStops() = runBlocking {
        val capture = AudioCaptureManager(context)
        val firstChunk = CompletableDeferred<ByteArray>()
        val started = capture.start(scope) { pcm, _ -> firstChunk.complete(pcm) }
        assumeTrue("No microphone available on this device/emulator", started)
        val chunk = withTimeout(5_000) { firstChunk.await() }
        // 100 ms at 16 kHz, 16-bit mono = 3200 bytes (a short first read is allowed).
        assertEquals(0, chunk.size % 2)
        assertTrue(chunk.size <= PcmAudioFormat.GeminiLiveInput.bytesFor(100))
        capture.stop()
        delay(300)
        assertFalse(capture.isRunning)
    }

    @Test
    fun playbackQueuesDrainsAndClears() = runBlocking {
        val playback = AudioPlaybackManager()
        val drained = CompletableDeferred<Unit>()
        val started = playback.start(scope, onLevel = {}, onDrained = { drained.complete(Unit) })
        assumeTrue("No audio output available on this device/emulator", started)

        playback.enqueue(ByteArray(PcmAudioFormat.GeminiLiveOutput.bytesFor(200)))
        withTimeout(5_000) { drained.await() }
        assertFalse(playback.isPlaying)

        // Interruption: a long chunk is dropped immediately by clear().
        playback.enqueue(ByteArray(PcmAudioFormat.GeminiLiveOutput.bytesFor(3_000)))
        playback.clear()
        assertFalse(playback.isPlaying)
        playback.stop()
    }
}
