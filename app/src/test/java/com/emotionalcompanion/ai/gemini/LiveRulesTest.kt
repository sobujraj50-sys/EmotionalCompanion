package com.emotionalcompanion.ai.gemini

import com.emotionalcompanion.core.audio.AudioLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRulesTest {

    private val limits = UsageLimits(sessionMaxMinutes = 10, dailyBudgetMinutes = 30, idleTimeoutSeconds = 60)

    @Test
    fun usageRules() {
        assertTrue(UsageRules.canStart(limits, usedTodaySeconds = 0))
        assertFalse(UsageRules.canStart(limits, usedTodaySeconds = 30 * 60))
        assertNull(UsageRules.endReason(limits, sessionSeconds = 30, idleSeconds = 5, usedTodaySeconds = 100))
        assertEquals(SessionEndReason.SessionLimit, UsageRules.endReason(limits, 600, 0, 600))
        assertEquals(SessionEndReason.Idle, UsageRules.endReason(limits, 100, 60, 100))
        assertEquals(SessionEndReason.DailyBudget, UsageRules.endReason(limits, 100, 0, 1800))
    }

    @Test
    fun closeCodes() {
        assertNull(ConnectionErrorHandler.fromClose(1000, ""))
        assertEquals(LiveError.KeyRejected, ConnectionErrorHandler.fromClose(1008, "API key not valid.")?.error)
        assertEquals(LiveError.QuotaExceeded, ConnectionErrorHandler.fromClose(1011, "You exceeded your current quota")?.error)
        assertEquals(LiveError.InvalidSetup, ConnectionErrorHandler.fromClose(1007, "Request contains an invalid argument.")?.error)
        assertEquals(LiveError.ServerError, ConnectionErrorHandler.fromClose(1011, "Internal error")?.error)
        assertTrue(ConnectionErrorHandler.fromClose(1006, "")!!.retryable)
    }

    @Test
    fun closeReasonNeverEchoesAKey() {
        val detail = ConnectionErrorHandler.fromClose(1008, "bad url key=AIzaSECRET123 here")?.detail
        assertFalse(detail!!.contains("SECRET"))
    }

    @Test
    fun handshakeFailures() {
        assertEquals(LiveError.NoInternet, ConnectionErrorHandler.fromFailure(TransportFailure.NoInternet).error)
        assertEquals(LiveError.QuotaExceeded, ConnectionErrorHandler.fromFailure(TransportFailure.Http(429)).error)
        assertEquals(LiveError.PermissionDenied, ConnectionErrorHandler.fromFailure(TransportFailure.Http(403)).error)
    }

    @Test
    fun durations() {
        assertEquals(5_000L, LiveProtocol.parseDurationMs("5s"))
        assertEquals(1_500L, LiveProtocol.parseDurationMs("1.5s"))
        assertNull(LiveProtocol.parseDurationMs("abc"))
        assertNull(LiveProtocol.parseDurationMs(null))
    }

    @Test
    fun audioMessageShape() {
        val msg = LiveProtocol.audioMessage(byteArrayOf(1, 2, 3))
        assertEquals("{\"realtimeInput\":{\"audio\":{\"data\":\"AQID\",\"mimeType\":\"audio/pcm;rate=16000\"}}}", msg)
    }

    @Test
    fun audioLevels() {
        assertEquals(0f, AudioLevel.fromPcm16(ByteArray(320)))
        // Full-scale square wave (±32767) → maximum level.
        val loud = ByteArray(320) { i -> if (i % 4 < 2) (if (i % 2 == 0) 0xFF else 0x7F).toByte() else (if (i % 2 == 0) 0x01 else 0x80).toByte() }
        assertTrue(AudioLevel.fromPcm16(loud) > 0.99f)
        assertEquals(0f, AudioLevel.fromPcm16(ByteArray(0)))
    }
}
