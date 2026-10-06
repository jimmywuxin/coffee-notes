package com.coffeelab.coffeenotes.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val day = 24L * 60 * 60 * 1000
    private val today = 1_700_000_000_000L / day * day   // 对齐到某个整日边界

    private fun daysAgo(n: Int) = today - n * day

    @Test
    fun `空列表返回 0`() {
        assertEquals(0, StreakCalculator.streakDays(emptyList(), today, day))
    }

    @Test
    fun `今天有记录时从今天起算`() {
        assertEquals(1, StreakCalculator.streakDays(listOf(today + 3600_000), today, day))
    }

    @Test
    fun `今天没冲昨天冲了从昨天起算`() {
        assertEquals(1, StreakCalculator.streakDays(listOf(daysAgo(1) + 3600_000), today, day))
    }

    @Test
    fun `连续三天`() {
        val times = (0..2).map { daysAgo(it) + 3600_000 }
        assertEquals(3, StreakCalculator.streakDays(times, today, day))
    }

    @Test
    fun `中间断一天则只算到断点前`() {
        // 今天、昨天有，前天没有，大前天有 => 2
        val times = listOf(today + 3600_000, daysAgo(1) + 3600_000, daysAgo(3) + 3600_000)
        assertEquals(2, StreakCalculator.streakDays(times, today, day))
    }

    @Test
    fun `最后一条记录远早于今天时必须终止且返回 0`() {
        //回归测试：原实现是 while(true)，所有break 都依赖 streak 增长，
        // streak 恒为 0 时无限回溯 → 长期未打开 app 后首页 ANR
        val ancient = listOf(daysAgo(500) + 3600_000)
        assertEquals(0, StreakCalculator.streakDays(ancient, today, day))
    }

    @Test
    fun `长期未打开但历史连续时不超上限`() {
        // 300 天连续，每天一条 => 期望 300（不超 366 上限）
        val times = (0 until 300).map { daysAgo(it) + 3600_000 }
        assertEquals(300, StreakCalculator.streakDays(times, today, day))
    }
}
