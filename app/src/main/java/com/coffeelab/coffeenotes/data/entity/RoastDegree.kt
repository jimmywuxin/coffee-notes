package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "roast_degrees")
data class RoastDegree(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
) {
    companion object {
        val DEFAULT_ROAST_DEGREES = listOf(
            "极浅烘",
            "浅烘",
            "中浅",
            "中烘",
            "中深",
            "深烘"
        )
    }
}
