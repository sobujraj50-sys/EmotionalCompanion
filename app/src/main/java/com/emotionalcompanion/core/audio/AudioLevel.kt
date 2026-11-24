package com.emotionalcompanion.core.audio

import kotlin.math.log10
import kotlin.math.sqrt

/** Converts real PCM audio into a 0..1 level for the orb and waveform. */
object AudioLevel {

    private const val FLOOR_DB = -60.0

    /** RMS of 16-bit little-endian PCM, mapped on a dB scale (-60 dB → 0, 0 dB → 1). */
    fun fromPcm16(bytes: ByteArray, length: Int = bytes.size): Float {
        val samples = length / 2
        if (samples == 0) return 0f
        var sum = 0.0
        for (i in 0 until samples) {
            val lo = bytes[2 * i].toInt() and 0xFF
            val hi = bytes[2 * i + 1].toInt() // signed high byte
            val sample = (hi shl 8) or lo
            sum += sample.toDouble() * sample
        }
        val rms = sqrt(sum / samples) / 32768.0
        if (rms <= 1e-6) return 0f
        val db = 20 * log10(rms)
        return ((db - FLOOR_DB) / -FLOOR_DB).coerceIn(0.0, 1.0).toFloat()
    }
}
