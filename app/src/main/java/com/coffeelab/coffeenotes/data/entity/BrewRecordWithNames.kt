package com.coffeelab.coffeenotes.data.entity

import androidx.room.Embedded

/**
 * Wrapper for JOIN query results that include equipment/grinder names and bean info.
 * Not an @Entity – just a POJO for Room to map query columns into.
 */
data class BrewRecordWithNames(
    @Embedded
    val record: BrewRecord,
    val equipmentName: String?,
    val grinderName: String?,
    val beanName: String = "",
    val beanRoaster: String = ""
)
