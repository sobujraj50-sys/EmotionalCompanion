package com.emotionalcompanion.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the API key with AES-256-GCM. The AES key lives in Android Keystore and never
 * leaves it. Only ciphertext + IV are stored, in app-private preferences, which are excluded
 * from backup and device transfer (data_extraction_rules.xml).
 */
class KeystoreSecretStore(context: Context) : SecretStore {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val _hasApiKey = MutableStateFlow(prefs.contains(KEY_CIPHERTEXT))
    override val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    override suspend fun saveApiKey(value: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            prefs.edit()
                .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .commit()
            _hasApiKey.value = true
        }
    }

    override suspend fun readApiKey(): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val ct = prefs.getString(KEY_CIPHERTEXT, null) ?: return@withLock null
            val iv = prefs.getString(KEY_IV, null) ?: return@withLock null
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateKey(),
                    GCMParameterSpec(GCM_TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)),
                )
                String(cipher.doFinal(Base64.decode(ct, Base64.NO_WRAP)), Charsets.UTF_8)
            } catch (e: GeneralSecurityException) {
                // Keystore key lost (e.g. lock screen reset): the stored key is unrecoverable.
                clearLocked()
                null
            } catch (e: IllegalArgumentException) {
                clearLocked()
                null
            }
        }
    }

    override suspend fun clearApiKey() = withContext(Dispatchers.IO) {
        mutex.withLock { clearLocked() }
    }

    private fun clearLocked() {
        prefs.edit().clear().commit()
        try {
            keyStore().deleteEntry(KEY_ALIAS)
        } catch (_: GeneralSecurityException) {
            // Nothing more to delete.
        }
        _hasApiKey.value = false
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        (keyStore().getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "ec_gemini_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PREFS_NAME = "secure_store"
        const val KEY_CIPHERTEXT = "gemini_key_ct"
        const val KEY_IV = "gemini_key_iv"
    }
}
