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

    /** 按水温分段统计（固定三段：88°C以下 / 88-91°C / 92-95°C） */
    @Query("""
        SELECT
            CASE
                WHEN waterTemp <= 88 THEN 0
                WHEN waterTemp <= 91 THEN 1
                ELSE 2
            END as bucket,
            COUNT(*) as cnt
        FROM brew_records WHERE waterTemp > 0
        GROUP BY bucket ORDER BY bucket
    """)
    fun getBrewCountsByTemp(): Flow<List<TempBucket>>

    /** 按评分分组统计 */
    @Query("SELECT overallRating, COUNT(*) as cnt FROM brew_records WHERE overallRating > 0 GROUP BY overallRating ORDER BY overallRating")
    fun getBrewCountsByRating(): Flow<List<RatingCount>>

    /** 某豆子的按评分分组统计 */
    @Query("SELECT overallRating, COUNT(*) as cnt FROM brew_records WHERE beanId = :beanId AND overallRating > 0 GROUP BY overallRating ORDER BY overallRating")
    fun getBrewCountsByRatingForBean(beanId: Long): Flow<List<RatingCount>>

    /** 某器具的冲煮次数 */
    @Query("SELECT equipment, COUNT(*) as cnt FROM brew_records WHERE beanId = :beanId AND equipment != '' GROUP BY equipment ORDER BY cnt DESC")
    fun getBrewCountsByEquipmentForBean(beanId: Long): Flow<List<EquipmentCount>>

    /** 某豆子的粉水比分布 */
    @Query("""
        SELECT coffeeWaterRatio, COUNT(*) as cnt FROM brew_records
        WHERE beanId = :beanId AND coffeeWaterRatio > 0
        GROUP BY CAST(coffeeWaterRatio AS INTEGER)
        ORDER BY CAST(coffeeWaterRatio AS INTEGER)
    """)
    fun getBrewCountsByRatioForBean(beanId: Long): Flow<List<RatioCount>>

    /** 某豆子的水温分布 */
    @Query("""
        SELECT
            CASE
                WHEN waterTemp <= 88 THEN 0
                WHEN waterTemp <= 91 THEN 1
                ELSE 2
            END as bucket,
            COUNT(*) as cnt
        FROM brew_records WHERE beanId = :beanId AND waterTemp > 0
        GROUP BY bucket ORDER BY bucket
    """)
    fun getBrewCountsByTempForBean(beanId: Long): Flow<List<TempBucket>>

    /** 某豆子的冲煮时段 */
    @Query("""
        SELECT
            CASE
                WHEN CAST(strftime('%H', dateTime / 1000, 'unixepoch', '+8 hours') AS INTEGER) < 6 THEN '深夜'
                WHEN CAST(strftime('%H', dateTime / 1000, 'unixepoch', '+8 hours') AS INTEGER) < 9 THEN '早晨'
                WHEN CAST(strftime('%H', dateTime / 1000, 'unixepoch', '+8 hours') AS INTEGER) < 12 THEN '上午'
                WHEN CAST(strftime('%H', dateTime / 1000, 'unixepoch', '+8 hours') AS INTEGER) < 18 THEN '下午'
                WHEN CAST(strftime('%H', dateTime / 1000, 'unixepoch', '+8 hours') AS INTEGER) < 22 THEN '晚上'
                ELSE '深夜'
            END as slot,
            COUNT(*) as cnt
        FROM brew_records WHERE beanId = :beanId
        GROUP BY slot
        ORDER BY
            CASE slot
                WHEN '早晨' THEN 1
                WHEN '上午' THEN 2
                WHEN '下午' THEN 3
                WHEN '晚上' THEN 4
                WHEN '深夜' THEN 5
            END
    """)
    fun getBrewCountsByTimeSlotForBean(beanId: Long): Flow<List<TimeSlotCount>>

    /** 某豆子的平均总评分 */
    @Query("SELECT AVG(overallRating * 1.0) FROM brew_records WHERE beanId = :beanId AND overallRating > 0")
    fun getAvgRatingForBean(beanId: Long): Flow<Double?>

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

    /** 按冲煮时段分组统计 */
    @Query("""
        SELECT
            CASE
                WHEN CAST(strftime('%H', dateTime / 1000, 'unixepoch', '+8 hours') AS INTEGER) < 6 THEN '深夜'
                WHEN CAST(strftime('%H', dateTime / 1000, 'unixepoch', '+8 hours') AS INTEGER) < 9 THEN '早晨'
                WHEN CAST(strftime('%H', dateTime / 1000, 'unixepoch', '+8 hours') AS INTEGER) < 12 THEN '上午'
                WHEN CAST(strftime('%H', dateTime / 1000, 'unixepoch', '+8 hours') AS INTEGER) < 18 THEN '下午'
                WHEN CAST(strftime('%H', dateTime / 1000, 'unixepoch', '+8 hours') AS INTEGER) < 22 THEN '晚上'
                ELSE '深夜'
            END as slot,
            COUNT(*) as cnt
        FROM brew_records
        GROUP BY slot
        ORDER BY
            CASE slot
                WHEN '早晨' THEN 1
                WHEN '上午' THEN 2
                WHEN '下午' THEN 3
                WHEN '晚上' THEN 4
                WHEN '深夜' THEN 5
            END
    """)
    fun getBrewCountsByTimeSlot(): Flow<List<TimeSlotCount>>
}

data class TimeSlotCount(val slot: String, val cnt: Int)

data class EquipmentCount(val equipment: String, val cnt: Int)
data class RatioCount(val coffeeWaterRatio: Double, val cnt: Int)
data class TempBucket(val bucket: Int, val cnt: Int)
data class RatingCount(val overallRating: Int, val cnt: Int)
data class EquipmentRating(val equipment: String, val avgRating: Double)
data class OriginCount(val origin: String, val cnt: Int)
data class RoastLevelCount(val roastLevel: String, val cnt: Int)
