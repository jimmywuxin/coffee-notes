package com.coffeelab.coffeenotes.util

import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.data.entity.PeakFlavorConfig
import com.coffeelab.coffeenotes.data.entity.RoastDegree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeakFlavorCalculatorTest {

    private val dayMs = 86_400_000L
    private val roastDate = 1_700_000_000_000L

    private fun bean(
        id: Long = 1,
        peakFlavorDays: Int? = null,
        roastLevel: String = "浅烘",
        date: Long? = roastDate
    ) = CoffeeBean(
        id = id, roaster = "", name = "测试豆", origin = "", region = "", estate = "",
        variety = "", process = "", roastLevel = roastLevel, grindSize = "",
        roastDate = date, notes = "", localPhotoPaths = emptyList(), imageUri = "",
        isFavorite = false, isArchived = false, sortOrder = 0,
        createdAt = 0, updatedAt = 0, peakFlavorDays = peakFlavorDays
    )

    @Test
    fun `赏味期结束日等于烘焙日加赏味期天数`() {
        assertEquals(roastDate + 35 * dayMs, PeakFlavorCalculator.peakEndDate(roastDate, 35))
    }

    @Test
    fun `手填 peakFlavorDays 优先于配置`() {
        val roastDegree = RoastDegree(id = 1, name = "浅烘", sortOrder = 0)
        val config = PeakFlavorConfig(id = 1, roastDegreeId = 1, peakFlavorDays = 30)
        val result = PeakFlavorCalculator.resolvePeakFlavorDays(
            bean(peakFlavorDays = 21), listOf(roastDegree), listOf(config)
        )
        assertEquals(21, result)
    }

    @Test
    fun `未手填时按烘焙度匹配配置`() {
        val roastDegree = RoastDegree(id = 1, name = "浅烘", sortOrder = 0)
        val config = PeakFlavorConfig(id = 1, roastDegreeId = 1, peakFlavorDays = 30)
        val result = PeakFlavorCalculator.resolvePeakFlavorDays(
            bean(peakFlavorDays = null), listOf(roastDegree), listOf(config)
        )
        assertEquals(30, result)
    }

    @Test
    fun `无配置时兜底默认 14 天`() {
        val result = PeakFlavorCalculator.resolvePeakFlavorDays(bean(), emptyList(), emptyList())
        assertEquals(PeakFlavorCalculator.DEFAULT_PEAK_FLAVOR_DAYS, result)
    }

    @Test
    fun `无烘焙日期返回 null`() {
        val result = PeakFlavorCalculator.resolvePeakFlavorDays(
            bean(date = null), emptyList(), emptyList()
        )
        assertNull(result)
    }

    @Test
    fun `窗口过滤只保留临近结束的豆子并按剩余天数升序`() {
        val now = 2_000_000_000_000L
        // 烘焙日相同（now），天数不同：3 天/20 天/10 天后结束（20 天超出 15 天窗口应被排除）
        fun b(id: Long, days: Int) = bean(id = id, peakFlavorDays = days, date = now)
        val beans = listOf(b(1, 3), b(2, 20), b(3, 10))
        val result = PeakFlavorCalculator.filterNearingEnd(beans, emptyList(), emptyList(), now)
        assertEquals(listOf(1L, 3L), result.map { it.first.id })
        assertEquals(3, result[0].second)
        assertEquals(10, result[1].second)
    }
}
