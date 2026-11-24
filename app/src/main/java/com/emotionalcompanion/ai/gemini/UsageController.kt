package com.emotionalcompanion.ai.gemini

import com.emotionalcompanion.data.settings.SettingsRepository
import java.time.LocalDate

data class UsageLimits(
    val sessionMaxMinutes: Int = 10,
    val dailyBudgetMinutes: Int = 30,
    val idleTimeoutSeconds: Int = 60,
)

enum class SessionEndReason { UserStopped, SessionLimit, DailyBudget, Idle, AudioFocusLost, AppInBackground, Error }

/** Pure rules for free-only safeguards (spec §14). Counts are local estimates, not Google billing data. */
object UsageRules {

    fun canStart(limits: UsageLimits, usedTodaySeconds: Int): Boolean =
        usedTodaySeconds < limits.dailyBudgetMinutes * 60

    fun endReason(
        limits: UsageLimits,
        sessionSeconds: Int,
        idleSeconds: Int,
        usedTodaySeconds: Int,
    ): SessionEndReason? = when {
        usedTodaySeconds >= limits.dailyBudgetMinutes * 60 -> SessionEndReason.DailyBudget
        sessionSeconds >= limits.sessionMaxMinutes * 60 -> SessionEndReason.SessionLimit
        idleSeconds >= limits.idleTimeoutSeconds -> SessionEndReason.Idle
        else -> null
    }
}

/** Stores today's locally counted conversation seconds. */
class UsageController(private val settings: SettingsRepository) {

    suspend fun usedTodaySeconds(today: LocalDate = LocalDate.now()): Int =
        settings.usageSecondsFor(today.toString())

    suspend fun add(seconds: Int, today: LocalDate = LocalDate.now()) {
        if (seconds > 0) settings.addUsageSeconds(today.toString(), seconds)
    }
}
