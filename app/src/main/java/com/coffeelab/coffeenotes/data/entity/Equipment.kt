package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipment")
data class Equipment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
) {
    companion object {
        val DEFAULT_EQUIPMENT = listOf(
            "V60",
            "蛋糕滤杯",
            "折纸滤杯",
            "气动咖啡机",
            "爱乐压",
            "法压壶",
            "意式咖啡机"
        )
    }
}
