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

    @Query("DELETE FROM brew_records")
    suspend fun deleteAll()

    // ===== Statistical Queries =====

    /** 按月分组统计冲煮次数（近12个月） */
    @Query("""
        SELECT COUNT(*) FROM brew_records
        WHERE dateTime >= :startTime
        GROUP BY strftime('%Y-%m', dateTime / 1000, 'unixepoch')
        ORDER BY strftime('%Y-%m', dateTime / 1000, 'unixepoch')
    """)
    fun getMonthlyBrewCounts(startTime: Long): Flow<List<Int>>

    /** 按器具分组统计冲煮次数 */
    @Query("SELECT equipment, COUNT(*) as cnt FROM brew_records GROUP BY equipment ORDER BY cnt DESC")
    fun getBrewCountsByEquipment(): Flow<List<EquipmentCount>>

    /** 按粉水比分段统计 */
    @Query("""
        SELECT coffeeWaterRatio, COUNT(*) as cnt FROM brew_records
        WHERE coffeeWaterRatio > 0
        GROUP BY CAST(coffeeWaterRatio AS INTEGER)
        ORDER BY CAST(coffeeWaterRatio AS INTEGER)
    """)
    fun getBrewCountsByRatio(): Flow<List<RatioCount>>

    /** 按水温分段统计（每5度一个区间） */
    @Query("""
        SELECT CAST(waterTemp / 5 AS INTEGER) * 5 as bucket, COUNT(*) as cnt
        FROM brew_records WHERE waterTemp > 0
        GROUP BY bucket ORDER BY bucket
    """)
    fun getBrewCountsByTemp(): Flow<List<TempBucket>>

    /** 按评分分组统计 */
    @Query("SELECT overallRating, COUNT(*) as cnt FROM brew_records WHERE overallRating > 0 GROUP BY overallRating ORDER BY overallRating")
    fun getBrewCountsByRating(): Flow<List<RatingCount>>

    /** 某豆子的月冲煮趋势 */
    @Query("""
        SELECT COUNT(*) FROM brew_records
        WHERE beanId = :beanId AND dateTime >= :startTime
        GROUP BY strftime('%Y-%m', dateTime / 1000, 'unixepoch')
        ORDER BY strftime('%Y-%m', dateTime / 1000, 'unixepoch')
    """)
    fun getMonthlyBrewCountsForBean(beanId: Long, startTime: Long): Flow<List<Int>>

    /** 器具平均评分 */
    @Query("SELECT equipment, AVG(overallRating * 1.0) as avgRating FROM brew_records WHERE overallRating > 0 GROUP BY equipment ORDER BY avgRating DESC")
    fun getAvgRatingByEquipment(): Flow<List<EquipmentRating>>

    /** 本周冲煮次数 */
    @Query("SELECT COUNT(*) FROM brew_records WHERE dateTime >= :weekStart")
    fun getBrewCountThisWeek(weekStart: Long): Flow<Int>

    /** 上周冲煮次数 */
    @Query("SELECT COUNT(*) FROM brew_records WHERE dateTime >= :lastWeekStart AND dateTime < :weekStart")
    fun getBrewCountLastWeek(lastWeekStart: Long, weekStart: Long): Flow<Int>

    /** 按产地分组统计豆子数量 */
    @Query("SELECT origin, COUNT(*) as cnt FROM coffee_beans WHERE origin != '' GROUP BY origin ORDER BY cnt DESC")
    fun getBeanCountByOrigin(): Flow<List<OriginCount>>

    /** 按烘焙度分组统计 */
    @Query("SELECT roastLevel, COUNT(*) as cnt FROM coffee_beans WHERE roastLevel != '' GROUP BY roastLevel ORDER BY cnt DESC")
    fun getBeanCountByRoastLevel(): Flow<List<RoastLevelCount>>
}

data class EquipmentCount(val equipment: String, val cnt: Int)
data class RatioCount(val coffeeWaterRatio: Double, val cnt: Int)
data class TempBucket(val bucket: Int, val cnt: Int)
data class RatingCount(val overallRating: Int, val cnt: Int)
data class EquipmentRating(val equipment: String, val avgRating: Double)
data class OriginCount(val origin: String, val cnt: Int)
data class RoastLevelCount(val roastLevel: String, val cnt: Int)
