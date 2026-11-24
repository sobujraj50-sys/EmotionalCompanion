package com.emotionalcompanion

import android.app.Application
import com.emotionalcompanion.core.common.AppContainer

/**
 * Application entry point. Owns the manual dependency-injection container.
 * See docs/ARCHITECTURE.md ("Dependency injection") for why Hilt is not used yet.
 */
class EmotionalCompanionApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
