package com.emotionalcompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test on a real Android system: the app starts, onboarding finishes, and every
 * top-level screen opens without crashing. Runs with the emulator in English.
 */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun finishOnboardingIfShown() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Skip").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Emotional Companion").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Skip").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Skip").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Get started").performClick()
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Gemini not set up").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Gemini key saved").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun homeShowsHonestStateWithoutKey() {
        finishOnboardingIfShown()
        compose.onNodeWithText("Emotional Companion").assertIsDisplayed()
        compose.onNodeWithText("Resting").assertExists()
        compose.onNodeWithText("This app is an AI companion. It is not a real person.").assertExists()
    }

    @Test
    fun voiceStudioOpensWithAllVoices() {
        finishOnboardingIfShown()
        compose.onAllNodesWithText("Voice")[0].performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Sulafat")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Show all 30 voices").performScrollTo().performClick()
        compose.onNodeWithText("Sadaltager").assertExists()
        compose.onNodeWithText("Edit personality").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Nickname for you").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun settingsOpensApiScreen() {
        finishOnboardingIfShown()
        compose.onAllNodesWithText("Settings")[0].performClick()
        compose.onNodeWithText("API key").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Paste your Gemini API key").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Free-only mode").performScrollTo().assertIsDisplayed()
    }
}
