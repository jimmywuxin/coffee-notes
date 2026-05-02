package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BrewRecordDao {
    @Query("SELECT * FROM brew_records ORDER BY dateTime DESC")
    fun getAllRecords(): Flow<List<BrewRecord>>

    @Query("SELECT * FROM brew_records WHERE id = :id")
    suspend fun getRecordById(id: Long): BrewRecord?

    @Query("SELECT * FROM brew_records WHERE beanId = :beanId ORDER BY dateTime DESC")
    fun getRecordsForBean(beanId: Long): Flow<List<BrewRecord>>

    @Query("SELECT * FROM brew_records WHERE equipment = :equipment ORDER BY dateTime DESC")
    fun getRecordsByEquipment(equipment: String): Flow<List<BrewRecord>>

    @Query("SELECT * FROM brew_records WHERE overallRating >= :minRating ORDER BY dateTime DESC")
    fun getRecordsByRating(minRating: Int): Flow<List<BrewRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BrewRecord): Long

    @Update
    suspend fun update(record: BrewRecord)

    @Delete
    suspend fun delete(record: BrewRecord)

    @Query("SELECT COUNT(*) FROM brew_records WHERE beanId = :beanId")
    fun getBrewCountForBean(beanId: Long): Flow<Int>

    @Query("SELECT * FROM brew_records WHERE beanId = :beanId ORDER BY overallRating DESC LIMIT 1")
    suspend fun getBestRecordForBean(beanId: Long): BrewRecord?
}
