package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.PeakFlavorConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface PeakFlavorConfigDao {
    @Query("SELECT * FROM peak_flavor_configs ORDER BY id ASC")
    fun getAll(): Flow<List<PeakFlavorConfig>>

    @Query("SELECT * FROM peak_flavor_configs ORDER BY id ASC")
    suspend fun getAllOnce(): List<PeakFlavorConfig>

    @Query("SELECT * FROM peak_flavor_configs WHERE roastDegreeId = :roastDegreeId")
    suspend fun getByRoastDegreeId(roastDegreeId: Long): PeakFlavorConfig?

    /** 折叠历史重复行，每个 roastDegreeId 只保留 id 最大那条。见 RoastDegreeConfigViewModel。 */
    @Query("DELETE FROM peak_flavor_configs WHERE id NOT IN (SELECT MAX(id) FROM peak_flavor_configs GROUP BY roastDegreeId)")
    suspend fun keepLatestPerRoastDegree()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: PeakFlavorConfig): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<PeakFlavorConfig>)

    @Update
    suspend fun update(config: PeakFlavorConfig)

    @Delete
    suspend fun delete(config: PeakFlavorConfig)

    @Query("DELETE FROM peak_flavor_configs")
    suspend fun deleteAll()
}
