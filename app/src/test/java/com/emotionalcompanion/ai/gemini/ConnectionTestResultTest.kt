package com.emotionalcompanion.ai.gemini

import com.emotionalcompanion.core.security.ApiKeyValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionTestResultTest {

    @Test
    fun mapsHttpCodes() {
        assertEquals(ConnectionTestResult.Success, ConnectionTestResult.fromHttpCode(200))
        assertEquals(ConnectionTestResult.KeyRejected, ConnectionTestResult.fromHttpCode(400))
        assertEquals(ConnectionTestResult.PermissionDenied, ConnectionTestResult.fromHttpCode(401))
        assertEquals(ConnectionTestResult.PermissionDenied, ConnectionTestResult.fromHttpCode(403))
        assertEquals(ConnectionTestResult.ModelUnavailable, ConnectionTestResult.fromHttpCode(404))
        assertEquals(ConnectionTestResult.QuotaExceeded, ConnectionTestResult.fromHttpCode(429))
        assertEquals(ConnectionTestResult.ServerError, ConnectionTestResult.fromHttpCode(503))
        assertEquals(ConnectionTestResult.Unexpected(418), ConnectionTestResult.fromHttpCode(418))
    }

    @Test
    fun keyValidator() {
        assertEquals(ApiKeyValidator.Problem.Empty, ApiKeyValidator.validate(""))
        assertEquals(ApiKeyValidator.Problem.ContainsWhitespace, ApiKeyValidator.validate("abc def ghi jkl mno pqr stu"))
        assertEquals(ApiKeyValidator.Problem.TooShort, ApiKeyValidator.validate("short"))
        assertNull(ApiKeyValidator.validate("a".repeat(39)))
    }
}
