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
    val region: String = "",
    val estate: String = "",
    val variety: String = "",
    val process: String = "",
    val roastLevel: String = "",
    val grindSize: String = "泰摩C5",
    val roastDate: Long? = null,
    val notes: String = "",
    val localPhotoPaths: List<String> = emptyList(),
    val imageUri: String = "",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // 官方萃取建议
    val extractionMethod: String? = null,   // 器具：手冲/意式/AeroPress/冷萃/法压
    val dose: Float? = null,               // 粉量（克）
    val brewRatio: String? = null,          // 粉水比，如 "1:15"
    val waterAmount: Float? = null,         // 注水量（毫升）
    val brewTime: Int? = null,               // 萃取时间（秒）
    val waterTemp: Int? = null,              // 水温（°C）
    val pouringDurationSeconds: Int? = null,  // 注水时长（秒），选填
    // 养豆/赏味期（手动覆盖，空则按烘焙度配置计算）
    val restDays: Int? = null,
    val peakFlavorDays: Int? = null
)
