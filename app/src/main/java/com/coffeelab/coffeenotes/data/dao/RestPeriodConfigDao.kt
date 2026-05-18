package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.RestPeriodConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface RestPeriodConfigDao {
    @Query("SELECT * FROM rest_period_configs ORDER BY id ASC")
    fun getAll(): Flow<List<RestPeriodConfig>>

    @Query("SELECT * FROM rest_period_configs ORDER BY id ASC")
    suspend fun getAllOnce(): List<RestPeriodConfig>

    @Query("SELECT * FROM rest_period_configs WHERE roastDegreeId = :roastDegreeId")
    suspend fun getByRoastDegreeId(roastDegreeId: Long): RestPeriodConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: RestPeriodConfig): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<RestPeriodConfig>)

    @Update
    suspend fun update(config: RestPeriodConfig)

    @Delete
    suspend fun delete(config: RestPeriodConfig)
}
