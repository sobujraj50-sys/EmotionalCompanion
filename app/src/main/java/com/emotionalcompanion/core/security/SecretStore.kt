package com.emotionalcompanion.core.security

import kotlinx.coroutines.flow.StateFlow

/** Local secret storage. Implementations must never log or export the secret. */
interface SecretStore {
    val hasApiKey: StateFlow<Boolean>
    suspend fun saveApiKey(value: String)
    /** Null when no key is saved, or when it can no longer be decrypted (it's then cleared). */
    suspend fun readApiKey(): String?
    suspend fun clearApiKey()
}

/** Basic sanity checks before saving. Google's key format isn't documented, so only obvious mistakes are rejected. */
object ApiKeyValidator {
    enum class Problem { Empty, ContainsWhitespace, TooShort }

    fun validate(input: String): Problem? = when {
        input.isEmpty() -> Problem.Empty
        input.any { it.isWhitespace() } -> Problem.ContainsWhitespace
        input.length < 20 -> Problem.TooShort
        else -> null
    }
}
