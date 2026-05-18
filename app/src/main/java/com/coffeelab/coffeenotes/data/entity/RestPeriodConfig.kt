package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rest_period_configs",
    foreignKeys = [
        ForeignKey(
            entity = RoastDegree::class,
            parentColumns = ["id"],
            childColumns = ["roastDegreeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("roastDegreeId")]
)
data class RestPeriodConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roastDegreeId: Long,
    val restDays: Int  // 养豆天数
)
