package com.emotionalcompanion

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emotionalcompanion.ai.gemini.LiveEvent
import com.emotionalcompanion.ai.gemini.LiveProtocol
import com.emotionalcompanion.core.security.KeystoreSecretStore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a device because it needs Android Keystore and the platform org.json. */
@RunWith(AndroidJUnit4::class)
class SecureStorageAndProtocolTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun keystoreRoundTripAndClear() = runBlocking {
        val store = KeystoreSecretStore(context)
        val fake = "TEST-not-a-real-key-0123456789abcdef"
        store.saveApiKey(fake)
        assertTrue(store.hasApiKey.value)
        // The stored value must not be the plain key.
        val raw = context.getSharedPreferences("secure_store", 0).all.values.joinToString()
        assertFalse(raw.contains(fake))
        assertEquals(fake, store.readApiKey())
        // A new instance (like after an app restart) reads the same key.
        assertEquals(fake, KeystoreSecretStore(context).readApiKey())
        store.clearApiKey()
        assertFalse(store.hasApiKey.value)
        assertNull(store.readApiKey())
    }

    @Test
    fun setupMessageMatchesDocumentedShape() {
        val setup = JSONObject(
            LiveProtocol.setupMessage("gemini-3.1-flash-live-preview", "Sulafat", "Be kind.", resumptionHandle = "h1"),
        ).getJSONObject("setup")
        assertEquals("models/gemini-3.1-flash-live-preview", setup.getString("model"))
        val gen = setup.getJSONObject("generationConfig")
        assertEquals("AUDIO", gen.getJSONArray("responseModalities").getString(0))
        assertEquals(
            "Sulafat",
            gen.getJSONObject("speechConfig").getJSONObject("voiceConfig")
                .getJSONObject("prebuiltVoiceConfig").getString("voiceName"),
        )
        assertEquals("Be kind.", setup.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text"))
        assertTrue(setup.getJSONObject("contextWindowCompression").has("slidingWindow"))
        assertEquals("h1", setup.getJSONObject("sessionResumption").getString("handle"))
        assertTrue(setup.has("inputAudioTranscription"))

        val audition = JSONObject(
            LiveProtocol.setupMessage("m", "Kore", "Read it.", resumptionHandle = null, conversation = false),
        ).getJSONObject("setup")
        assertFalse(audition.has("sessionResumption"))
        assertFalse(audition.has("inputAudioTranscription"))

        val text = JSONObject(LiveProtocol.textMessage("হ্যালো \"quote\""))
        assertEquals("হ্যালো \"quote\"", text.getJSONObject("realtimeInput").getString("text"))
    }

    @Test
    fun parsesServerMessages() {
        assertEquals(listOf(LiveEvent.SetupComplete), LiveProtocol.parse("""{"setupComplete":{}}"""))

        val audio = LiveProtocol.parse(
            """{"serverContent":{"modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/pcm;rate=24000","data":"AQID"}}]}}}""",
        )
        assertEquals(1, audio.size)
        assertArrayEquals(byteArrayOf(1, 2, 3), (audio[0] as LiveEvent.Audio).pcm)

        val turn = LiveProtocol.parse(
            """{"serverContent":{"interrupted":true,"inputTranscription":{"text":"কেমন আছো"},"outputTranscription":{"text":"ভালো"},"turnComplete":true}}""",
        )
        assertEquals(
            listOf(
                LiveEvent.Interrupted,
                LiveEvent.InputTranscript("কেমন আছো"),
                LiveEvent.OutputTranscript("ভালো"),
                LiveEvent.TurnComplete,
            ),
            turn,
        )

        assertEquals(listOf(LiveEvent.GoAway(5_000)), LiveProtocol.parse("""{"goAway":{"timeLeft":"5s"}}"""))
        assertEquals(
            listOf(LiveEvent.ResumptionUpdate("abc", true)),
            LiveProtocol.parse("""{"sessionResumptionUpdate":{"newHandle":"abc","resumable":true}}"""),
        )
        assertEquals(
            listOf(LiveEvent.ResumptionUpdate(null, false)),
            LiveProtocol.parse("""{"sessionResumptionUpdate":{"newHandle":"","resumable":false}}"""),
        )
        assertTrue(LiveProtocol.parse("not json").isEmpty())
    }
}
