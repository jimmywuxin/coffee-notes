package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coffee_beans")
data class CoffeeBean(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roaster: String = "",
    val name: String = "",
    val origin: String = "",
    val estate: String = "",
    val variety: String = "",
    val process: String = "",
    val roastLevel: String = "",
    val roastDate: Long? = null,
    val notes: String = "",
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
