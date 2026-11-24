package com.emotionalcompanion.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Microphone capture: 16 kHz mono 16-bit PCM in 100 ms chunks (the Gemini Live input format).
 * Uses the VOICE_COMMUNICATION source, so the platform applies echo cancellation, plus the
 * AEC / noise-suppression effects when the device offers them.
 */
class AudioCaptureManager(private val context: Context) {

    private var record: AudioRecord? = null
    private var job: Job? = null

    /** True when the device reports a hardware/software acoustic echo canceler. */
    val echoCancellationAvailable: Boolean
        get() = AcousticEchoCanceler.isAvailable()

    val isRunning: Boolean get() = job?.isActive == true

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Starts capturing. [onChunk] receives each chunk and its level, on a background thread.
     * Returns false if permission is missing or the microphone can't be opened.
     */
    @SuppressLint("MissingPermission") // checked by hasPermission() right below
    fun start(scope: CoroutineScope, onChunk: (pcm: ByteArray, level: Float) -> Unit): Boolean {
        if (isRunning) return true
        if (!hasPermission()) return false

        val format = PcmAudioFormat.GeminiLiveInput
        val minBuffer = AudioRecord.getMinBufferSize(
            format.sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return false
        val chunkBytes = format.bytesFor(CHUNK_MS)

        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                format.sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, chunkBytes * 4),
            )
        } catch (_: IllegalArgumentException) {
            return false
        } catch (_: SecurityException) {
            return false
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return false
        }

        val aec = if (AcousticEchoCanceler.isAvailable()) {
            AcousticEchoCanceler.create(rec.audioSessionId)?.apply { enabled = true }
        } else null
        val ns = if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor.create(rec.audioSessionId)?.apply { enabled = true }
        } else null

        try {
            rec.startRecording()
        } catch (_: IllegalStateException) {
            aec?.release()
            ns?.release()
            rec.release()
            return false
        }
        record = rec

        job = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(chunkBytes)
            try {
                while (isActive) {
                    val read = rec.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val chunk = buffer.copyOf(read)
                        onChunk(chunk, AudioLevel.fromPcm16(chunk))
                    } else if (read < 0) {
                        break
                    }
                }
            } finally {
                aec?.release()
                ns?.release()
                rec.release()
            }
        }
        return true
    }

    /** Stops immediately. stop() unblocks the read; the capture coroutine then releases everything. */
    fun stop() {
        val rec = record
        record = null
        job?.cancel()
        job = null
        try {
            rec?.stop()
        } catch (_: IllegalStateException) {
            // Already stopped.
        }
    }

    private companion object {
        const val CHUNK_MS = 100
    }
}
