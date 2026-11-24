package com.emotionalcompanion.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun defaultsToDarkWhenNothingSaved() {
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.Dark, AppSettings().themeMode)
    }

    @Test
    fun unknownValueFallsBackToDark() {
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorage("purple"))
    }

    @Test
    fun storageKeysRoundTrip() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStorage(it.storageKey)) }
    }
}
