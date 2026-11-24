package com.emotionalcompanion.ai.gemini

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import javax.net.ssl.HttpsURLConnection

sealed interface ConnectionTestResult {
    data object Success : ConnectionTestResult
    /** HTTP 400: Google rejected the request, almost always an invalid key. */
    data object KeyRejected : ConnectionTestResult
    /** HTTP 401/403: the key isn't allowed to use the Gemini API (API disabled or restricted key). */
    data object PermissionDenied : ConnectionTestResult
    /** HTTP 404: this model isn't available to this key. */
    data object ModelUnavailable : ConnectionTestResult
    /** HTTP 429: rate limit or free-tier quota reached. */
    data object QuotaExceeded : ConnectionTestResult
    data object ServerError : ConnectionTestResult
    data object NoInternet : ConnectionTestResult
    data object Timeout : ConnectionTestResult
    data class Unexpected(val httpCode: Int?) : ConnectionTestResult

    companion object {
        fun fromHttpCode(code: Int): ConnectionTestResult = when (code) {
            200 -> Success
            400 -> KeyRejected
            401, 403 -> PermissionDenied
            404 -> ModelUnavailable
            429 -> QuotaExceeded
            in 500..599 -> ServerError
            else -> Unexpected(code)
        }
    }
}

fun interface GeminiConnectionTester {
    suspend fun test(apiKey: String, modelId: String): ConnectionTestResult
}

/**
 * Checks the key and model with models.get (free, no tokens used):
 * GET https://generativelanguage.googleapis.com/v1beta/models/{model}?key=API_KEY
 * (https://ai.google.dev/api/models, checked 2026-11-24).
 * The URL contains the key, so neither the URL nor exception messages are ever logged.
 */
class HttpGeminiConnectionTester : GeminiConnectionTester {

    override suspend fun test(apiKey: String, modelId: String): ConnectionTestResult =
        withContext(Dispatchers.IO) {
            val url = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                    URLEncoder.encode(modelId, "UTF-8") +
                    "?key=" + URLEncoder.encode(apiKey, "UTF-8"),
            )
            var connection: HttpsURLConnection? = null
            try {
                connection = (url.openConnection() as HttpsURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    useCaches = false
                }
                ConnectionTestResult.fromHttpCode(connection.responseCode)
            } catch (_: UnknownHostException) {
                ConnectionTestResult.NoInternet
            } catch (_: SocketTimeoutException) {
                ConnectionTestResult.Timeout
            } catch (_: IOException) {
                ConnectionTestResult.Unexpected(null)
            } finally {
                connection?.disconnect()
            }
        }

    private companion object {
        const val TIMEOUT_MS = 12_000
    }
}
