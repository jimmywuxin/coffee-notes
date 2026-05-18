package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_records",
    foreignKeys = [
        ForeignKey(
            entity = CoffeeBean::class,
            parentColumns = ["id"],
            childColumns = ["beanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("beanId")]
)
data class PurchaseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val beanId: Long,
    val date: Long,           // 购买日期（时间戳，毫秒）
    val weightGrams: Int,     // 克重
    val price: Float,         // 总价
    val roastDate: Long? = null  // 烘焙日期（时间戳，毫秒），保存时自动同步到豆子
) {
    /** 单价：自动计算 = price / weightGrams */
    @get:Ignore
    val unitPrice: Float
        get() = if (weightGrams > 0) price / weightGrams else 0f
}
