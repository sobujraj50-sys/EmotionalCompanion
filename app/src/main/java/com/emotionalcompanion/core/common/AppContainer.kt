package com.emotionalcompanion.core.common

import android.content.Context
import com.emotionalcompanion.data.settings.SettingsRepository

/**
 * Manual DI container. Each milestone adds its dependencies here as lazily
 * created singletons, so providers and audio engines stay replaceable.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
}
