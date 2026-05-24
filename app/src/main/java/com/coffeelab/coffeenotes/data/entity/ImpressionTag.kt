package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "impression_tags")
data class ImpressionTag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
) {
    companion object {
        val DEFAULT_TAGS = listOf(
            "甜感突出",
            "回甘悠长",
            "清爽",
            "浓郁",
            "平衡",
            "复杂",
            "干净",
            "厚重"
        )
    }
}
