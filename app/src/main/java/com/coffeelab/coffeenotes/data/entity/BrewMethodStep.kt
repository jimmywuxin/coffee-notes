package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单个冲煮步骤
 * @param waterAmount 注水量（ml），null 表示至总水量
 * @param durationSeconds **累计**时间（秒）——指本步骤结束的时刻，从注水开始算起。
 *   例：步骤1=30s、步骤2=60s 表示第 60 秒时完成第二段注水；因此一份手法的
 *   **总时长 = 最后一步的 durationSeconds，不是各步相加**（2026-10-04  Jimmy 确认口径）
 * @param description 步骤描述（水流、注水方式等提示）
 */
data class BrewMethodStep(
    val waterAmount: Float?,   // null = 至总水量
    val durationSeconds: Int,
    val description: String? = null  // 水流、注水方式等描述
)
