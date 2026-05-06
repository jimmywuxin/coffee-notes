package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grinders")
data class Grinder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
) {
    companion object {
        val DEFAULT_GRINDERS = listOf(
            "泰摩C5",
            "司令官C40"
        )
    }
}
