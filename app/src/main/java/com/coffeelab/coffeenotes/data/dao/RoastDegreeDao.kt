package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.RoastDegree
import kotlinx.coroutines.flow.Flow

@Dao
interface RoastDegreeDao {
    @Query("SELECT * FROM roast_degrees ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<RoastDegree>>

    @Query("SELECT * FROM roast_degrees ORDER BY sortOrder ASC")
    suspend fun getAllOnce(): List<RoastDegree>

    @Query("SELECT * FROM roast_degrees WHERE id = :id")
    suspend fun getById(id: Long): RoastDegree?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(roastDegree: RoastDegree): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<RoastDegree>)

    @Update
    suspend fun update(roastDegree: RoastDegree)

    @Delete
    suspend fun delete(roastDegree: RoastDegree)

    @Query("SELECT MAX(sortOrder) FROM roast_degrees")
    suspend fun getMaxSortOrder(): Int?
}
