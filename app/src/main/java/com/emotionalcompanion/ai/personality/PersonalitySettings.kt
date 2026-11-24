package com.emotionalcompanion.ai.personality

/** Five-step scale used by every personality and voice-delivery preference. */
enum class Level { VeryLow, Low, Medium, High, VeryHigh;

    companion object {
        fun fromIndex(index: Int?): Level? = index?.let { entries.getOrNull(it) }
    }
}

enum class LanguagePreference { BanglaFirst, Mixed, EnglishFirst }

/**
 * User preferences turned into a system instruction by [PersonalityEngine].
 * They are preferences, not API parameters: Gemini Live has no native "warmth" control.
 */
data class PersonalitySettings(
    /** How she addresses the user, e.g. a pet name. Empty = no nickname. */
    val nickname: String = "",
    val language: LanguagePreference = LanguagePreference.BanglaFirst,
    // Character (spec §12)
    val affection: Level = Level.High,
    val care: Level = Level.High,
    val humor: Level = Level.Medium,
    val romance: Level = Level.Medium,
    val expressiveness: Level = Level.High,
    val talkativeness: Level = Level.Medium,
    val initiative: Level = Level.Medium,
    // Voice delivery (spec §11)
    val warmth: Level = Level.VeryHigh,
    val softness: Level = Level.High,
    val energy: Level = Level.Medium,
    val pace: Level = Level.Medium,
) {
    companion object {
        const val NICKNAME_MAX = 30

        /** Keeps the nickname a short single line, so it can't smuggle extra instructions in. */
        fun cleanNickname(input: String): String =
            input.replace(Regex("[\\r\\n\\t\"]"), " ").replace(Regex("\\s+"), " ").trim().take(NICKNAME_MAX)
    }
}
