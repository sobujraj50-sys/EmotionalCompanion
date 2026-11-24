package com.emotionalcompanion.ai.gemini

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Base64

/** Events decoded from Gemini Live server messages. */
sealed interface LiveEvent {
    data object SetupComplete : LiveEvent
    class Audio(val pcm: ByteArray) : LiveEvent
    data class InputTranscript(val text: String) : LiveEvent
    data class OutputTranscript(val text: String) : LiveEvent
    data object Interrupted : LiveEvent
    data object GenerationComplete : LiveEvent
    data object TurnComplete : LiveEvent
    data class GoAway(val timeLeftMs: Long?) : LiveEvent
    data class ResumptionUpdate(val handle: String?, val resumable: Boolean) : LiveEvent
    data class Usage(val totalTokens: Long) : LiveEvent
}

/**
 * Gemini Live WebSocket messages. Every field name here is from the official docs (2026-11-24):
 * - https://ai.google.dev/api/live (setup, realtimeInput, serverContent, goAway, sessionResumptionUpdate)
 * - https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket (endpoint, ?key=, audio mimeType)
 * - https://ai.google.dev/gemini-api/docs/live-guide (speechConfig.voiceConfig.prebuiltVoiceConfig.voiceName)
 * - https://ai.google.dev/gemini-api/docs/live-session (contextWindowCompression.slidingWindow, sessionResumption)
 */
object LiveProtocol {

    const val ENDPOINT =
        "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

    const val INPUT_MIME_TYPE = "audio/pcm;rate=16000"

    fun setupMessage(
        modelId: String,
        voiceName: String,
        systemInstruction: String,
        resumptionHandle: String?,
        /** Captions + resumption are off for one-shot voice auditions. */
        conversation: Boolean = true,
    ): String {
        val speechConfig = JSONObject().put(
            "voiceConfig",
            JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", voiceName)),
        )
        val generationConfig = JSONObject()
            .put("responseModalities", JSONArray().put("AUDIO"))
            .put("speechConfig", speechConfig)
        val resumption = JSONObject()
        if (!resumptionHandle.isNullOrEmpty()) resumption.put("handle", resumptionHandle)

        val setup = JSONObject()
            .put("model", "models/$modelId")
            .put("generationConfig", generationConfig)
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
        if (conversation) {
            setup.put("inputAudioTranscription", JSONObject())
                .put("outputAudioTranscription", JSONObject())
                // Removes the 15-minute audio session limit.
                .put("contextWindowCompression", JSONObject().put("slidingWindow", JSONObject()))
                // Lets a session survive the ~10-minute connection lifetime.
                .put("sessionResumption", resumption)
        }
        return JSONObject().put("setup", setup).toString()
    }

    fun audioMessage(pcm: ByteArray): String {
        // Built by hand: this runs 10 times per second and the payload is plain base64.
        val data = Base64.getEncoder().encodeToString(pcm)
        return "{\"realtimeInput\":{\"audio\":{\"data\":\"$data\",\"mimeType\":\"$INPUT_MIME_TYPE\"}}}"
    }

    /** Text turn (realtimeInput.text, per the WebSockets guide). */
    fun textMessage(text: String): String =
        JSONObject().put("realtimeInput", JSONObject().put("text", text)).toString()

    fun audioStreamEndMessage(): String = "{\"realtimeInput\":{\"audioStreamEnd\":true}}"

    /** Decodes one server message. Unknown fields are ignored; malformed JSON yields no events. */
    fun parse(text: String): List<LiveEvent> {
        val root = try {
            JSONObject(text)
        } catch (_: JSONException) {
            return emptyList()
        }
        val events = mutableListOf<LiveEvent>()

        if (root.has("setupComplete")) events += LiveEvent.SetupComplete

        root.optJSONObject("serverContent")?.let { content ->
            if (content.optBoolean("interrupted")) events += LiveEvent.Interrupted
            content.optJSONObject("modelTurn")?.optJSONArray("parts")?.let { parts ->
                for (i in 0 until parts.length()) {
                    val inline = parts.optJSONObject(i)?.optJSONObject("inlineData") ?: continue
                    val mime = inline.optString("mimeType")
                    val data = inline.optString("data")
                    if (data.isNotEmpty() && (mime.isEmpty() || mime.startsWith("audio/"))) {
                        try {
                            events += LiveEvent.Audio(Base64.getDecoder().decode(data))
                        } catch (_: IllegalArgumentException) {
                            // Skip a corrupt chunk rather than end the session.
                        }
                    }
                }
            }
            content.optJSONObject("inputTranscription")?.optString("text")
                ?.takeIf { it.isNotBlank() }?.let { events += LiveEvent.InputTranscript(it) }
            content.optJSONObject("outputTranscription")?.optString("text")
                ?.takeIf { it.isNotBlank() }?.let { events += LiveEvent.OutputTranscript(it) }
            if (content.optBoolean("generationComplete")) events += LiveEvent.GenerationComplete
            if (content.optBoolean("turnComplete")) events += LiveEvent.TurnComplete
        }

        root.optJSONObject("goAway")?.let { events += LiveEvent.GoAway(parseDurationMs(it.optString("timeLeft"))) }

        root.optJSONObject("sessionResumptionUpdate")?.let {
            val resumable = it.optBoolean("resumable")
            events += LiveEvent.ResumptionUpdate(it.optString("newHandle").takeIf { h -> resumable && h.isNotEmpty() }, resumable)
        }

        root.optJSONObject("usageMetadata")?.let { events += LiveEvent.Usage(it.optLong("totalTokenCount")) }

        return events
    }

    /** Protobuf JSON Duration, e.g. "5s" or "1.5s". */
    fun parseDurationMs(value: String?): Long? {
        if (value.isNullOrEmpty() || !value.endsWith("s")) return null
        return value.dropLast(1).toDoubleOrNull()?.let { (it * 1000).toLong() }
    }
}
