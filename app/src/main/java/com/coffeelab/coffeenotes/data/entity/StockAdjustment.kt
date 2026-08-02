package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 库存调整记录（快捷扣减等）
 *
 * 用于记录不产生冲煮记录但消耗了豆子的场景（如随手冲一杯没记参数、分装送人等），
 * 以及补录（如发现实际库存多于记录时）。
 * 库存计算口径：余量 = 累计购入 + Σ调整值 − 累计消耗。
 * 调整值为负表示扣减，为正表示补回。
 */
@Entity(
    tableName = "stock_adjustments",
    indices = [Index(value = ["beanId"])]
)
data class StockAdjustment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 关联豆子 ID */
    val beanId: Long,
    /** 调整克重：负=扣减，正=补回 */
    val changeGrams: Double,
    /** 备注（可选），如"分装送人 50g" */
    val note: String = "",
    /** 调整时间戳 */
    val createdAt: Long = System.currentTimeMillis()
)
