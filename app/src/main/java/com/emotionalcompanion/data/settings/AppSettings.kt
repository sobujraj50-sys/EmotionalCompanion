package com.emotionalcompanion.data.settings

import com.emotionalcompanion.ai.gemini.GeminiModelCatalog
import com.emotionalcompanion.ai.gemini.UsageLimits
import com.emotionalcompanion.ai.gemini.VoiceConfiguration
import com.emotionalcompanion.ai.personality.PersonalitySettings

data class AppSettings(
    val onboardingDone: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.Dark,
    val reducedMotion: Boolean = false,
    /** Free-only mode (spec §14): on by default; hides models without a confirmed free tier. */
    val freeOnly: Boolean = true,
    /** Live model ID, resolved through GeminiModelCatalog. */
    val liveModelId: String = GeminiModelCatalog.defaultModelId,
    /** User accepted the "audio is sent to Google" disclosure before the first conversation. */
    val audioDisclosureAccepted: Boolean = false,
    /** Full duplex: the user can interrupt by speaking. Off = half duplex (mic muted while the AI speaks). */
    val voiceInterruption: Boolean = true,
    val usageLimits: UsageLimits = UsageLimits(),
    /** Prebuilt Gemini voice, resolved through VoiceConfiguration. */
    val voiceName: String = VoiceConfiguration.DEFAULT_VOICE,
    val personality: PersonalitySettings = PersonalitySettings(),
)

/** Dark is the primary design (spec §7); light and follow-system are optional. */
enum class ThemeMode(val storageKey: String) {
    Dark("dark"),
    Light("light"),
    System("system");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == value } ?: Dark
    }
}
