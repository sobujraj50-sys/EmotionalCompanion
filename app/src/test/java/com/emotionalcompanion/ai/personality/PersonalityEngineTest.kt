package com.emotionalcompanion.ai.personality

import com.emotionalcompanion.ai.gemini.TestDialogue
import com.emotionalcompanion.ai.gemini.VoiceConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityEngineTest {

    @Test
    fun honestyRulesAreAlwaysPresent() {
        // Every combination of the lowest and highest settings keeps the rules, at the end.
        listOf(Level.VeryLow, Level.VeryHigh).forEach { l ->
            val p = PersonalitySettings(
                affection = l, care = l, humor = l, romance = l, expressiveness = l,
                talkativeness = l, initiative = l, warmth = l, softness = l, energy = l, pace = l,
            )
            val instruction = PersonalityEngine.build(p)
            assertTrue(instruction.endsWith(PersonalityEngine.HONESTY_RULES))
            assertTrue(instruction.contains("Never claim to be the user's wife"))
        }
    }

    @Test
    fun nicknameIsIncludedAndCleaned() {
        val withName = PersonalityEngine.build(PersonalitySettings(nickname = "জান"))
        assertTrue(withName.contains("\"জান\""))
        val none = PersonalityEngine.build(PersonalitySettings(nickname = ""))
        assertFalse(none.contains("address the user affectionately"))
        assertEquals("a b", PersonalitySettings.cleanNickname("a\n\"b\""))
        assertEquals(PersonalitySettings.NICKNAME_MAX, PersonalitySettings.cleanNickname("x".repeat(100)).length)
    }

    @Test
    fun languagePreferenceChangesInstruction() {
        val bangla = PersonalityEngine.build(PersonalitySettings(language = LanguagePreference.BanglaFirst))
        val english = PersonalityEngine.build(PersonalitySettings(language = LanguagePreference.EnglishFirst))
        assertTrue(bangla.contains("mainly in natural Bangladeshi Bangla"))
        assertTrue(english.contains("mainly in English"))
    }

    @Test
    fun levelsMapToDifferentPhrases() {
        val quiet = PersonalityEngine.build(PersonalitySettings(talkativeness = Level.VeryLow))
        val chatty = PersonalityEngine.build(PersonalitySettings(talkativeness = Level.VeryHigh))
        assertTrue(quiet.contains("very short"))
        assertTrue(chatty.contains("chatty"))
    }

    @Test
    fun auditionInstructionAsksForExactReading() {
        val a = PersonalityEngine.auditionInstruction(PersonalitySettings())
        assertTrue(a.contains("exactly as written"))
    }

    @Test
    fun levelIndexing() {
        assertEquals(Level.Medium, Level.fromIndex(2))
        assertEquals(null, Level.fromIndex(9))
        assertEquals(null, Level.fromIndex(null))
    }

    @Test
    fun voiceCatalog() {
        assertEquals(30, VoiceConfiguration.all.size)
        assertEquals(30, VoiceConfiguration.all.map { it.name }.toSet().size)
        assertEquals(6, VoiceConfiguration.candidates.size)
        assertEquals("Sulafat", VoiceConfiguration.resolve(null))
        assertEquals("Kore", VoiceConfiguration.resolve("Kore"))
        assertEquals("Sulafat", VoiceConfiguration.resolve("NotAVoice"))
        assertTrue(TestDialogue.Bangla.text.startsWith("জান..."))
    }
}
