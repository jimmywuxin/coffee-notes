package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "peak_flavor_configs",
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
data class PeakFlavorConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roastDegreeId: Long,
    val peakFlavorDays: Int  // 赏味期天数
)
