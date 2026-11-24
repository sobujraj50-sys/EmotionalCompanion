package com.emotionalcompanion.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Offline fallback voice: Android's built-in Text-to-Speech engine (spec §3, §14).
 * It can only read fixed text (it is not the AI), e.g. an offline notice or a voice sample.
 * Bangla works only if the phone's TTS engine has a Bangla voice installed.
 */
class OfflineSpeech(context: Context) {

    enum class Availability { Unknown, Ready, NoBangla, Unavailable }

    private val _availability = MutableStateFlow(Availability.Unknown)
    val availability: StateFlow<Availability> = _availability.asStateFlow()

    private var pending: Pair<String, Boolean>? = null
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status != TextToSpeech.SUCCESS) {
            _availability.value = Availability.Unavailable
        } else {
            _availability.value = if (supports(BANGLA)) Availability.Ready else Availability.NoBangla
            pending?.let { (text, bangla) -> speak(text, bangla) }
            pending = null
        }
    }

    private fun supports(locale: Locale): Boolean =
        tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE

    /** Returns false if it can't speak this text (no engine, or Bangla missing). */
    fun speak(text: String, bangla: Boolean): Boolean {
        when (_availability.value) {
            Availability.Unknown -> {
                pending = text to bangla
                return true
            }
            Availability.Unavailable -> return false
            Availability.NoBangla -> if (bangla) return false
            Availability.Ready -> Unit
        }
        tts.language = if (bangla) BANGLA else Locale.ENGLISH
        tts.setSpeechRate(0.95f)
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) == TextToSpeech.SUCCESS
    }

    fun stop() {
        tts.stop()
    }

    private companion object {
        val BANGLA: Locale = Locale.forLanguageTag("bn-BD")
        const val UTTERANCE_ID = "ec_offline"
    }
}
