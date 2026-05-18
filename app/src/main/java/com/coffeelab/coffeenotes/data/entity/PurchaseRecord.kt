package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
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
    val date: Long,           // 购买日期（时间戳）
    val weightGrams: Int,     // 克重
    val price: Float,         // 总价
    val unitPrice: Float      // 单价（自动计算：price / weightGrams）
)
