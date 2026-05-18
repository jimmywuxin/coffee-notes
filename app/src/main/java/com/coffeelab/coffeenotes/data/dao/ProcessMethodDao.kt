package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.ProcessMethod
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessMethodDao {
    @Query("SELECT * FROM process_methods ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<ProcessMethod>>

    @Query("SELECT * FROM process_methods ORDER BY sortOrder ASC")
    suspend fun getAllOnce(): List<ProcessMethod>

    @Query("SELECT * FROM process_methods WHERE id = :id")
    suspend fun getById(id: Long): ProcessMethod?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(processMethod: ProcessMethod): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ProcessMethod>)

    @Update
    suspend fun update(processMethod: ProcessMethod)

    @Delete
    suspend fun delete(processMethod: ProcessMethod)

    @Query("SELECT MAX(sortOrder) FROM process_methods")
    suspend fun getMaxSortOrder(): Int?
}
