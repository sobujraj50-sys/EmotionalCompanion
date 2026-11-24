package com.emotionalcompanion.core.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class PcmAudioFormatTest {

    @Test
    fun geminiInput_is16kHzMono16Bit() {
        val f = PcmAudioFormat.GeminiLiveInput
        assertEquals(16_000, f.sampleRateHz)
        assertEquals(2, f.bytesPerFrame)
        assertEquals(32_000, f.bytesPerSecond)
    }

    @Test
    fun geminiOutput_is24kHzMono16Bit() {
        assertEquals(48_000, PcmAudioFormat.GeminiLiveOutput.bytesPerSecond)
    }

    @Test
    fun bytesFor_100msInputChunk() {
        // 16000 samples/s * 0.1 s * 2 bytes = 3200 bytes
        assertEquals(3_200, PcmAudioFormat.GeminiLiveInput.bytesFor(100))
    }

    @Test
    fun durationMsOf_roundTrips() {
        val f = PcmAudioFormat.GeminiLiveOutput
        assertEquals(250L, f.durationMsOf(f.bytesFor(250)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects8BitPcm() {
        PcmAudioFormat(sampleRateHz = 16_000, bitsPerSample = 8)
    }
}
