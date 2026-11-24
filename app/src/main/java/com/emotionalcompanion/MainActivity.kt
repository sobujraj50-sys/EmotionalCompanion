package com.emotionalcompanion

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emotionalcompanion.core.designsystem.EmotionalCompanionTheme
import com.emotionalcompanion.core.designsystem.LocalReducedMotion
import com.emotionalcompanion.data.settings.ThemeMode
import com.emotionalcompanion.features.onboarding.OnboardingScreen
import com.emotionalcompanion.navigation.AppNavHost

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as EmotionalCompanionApp).container }
    private val mainViewModel: MainViewModel by viewModels { MainViewModel.factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            val current = settings
            val darkTheme = when (current?.themeMode ?: ThemeMode.Dark) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                ThemeMode.System -> isSystemInDarkTheme()
            }

            // System bar icons follow the app's theme, not the phone's, so they stay readable.
            DisposableEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            EmotionalCompanionTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalReducedMotion provides (current?.reducedMotion ?: false)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        when {
                            current == null -> Unit // first settings read in progress
                            !current.onboardingDone -> OnboardingScreen(
                                onFinish = mainViewModel::completeOnboarding,
                            )
                            else -> AppNavHost(container = container)
                        }
                    }
                }
            }
        }
    }
}
