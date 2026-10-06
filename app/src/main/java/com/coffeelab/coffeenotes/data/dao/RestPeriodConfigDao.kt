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

    /**
     * 折叠历史重复行：一个 roastDegreeId 只保留 id 最大那条（即用户最后设置的值），
     * 修updateRestDays 曾把每次编辑都插成新行的问题。见 RoastDegreeConfigViewModel。
     */
    @Query("DELETE FROM rest_period_configs WHERE id NOT IN (SELECT MAX(id) FROM rest_period_configs GROUP BY roastDegreeId)")
    suspend fun keepLatestPerRoastDegree()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: RestPeriodConfig): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<RestPeriodConfig>)

    @Update
    suspend fun update(config: RestPeriodConfig)

    @Delete
    suspend fun delete(config: RestPeriodConfig)

    @Query("DELETE FROM rest_period_configs")
    suspend fun deleteAll()
}
