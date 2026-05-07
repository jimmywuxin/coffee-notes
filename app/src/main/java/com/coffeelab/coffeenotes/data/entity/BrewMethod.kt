package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.coffeelab.coffeenotes.data.Converters

@Entity(tableName = "brew_methods")
@TypeConverters(Converters::class)
data class BrewMethod(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val isPreset: Boolean = false,        // 是否预置手法
    // Stored as JSON string in SQLite (TypeConverter: List<BrewMethodStep> ↔ String)
    // Backup: Gson serializes as JSON array in backup JSON
    val steps: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
