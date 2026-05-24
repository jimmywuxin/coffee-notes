package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bean_impression_tags",
    foreignKeys = [
        ForeignKey(
            entity = CoffeeBean::class,
            parentColumns = ["id"],
            childColumns = ["beanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ImpressionTag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("beanId"), Index("tagId")]
)
data class BeanImpressionTag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val beanId: Long,
    val tagId: Long
)
