package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brew_recipes")
data class BrewRecipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val beanId: Long? = null,
    val equipment: String = "",
    val coffeeWeight: Double = 0.0,
    val waterWeight: Double = 0.0,
    val waterTemp: Double = 0.0,
    val grindSize: String = "",
    val bloomTime: Int = 0,
    val pourCount: Int = 0,
    val totalTime: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
