package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "process_methods")
data class ProcessMethod(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
) {
    companion object {
        val DEFAULT_PROCESS_METHODS = listOf(
            "水洗",
            "日晒",
            "蜜处理",
            "厌氧",
            "其他"
        )
    }
}
