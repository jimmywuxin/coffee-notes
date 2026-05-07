package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.BrewMethod
import kotlinx.coroutines.flow.Flow

@Dao
interface BrewMethodDao {
    @Query("SELECT * FROM brew_methods ORDER BY isPreset DESC, updatedAt DESC")
    fun getAllMethods(): Flow<List<BrewMethod>>

    @Query("SELECT * FROM brew_methods ORDER BY isPreset DESC, updatedAt DESC")
    suspend fun getAllOnce(): List<BrewMethod>

    @Query("SELECT * FROM brew_methods WHERE id = :id")
    suspend fun getMethodById(id: Long): BrewMethod?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(method: BrewMethod): Long

    @Update
    suspend fun update(method: BrewMethod)

    @Delete
    suspend fun delete(method: BrewMethod)

    @Query("DELETE FROM brew_methods")
    suspend fun deleteAll()
}
