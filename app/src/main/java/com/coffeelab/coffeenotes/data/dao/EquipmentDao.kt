package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.Equipment
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipment ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<Equipment>>

    @Query("SELECT * FROM equipment ORDER BY sortOrder ASC")
    suspend fun getAllOnce(): List<Equipment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(equipment: Equipment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(equipmentList: List<Equipment>)

    @Update
    suspend fun update(equipment: Equipment)

    @Delete
    suspend fun delete(equipment: Equipment)

    @Query("DELETE FROM equipment")
    suspend fun deleteAll()

    @Query("SELECT MAX(sortOrder) FROM equipment")
    suspend fun getMaxSortOrder(): Int?
}
