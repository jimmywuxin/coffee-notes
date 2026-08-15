package com.coffeelab.coffeenotes.util

import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.data.entity.PeakFlavorConfig
import com.coffeelab.coffeenotes.data.entity.RoastDegree

/**
 * 赏味期计算（纯函数，可独立测试）。
 *
 * 业务口径（重要）：
 * - 赏味期天数 = 从烘焙日起算的总可用天数（含养豆期在内），不再叠加 restDays
 * - 赏味期结束日 = 烘焙日 + 赏味期天数（peakFlavorDays）
 * - 倒计时提前 [NEARING_THRESHOLD_DAYS] 天开始（首页「即将结束」）
 */
object PeakFlavorCalculator {

    /** 赏味期默认天数（未配置烘焙度时的兜底） */
    const val DEFAULT_PEAK_FLAVOR_DAYS = 14

    /** 距赏味期结束提前 N 天开始倒计时 */
    const val NEARING_THRESHOLD_DAYS = 15

    private const val DAY_MS = 86_400_000L

    /** 赏味期结束日 = 烘焙日 + 赏味期天数 */
    fun peakEndDate(roastDate: Long, peakFlavorDays: Int): Long =
        roastDate + peakFlavorDays * DAY_MS

    /** 距赏味期结束剩余天数（向下取整） */
    fun daysLeft(roastDate: Long, peakFlavorDays: Int, now: Long): Int =
        ((peakEndDate(roastDate, peakFlavorDays) - now) / DAY_MS).toInt()

    /**
     * 解析豆子的赏味期天数：优先 bean.peakFlavorDays 手填值，
     * 否则按烘焙度匹配 peak_flavor_configs，最后兜底 [DEFAULT_PEAK_FLAVOR_DAYS]。
     */
    fun resolvePeakFlavorDays(
        bean: CoffeeBean,
        roastDegrees: List<RoastDegree>,
        peakConfigs: List<PeakFlavorConfig>
    ): Int? {
        if (bean.roastDate == null) return null
        return bean.peakFlavorDays
            ?: roastDegrees.find { it.name == bean.roastLevel }?.let { rd ->
                peakConfigs.find { it.roastDegreeId == rd.id }?.peakFlavorDays
            }
            ?: DEFAULT_PEAK_FLAVOR_DAYS
    }

    /**
     * 过滤「距赏味期结束 ≤ [NEARING_THRESHOLD_DAYS] 天」的豆子（含已到期未超阈值部分），
     * 返回 (豆子, 剩余天数) 并按剩余天数升序。
     */
    fun filterNearingEnd(
        beans: List<CoffeeBean>,
        roastDegrees: List<RoastDegree>,
        peakConfigs: List<PeakFlavorConfig>,
        now: Long
    ): List<Pair<CoffeeBean, Int>> {
        val threshold = now + NEARING_THRESHOLD_DAYS * DAY_MS
        return beans.mapNotNull { bean ->
            val peakDays = resolvePeakFlavorDays(bean, roastDegrees, peakConfigs) ?: return@mapNotNull null
            val end = peakEndDate(bean.roastDate!!, peakDays)
            if (end in now..threshold) {
                bean to daysLeft(bean.roastDate!!, peakDays, now)
            } else null
        }.sortedBy { it.second }
    }
}
