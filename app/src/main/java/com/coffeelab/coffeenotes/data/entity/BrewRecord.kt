package com.coffeelab.coffeenotes.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "brew_records",
    foreignKeys = [
        ForeignKey(
            entity = CoffeeBean::class,
            parentColumns = ["id"],
            childColumns = ["beanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Equipment::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Grinder::class,
            parentColumns = ["id"],
            childColumns = ["grinderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("beanId"), Index("equipmentId"), Index("grinderId")]
)
data class BrewRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val beanId: Long,
    val methodId: Long? = null,
    val dateTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "equipmentId")
    val equipmentId: Long? = null,
    val coffeeWeight: Double = 0.0,
    val coffeeWaterRatio: Double = 0.0,
    val waterAmount: Double = 0.0,
    val waterTemp: Double = 0.0,
    val grindSize: String = "",
    @ColumnInfo(name = "grinderId")
    val grinderId: Long? = null,
    val extractionTime: Int = 0,
    val pouringDurationSeconds: Int? = null,
    val acidity: Int = 0,
    val sweetness: Int = 0,
    val bitterness: Int = 0,
    val mouthfeel: Int = 0,
    val aftertaste: Int = 0,
    val overallRating: Int = 0,
    val flavorNotes: String = "",
    val imageUri: String = "",
    val isIced: Boolean = false,
    val iceAmount: Int = 0,
    val bypassAmount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    @Ignore
    val equipmentName: String? = null

    @Ignore
    val grinderName: String? = null
}
