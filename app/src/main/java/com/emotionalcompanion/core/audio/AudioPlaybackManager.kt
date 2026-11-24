package com.emotionalcompanion.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Streams the model's 24 kHz mono 16-bit PCM to an AudioTrack through a queue.
 * [clear] drops everything queued and already buffered (used for interruptions).
 */
class AudioPlaybackManager {

    private var track: AudioTrack? = null
    private var job: Job? = null
    private var queue = Channel<ByteArray>(Channel.UNLIMITED)
    private val pendingBytes = AtomicLong(0)
    private val generation = AtomicInteger(0)

    /** True while queued audio hasn't been written to the device yet. */
    val isPlaying: Boolean get() = pendingBytes.get() > 0

    /**
     * [onLevel] gets the level of each piece as it's written. [onDrained] fires when the
     * queue becomes empty. Both run on a background thread.
     */
    fun start(scope: CoroutineScope, onLevel: (Float) -> Unit, onDrained: () -> Unit): Boolean {
        if (job?.isActive == true) return true
        val format = PcmAudioFormat.GeminiLiveOutput
        val minBuffer = AudioTrack.getMinBufferSize(
            format.sampleRateHz,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return false

        val t = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(format.sampleRateHz)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minBuffer, format.bytesFor(200)))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: UnsupportedOperationException) {
            return false
        } catch (_: IllegalArgumentException) {
            return false
        }
        if (t.state != AudioTrack.STATE_INITIALIZED) {
            t.release()
            return false
        }
        t.play()
        track = t
        queue = Channel(Channel.UNLIMITED)
        pendingBytes.set(0)
        val pieceBytes = format.bytesFor(PIECE_MS)
        val localQueue = queue

        job = scope.launch(Dispatchers.IO) {
            try {
                for (chunk in localQueue) {
                    val gen = generation.get()
                    var offset = 0
                    while (offset < chunk.size && gen == generation.get()) {
                        val len = minOf(pieceBytes, chunk.size - offset)
                        val written = t.write(chunk, offset, len)
                        if (written <= 0) break
                        onLevel(AudioLevel.fromPcm16(chunk.copyOfRange(offset, offset + written)))
                        offset += written
                    }
                    if (gen == generation.get()) {
                        val left = pendingBytes.addAndGet(-chunk.size.toLong())
                        if (left <= 0) {
                            pendingBytes.set(0)
                            onLevel(0f)
                            onDrained()
                        }
                    }
                }
            } finally {
                try {
                    t.stop()
                } catch (_: IllegalStateException) {
                }
                t.release()
            }
        }
        return true
    }

    fun enqueue(pcm: ByteArray) {
        if (pcm.isEmpty() || job?.isActive != true) return
        pendingBytes.addAndGet(pcm.size.toLong())
        queue.trySend(pcm)
    }

    /** Drops all queued and buffered audio immediately. */
    fun clear() {
        generation.incrementAndGet()
        while (queue.tryReceive().isSuccess) { /* drain */ }
        pendingBytes.set(0)
        track?.let {
            try {
                it.pause()
                it.flush()
                it.play()
            } catch (_: IllegalStateException) {
            }
        }
    }

    fun stop() {
        generation.incrementAndGet()
        // Pausing unblocks a write() in progress, so the playback coroutine can finish.
        track?.let {
            try {
                it.pause()
                it.flush()
            } catch (_: IllegalStateException) {
            }
        }
        queue.close()
        job?.cancel()
        job = null
        track = null
        pendingBytes.set(0)
    }

    private companion object {
        const val PIECE_MS = 40
    }
}
