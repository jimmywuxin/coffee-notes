package com.coffeelab.coffeenotes.util

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class DateUtilsTest {

    @Test
    fun `formatDate returns correct format`() {
        // 2026-01-15 00:00:00 CST = 1768406400000
        val result = DateUtils.formatDate(1768406400000L)
        assertEquals("2026/01/15", result)
    }

    @Test
    fun `formatDateTime returns correct format`() {
        val result = DateUtils.formatDateTime(1768406400000L)
        assertEquals("2026/01/15 00:00", result)
    }

    @Test
    fun `parseDate roundtrips with formatDate`() {
        val input = "2026/05/15"
        val parsed = DateUtils.parseDate(input)
        assertNotNull(parsed)
        assertEquals(input, DateUtils.formatDate(parsed!!))
    }

    @Test
    fun `parseDate returns null for invalid input`() {
        assertNull(DateUtils.parseDate("invalid"))
        assertNull(DateUtils.parseDate(""))
    }

    @Test
    fun `resolvePurchaseDate uses full timestamp for new record dated today`() {
        val now = java.time.LocalDate.of(2026, 11, 24)
            .atTime(15, 30)
            .atZone(ZoneId.of("Asia/Shanghai"))
            .toInstant().toEpochMilli()
        val today = DateUtils.formatDate(now)

        val resolved = DateUtils.resolvePurchaseDate(today, isNew = true, now = now)

        assertEquals(now, resolved)
        // 关键：晚于当天重置时刻，才不会被库存查询的 date >= stockResetAt 过滤
        assertTrue(resolved > DateUtils.parseDate(today)!!)
    }

    @Test
    fun `resolvePurchaseDate keeps midnight for edits and backfilled dates`() {
        val now = java.time.LocalDate.of(2026, 11, 24)
            .atTime(15, 30)
            .atZone(ZoneId.of("Asia/Shanghai"))
            .toInstant().toEpochMilli()
        val today = DateUtils.formatDate(now)

        assertEquals(
            DateUtils.parseDate(today),
            DateUtils.resolvePurchaseDate(today, isNew = false, now = now)
        )
        assertEquals(
            DateUtils.parseDate("2026/05/15"),
            DateUtils.resolvePurchaseDate("2026/05/15", isNew = true, now = now)
        )
    }

    @Test
    fun `getWeekStart returns Monday`() {
        // 2026-05-15 is a Friday, so week start should be 2026-05-11 (Monday)
        // 2026-05-11 00:00:00 CST
        val friday = java.time.LocalDate.of(2026, 5, 15)
            .atStartOfDay(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()
        val weekStart = DateUtils.getWeekStart(friday)
        val mondayStr = DateUtils.formatDate(weekStart)
        assertEquals("2026/05/11", mondayStr)
    }
}
