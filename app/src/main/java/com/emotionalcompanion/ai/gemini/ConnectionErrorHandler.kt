package com.emotionalcompanion.ai.gemini

enum class LiveError {
    NotConfigured,
    MicPermission,
    MicUnavailable,
    AudioBusy,
    NoInternet,
    Timeout,
    KeyRejected,
    PermissionDenied,
    QuotaExceeded,
    /** The local daily budget (Settings) is used up. */
    BudgetReached,
    ModelUnavailable,
    InvalidSetup,
    ServerError,
    ConnectionLost,
    Unknown,
}

data class ClassifiedError(val error: LiveError, val detail: String? = null) {
    /** Worth an automatic reconnect (with a resumption handle) before showing an error. */
    val retryable: Boolean
        get() = error == LiveError.ServerError || error == LiveError.ConnectionLost || error == LiveError.Timeout
}

/**
 * Maps WebSocket closes and transport failures to user-facing errors.
 * Close codes are standard WebSocket codes; Google's close-reason wording isn't documented,
 * so reasons are matched loosely and an unmatched reason is shown as detail.
 */
object ConnectionErrorHandler {

    fun fromClose(code: Int, reason: String): ClassifiedError? {
        if (code == 1000) return null // normal close
        val r = reason.lowercase()
        val error = when {
            "api key" in r || "api_key" in r -> LiveError.KeyRejected
            "quota" in r || "resource_exhausted" in r || "rate limit" in r || "exceeded" in r -> LiveError.QuotaExceeded
            "permission" in r || "denied" in r -> LiveError.PermissionDenied
            "not found" in r || ("model" in r && "not" in r && "support" in r) -> LiveError.ModelUnavailable
            code == 1007 || "invalid argument" in r -> LiveError.InvalidSetup
            code == 1008 -> LiveError.PermissionDenied
            code == 1011 || code == 1013 || code == 1014 -> LiveError.ServerError
            code == 1001 || code == 1006 -> LiveError.ConnectionLost
            else -> LiveError.Unknown
        }
        return ClassifiedError(error, sanitize(reason))
    }

    fun fromFailure(failure: TransportFailure): ClassifiedError = when (failure) {
        TransportFailure.NoInternet -> ClassifiedError(LiveError.NoInternet)
        TransportFailure.Timeout -> ClassifiedError(LiveError.Timeout)
        TransportFailure.Network -> ClassifiedError(LiveError.ConnectionLost)
        is TransportFailure.Http -> when (failure.code) {
            400 -> ClassifiedError(LiveError.KeyRejected)
            401, 403 -> ClassifiedError(LiveError.PermissionDenied)
            404 -> ClassifiedError(LiveError.ModelUnavailable)
            429 -> ClassifiedError(LiveError.QuotaExceeded)
            in 500..599 -> ClassifiedError(LiveError.ServerError)
            else -> ClassifiedError(LiveError.Unknown, "HTTP ${failure.code}")
        }
    }

    /** Close reasons come from Google and never contain the key, but are shortened and trimmed. */
    private fun sanitize(reason: String): String? =
        reason.trim().replace(Regex("key=[^\\s&]+"), "key=…").take(160).ifEmpty { null }
}
