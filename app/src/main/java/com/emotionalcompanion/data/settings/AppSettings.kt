package com.emotionalcompanion.data.settings

data class AppSettings(
    val onboardingDone: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.Dark,
    val reducedMotion: Boolean = false,
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
