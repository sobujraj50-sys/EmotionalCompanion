package com.emotionalcompanion.features.home

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeOfDayTest {

    @Test
    fun boundaries() {
        assertEquals(TimeOfDay.Night, TimeOfDay.fromHour(4))
        assertEquals(TimeOfDay.Morning, TimeOfDay.fromHour(5))
        assertEquals(TimeOfDay.Morning, TimeOfDay.fromHour(11))
        assertEquals(TimeOfDay.Afternoon, TimeOfDay.fromHour(12))
        assertEquals(TimeOfDay.Afternoon, TimeOfDay.fromHour(16))
        assertEquals(TimeOfDay.Evening, TimeOfDay.fromHour(17))
        assertEquals(TimeOfDay.Evening, TimeOfDay.fromHour(20))
        assertEquals(TimeOfDay.Night, TimeOfDay.fromHour(21))
        assertEquals(TimeOfDay.Night, TimeOfDay.fromHour(0))
    }

    @Test
    fun everyHourHasAGreeting() {
        (0..23).forEach { TimeOfDay.fromHour(it) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidHour() {
        TimeOfDay.fromHour(24)
    }
}
