package com.coffeelab.coffeenotes.util

/**
 * 连续冲煮天数。从今天往前数，允许「今天还没冲、昨天冲了」从昨天起算。
 *
 * 上限 366 天：不能写成 while(true) + break 的组合——所有终止条件都依赖 streak 增长，
 * 而 streak 只在当天有冲煮时才++，若最后一条记录远早于今天就会无限回溯（曾导致首页 ANR）。
 */
object StreakCalculator {
    private const val MAX_DAYS = 366

    /** [recordTimes] 为冲煮时间戳（毫秒），只关心日期部分，无序无妨。 */
    fun streakDays(recordTimes: List<Long>, todayStart: Long, millisPerDay: Long): Int {
        if (recordTimes.isEmpty()) return 0
        var streak = 0
        var dayStart = todayStart
        repeat(MAX_DAYS) {
            val hasBrew = recordTimes.any { it >= dayStart && it < dayStart + millisPerDay }
            if (!hasBrew && streak > 0) return streak
            if (hasBrew) streak++
            dayStart -= millisPerDay
        }
        return streak
    }
}
