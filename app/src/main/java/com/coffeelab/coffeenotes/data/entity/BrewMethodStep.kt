package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单个冲煮步骤
 * @param waterAmount 注水量（ml），null 表示至总水量
 * @param durationSeconds 持续时间（秒）
 * @param description 步骤描述（水流、注水方式等提示）
 */
data class BrewMethodStep(
    val waterAmount: Float?,   // null = 至总水量
    val durationSeconds: Int,
    val description: String? = null  // 水流、注水方式等描述
)
