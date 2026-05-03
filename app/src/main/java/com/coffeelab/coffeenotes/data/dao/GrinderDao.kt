package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.Grinder
import kotlinx.coroutines.flow.Flow

@Dao
interface GrinderDao {
    @Query("SELECT * FROM grinders ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<Grinder>>

    @Query("SELECT * FROM grinders ORDER BY sortOrder ASC")
    suspend fun getAllOnce(): List<Grinder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grinder: Grinder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(grinderList: List<Grinder>)

    @Update
    suspend fun update(grinder: Grinder)

    @Delete
    suspend fun delete(grinder: Grinder)

    @Query("DELETE FROM grinders")
    suspend fun deleteAll()

    @Query("SELECT MAX(sortOrder) FROM grinders")
    suspend fun getMaxSortOrder(): Int?
}
