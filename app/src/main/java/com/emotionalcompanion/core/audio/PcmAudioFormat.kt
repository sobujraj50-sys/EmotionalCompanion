package com.emotionalcompanion.core.audio

/**
 * Raw PCM stream description.
 *
 * Values for the Gemini Live API come from https://ai.google.dev/gemini-api/docs/live
 * (checked 2026-11-24): input is raw 16-bit little-endian PCM at 16 kHz,
 * output is raw 16-bit little-endian PCM at 24 kHz. Mono is assumed and will be
 * re-confirmed in Milestone D (see docs/API_CONTRACT.md).
 */
data class PcmAudioFormat(
    val sampleRateHz: Int,
    val channelCount: Int = 1,
    val bitsPerSample: Int = 16,
) {
    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        require(channelCount in 1..2) { "channelCount must be 1 or 2" }
        require(bitsPerSample == 16) { "only 16-bit PCM is supported" }
    }

    val bytesPerFrame: Int get() = channelCount * bitsPerSample / 8

    val bytesPerSecond: Int get() = sampleRateHz * bytesPerFrame

    /** Bytes needed to hold [durationMs] of audio, rounded down to a whole frame. */
    fun bytesFor(durationMs: Int): Int {
        require(durationMs >= 0) { "durationMs must not be negative" }
        val frames = sampleRateHz.toLong() * durationMs / 1000
        return (frames * bytesPerFrame).toInt()
    }

    /** Playback duration of [byteCount] bytes, in milliseconds. */
    fun durationMsOf(byteCount: Int): Long {
        require(byteCount >= 0) { "byteCount must not be negative" }
        return byteCount.toLong() * 1000 / bytesPerSecond
    }

    companion object {
        val GeminiLiveInput = PcmAudioFormat(sampleRateHz = 16_000)
        val GeminiLiveOutput = PcmAudioFormat(sampleRateHz = 24_000)
    }
}
