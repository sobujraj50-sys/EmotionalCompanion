package com.emotionalcompanion.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    }

    val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            AppSettings(
                onboardingDone = p[Keys.OnboardingDone] ?: false,
                themeMode = ThemeMode.fromStorage(p[Keys.ThemeModeKey]),
                reducedMotion = p[Keys.ReducedMotion] ?: false,
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
}
