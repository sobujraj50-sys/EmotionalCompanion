package com.emotionalcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.emotionalcompanion.core.designsystem.EmotionalCompanionTheme
import com.emotionalcompanion.features.home.FoundationScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EmotionalCompanionTheme {
                FoundationScreen()
            }
        }
    }
}
