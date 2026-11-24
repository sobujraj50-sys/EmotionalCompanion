package com.emotionalcompanion.features.home

import androidx.annotation.StringRes
import com.emotionalcompanion.R

/** Drives the contextual greeting on the Home Screen, from the phone's local time. */
enum class TimeOfDay(@StringRes val greetingRes: Int) {
    Morning(R.string.greeting_morning),
    Afternoon(R.string.greeting_afternoon),
    Evening(R.string.greeting_evening),
    Night(R.string.greeting_night);

    companion object {
        fun fromHour(hour: Int): TimeOfDay {
            require(hour in 0..23) { "hour must be 0..23, was $hour" }
            return when (hour) {
                in 5..11 -> Morning
                in 12..16 -> Afternoon
                in 17..20 -> Evening
                else -> Night
            }
        }
    }
}
