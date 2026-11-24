package com.emotionalcompanion.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.emotionalcompanion.ai.gemini.GeminiModelCatalog
import com.emotionalcompanion.ai.gemini.UsageLimits
import com.emotionalcompanion.ai.gemini.VoiceConfiguration
import com.emotionalcompanion.ai.personality.LanguagePreference
import com.emotionalcompanion.ai.personality.Level
import com.emotionalcompanion.ai.personality.PersonalitySettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Non-secret UI preferences. Secrets (API key) never go here: see core/security (Milestone C). */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    private object Keys {
        val OnboardingDone = booleanPreferencesKey("onboarding_done")
        val ThemeModeKey = stringPreferencesKey("theme_mode")
        val ReducedMotion = booleanPreferencesKey("reduced_motion")
        val FreeOnly = booleanPreferencesKey("free_only")
        val LiveModelId = stringPreferencesKey("live_model_id")
        val AudioDisclosure = booleanPreferencesKey("audio_disclosure_accepted")
        val VoiceInterruption = booleanPreferencesKey("voice_interruption")
        val SessionMaxMinutes = intPreferencesKey("session_max_minutes")
        val DailyBudgetMinutes = intPreferencesKey("daily_budget_minutes")
        val IdleTimeoutSeconds = intPreferencesKey("idle_timeout_seconds")
        val UsageDay = stringPreferencesKey("usage_day")
        val VoiceName = stringPreferencesKey("voice_name")
        val Nickname = stringPreferencesKey("p_nickname")
        val Language = stringPreferencesKey("p_language")
        fun level(name: String) = intPreferencesKey("p_$name")
        val UsageSeconds = intPreferencesKey("usage_seconds")
    }

    val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val freeOnly = p[Keys.FreeOnly] ?: true
            AppSettings(
                onboardingDone = p[Keys.OnboardingDone] ?: false,
                themeMode = ThemeMode.fromStorage(p[Keys.ThemeModeKey]),
                reducedMotion = p[Keys.ReducedMotion] ?: false,
                freeOnly = freeOnly,
                liveModelId = GeminiModelCatalog.resolve(p[Keys.LiveModelId], freeOnly),
                audioDisclosureAccepted = p[Keys.AudioDisclosure] ?: false,
                voiceInterruption = p[Keys.VoiceInterruption] ?: true,
                usageLimits = UsageLimits(
                    sessionMaxMinutes = p[Keys.SessionMaxMinutes] ?: UsageLimits().sessionMaxMinutes,
                    dailyBudgetMinutes = p[Keys.DailyBudgetMinutes] ?: UsageLimits().dailyBudgetMinutes,
                    idleTimeoutSeconds = p[Keys.IdleTimeoutSeconds] ?: UsageLimits().idleTimeoutSeconds,
                ),
                voiceName = VoiceConfiguration.resolve(p[Keys.VoiceName]),
                personality = readPersonality(p),
            )
        }

    suspend fun setOnboardingDone() {
        dataStore.edit { it[Keys.OnboardingDone] = true }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.ThemeModeKey] = mode.storageKey }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        dataStore.edit { it[Keys.ReducedMotion] = enabled }
    }

    suspend fun setFreeOnly(enabled: Boolean) {
        dataStore.edit { it[Keys.FreeOnly] = enabled }
    }

    suspend fun setLiveModelId(id: String) {
        dataStore.edit { it[Keys.LiveModelId] = id }
    }

    suspend fun setAudioDisclosureAccepted() {
        dataStore.edit { it[Keys.AudioDisclosure] = true }
    }

    suspend fun setVoiceInterruption(enabled: Boolean) {
        dataStore.edit { it[Keys.VoiceInterruption] = enabled }
    }

    suspend fun setSessionMaxMinutes(minutes: Int) {
        dataStore.edit { it[Keys.SessionMaxMinutes] = minutes }
    }

    suspend fun setDailyBudgetMinutes(minutes: Int) {
        dataStore.edit { it[Keys.DailyBudgetMinutes] = minutes }
    }

    suspend fun setIdleTimeoutSeconds(seconds: Int) {
        dataStore.edit { it[Keys.IdleTimeoutSeconds] = seconds }
    }

    /** Local usage counter for [day] (ISO date). A new day starts from zero. */
    suspend fun usageSecondsFor(day: String): Int {
        val p = dataStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }.first()
        return if (p[Keys.UsageDay] == day) p[Keys.UsageSeconds] ?: 0 else 0
    }

    suspend fun addUsageSeconds(day: String, seconds: Int) {
        dataStore.edit {
            val current = if (it[Keys.UsageDay] == day) it[Keys.UsageSeconds] ?: 0 else 0
            it[Keys.UsageDay] = day
            it[Keys.UsageSeconds] = current + seconds
        }
    }

    suspend fun setVoiceName(name: String) {
        dataStore.edit { it[Keys.VoiceName] = VoiceConfiguration.resolve(name) }
    }

    suspend fun setPersonality(value: PersonalitySettings) {
        dataStore.edit { prefs ->
            prefs[Keys.Nickname] = PersonalitySettings.cleanNickname(value.nickname)
            prefs[Keys.Language] = value.language.name
            levelFields(value).forEach { (name, level) -> prefs[Keys.level(name)] = level.ordinal }
        }
    }

    private fun readPersonality(p: Preferences): PersonalitySettings {
        val d = PersonalitySettings()
        fun lvl(name: String, default: Level) = Level.fromIndex(p[Keys.level(name)]) ?: default
        return PersonalitySettings(
            nickname = p[Keys.Nickname] ?: d.nickname,
            language = LanguagePreference.entries.firstOrNull { it.name == p[Keys.Language] } ?: d.language,
            affection = lvl("affection", d.affection),
            care = lvl("care", d.care),
            humor = lvl("humor", d.humor),
            romance = lvl("romance", d.romance),
            expressiveness = lvl("expressiveness", d.expressiveness),
            talkativeness = lvl("talkativeness", d.talkativeness),
            initiative = lvl("initiative", d.initiative),
            warmth = lvl("warmth", d.warmth),
            softness = lvl("softness", d.softness),
            energy = lvl("energy", d.energy),
            pace = lvl("pace", d.pace),
        )
    }

    private fun levelFields(v: PersonalitySettings): List<Pair<String, Level>> = listOf(
        "affection" to v.affection,
        "care" to v.care,
        "humor" to v.humor,
        "romance" to v.romance,
        "expressiveness" to v.expressiveness,
        "talkativeness" to v.talkativeness,
        "initiative" to v.initiative,
        "warmth" to v.warmth,
        "softness" to v.softness,
        "energy" to v.energy,
        "pace" to v.pace,
    )
}
