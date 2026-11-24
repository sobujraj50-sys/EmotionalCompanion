package com.emotionalcompanion.data.settings

import com.emotionalcompanion.ai.gemini.GeminiModelCatalog

data class AppSettings(
    val onboardingDone: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.Dark,
    val reducedMotion: Boolean = false,
    /** Free-only mode (spec §14): on by default; hides models without a confirmed free tier. */
    val freeOnly: Boolean = true,
    /** Live model ID, resolved through GeminiModelCatalog. */
    val liveModelId: String = GeminiModelCatalog.defaultModelId,
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
