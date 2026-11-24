package com.emotionalcompanion.core.common

import android.content.Context

/**
 * Manual DI container. Each later milestone adds its dependencies here as
 * lazily created singletons (e.g. SecureKeyStore in C, LiveSessionManager in D),
 * so that providers and audio engines stay replaceable behind interfaces.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
}
