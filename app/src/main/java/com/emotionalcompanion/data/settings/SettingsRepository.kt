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
}
