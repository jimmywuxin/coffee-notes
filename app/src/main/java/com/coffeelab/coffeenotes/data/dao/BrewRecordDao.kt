package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.BrewRecordWithNames
import kotlinx.coroutines.flow.Flow

@Dao
interface BrewRecordDao {
    // ===== Basic CRUD =====

    /** Get all records with equipment/grinder/bean names via JOIN */
    @Query("""
        SELECT br.*, e.name AS equipmentName, g.name AS grinderName, cb.name AS beanName, cb.roaster AS beanRoaster
        FROM brew_records br
        LEFT JOIN equipment e ON br.equipmentId = e.id
        LEFT JOIN grinders g ON br.grinderId = g.id
        LEFT JOIN coffee_beans cb ON br.beanId = cb.id
        ORDER BY br.dateTime DESC
    """)
    fun getAllRecords(): Flow<List<BrewRecordWithNames>>

    /** Get single record by id with names */
    @Query("""
        SELECT br.*, e.name AS equipmentName, g.name AS grinderName, cb.name AS beanName, cb.roaster AS beanRoaster
        FROM brew_records br
        LEFT JOIN equipment e ON br.equipmentId = e.id
        LEFT JOIN grinders g ON br.grinderId = g.id
        LEFT JOIN coffee_beans cb ON br.beanId = cb.id
        WHERE br.id = :id
    """)
    suspend fun getRecordById(id: Long): BrewRecordWithNames?

    /** Get records for a bean with names */
    @Query("""
        SELECT br.*, e.name AS equipmentName, g.name AS grinderName, cb.name AS beanName, cb.roaster AS beanRoaster
        FROM brew_records br
        LEFT JOIN equipment e ON br.equipmentId = e.id
        LEFT JOIN grinders g ON br.grinderId = g.id
        LEFT JOIN coffee_beans cb ON br.beanId = cb.id
        WHERE br.beanId = :beanId
        ORDER BY br.dateTime DESC
    """)
    fun getRecordsForBean(beanId: Long): Flow<List<BrewRecordWithNames>>

    /** Get records by equipment ID with names */
    @Query("""
        SELECT br.*, e.name AS equipmentName, g.name AS grinderName, cb.name AS beanName, cb.roaster AS beanRoaster
        FROM brew_records br
        LEFT JOIN equipment e ON br.equipmentId = e.id
        LEFT JOIN grinders g ON br.grinderId = g.id
        LEFT JOIN coffee_beans cb ON br.beanId = cb.id
        WHERE br.equipmentId = :equipmentId
        ORDER BY br.dateTime DESC
    """)
    fun getRecordsByEquipmentId(equipmentId: Long): Flow<List<BrewRecordWithNames>>

    /** Get records by grinder ID with names */
    @Query("""
        SELECT br.*, e.name AS equipmentName, g.name AS grinderName, cb.name AS beanName, cb.roaster AS beanRoaster
        FROM brew_records br
        LEFT JOIN equipment e ON br.equipmentId = e.id
        LEFT JOIN grinders g ON br.grinderId = g.id
        LEFT JOIN coffee_beans cb ON br.beanId = cb.id
        WHERE br.grinderId = :grinderId
        ORDER BY br.dateTime DESC
    """)
    fun getRecordsByGrinderId(grinderId: Long): Flow<List<BrewRecordWithNames>>

    /** Get records by minimum rating with names */
    @Query("""
        SELECT br.*, e.name AS equipmentName, g.name AS grinderName, cb.name AS beanName, cb.roaster AS beanRoaster
        FROM brew_records br
        LEFT JOIN equipment e ON br.equipmentId = e.id
        LEFT JOIN grinders g ON br.grinderId = g.id
        LEFT JOIN coffee_beans cb ON br.beanId = cb.id
        WHERE br.overallRating >= :minRating
        ORDER BY br.dateTime DESC
    """)
    fun getRecordsByRating(minRating: Int): Flow<List<BrewRecordWithNames>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BrewRecord): Long

    @Update
    suspend fun update(record: BrewRecord)

    @Delete
    suspend fun delete(record: BrewRecord)

    @Query("SELECT COUNT(*) FROM brew_records WHERE beanId = :beanId")
    fun getBrewCountForBean(beanId: Long): Flow<Int>

    /** Get best record for a bean with names */
    @Query("""
        SELECT br.*, e.name AS equipmentName, g.name AS grinderName, cb.name AS beanName, cb.roaster AS beanRoaster
        FROM brew_records br
        LEFT JOIN equipment e ON br.equipmentId = e.id
        LEFT JOIN grinders g ON br.grinderId = g.id
        LEFT JOIN coffee_beans cb ON br.beanId = cb.id
        WHERE br.beanId = :beanId
        ORDER BY br.overallRating DESC LIMIT 1
    """)
    suspend fun getBestRecordForBean(beanId: Long): BrewRecordWithNames?

    @Query("DELETE FROM brew_records")
    suspend fun deleteAll()

    // ===== Search =====

    /** Full-text search across records + joined bean/equipment/grinder/method names */
    @Query("""
        SELECT br.*, e.name AS equipmentName, g.name AS grinderName, cb.name AS beanName, cb.roaster AS beanRoaster
        FROM brew_records br
        LEFT JOIN equipment e ON br.equipmentId = e.id
        LEFT JOIN grinders g ON br.grinderId = g.id
        LEFT JOIN coffee_beans cb ON br.beanId = cb.id
        LEFT JOIN brew_methods bm ON br.methodId = bm.id
        WHERE cb.name LIKE '%' || :query || '%'
           OR cb.roaster LIKE '%' || :query || '%'
           OR br.grindSize LIKE '%' || :query || '%'
           OR br.flavorNotes LIKE '%' || :query || '%'
           OR e.name LIKE '%' || :query || '%'
           OR g.name LIKE '%' || :query || '%'
           OR bm.name LIKE '%' || :query || '%'
        ORDER BY br.dateTime DESC
    """)
    fun searchRecords(query: String): Flow<List<BrewRecordWithNames>>

    // ===== Statistical Queries =====

    @Query("""
        SELECT COUNT(*) FROM brew_records
        WHERE dateTime >= :startTime
        GROUP BY strftime('%Y-%m', dateTime / 1000, 'unixepoch')
        ORDER BY strftime('%Y-%m', dateTime / 1000, 'unixepoch')
    """)
    fun getMonthlyBrewCounts(startTime: Long): Flow<List<Int>>

    @Query("""
        SELECT e.name AS equipmentName, COUNT(*) as cnt
        FROM brew_records br
        JOIN equipment e ON br.equipmentId = e.id
        GROUP BY br.equipmentId
        ORDER BY cnt DESC
    """)
    fun getBrewCountsByEquipment(): Flow<List<EquipmentCount>>

    @Query("""
        SELECT coffeeWaterRatio, COUNT(*) as cnt FROM brew_records
        WHERE coffeeWaterRatio > 0
        GROUP BY CAST(coffeeWaterRatio AS INTEGER)
        ORDER BY CAST(coffeeWaterRatio AS INTEGER)
    """)
    fun getBrewCountsByRatio(): Flow<List<RatioCount>>

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

    @Query("""
        SELECT overallRating, COUNT(*) as cnt FROM brew_records
        WHERE overallRating > 0
        GROUP BY overallRating ORDER BY overallRating
    """)
    fun getBrewCountsByRating(): Flow<List<RatingCount>>

    // ===== Bean-Specific Stats =====

    @Query("""
        SELECT coffeeWaterRatio, COUNT(*) as cnt FROM brew_records
        WHERE beanId = :beanId AND coffeeWaterRatio > 0
        GROUP BY CAST(coffeeWaterRatio AS INTEGER)
        ORDER BY CAST(coffeeWaterRatio AS INTEGER)
    """)
    fun getBrewCountsByRatioForBean(beanId: Long): Flow<List<RatioCount>>

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

    @Query("""
        SELECT e.name AS equipmentName, COUNT(*) as cnt
        FROM brew_records br
        JOIN equipment e ON br.equipmentId = e.id
        WHERE br.beanId = :beanId
        GROUP BY br.equipmentId
        ORDER BY cnt DESC
    """)
    fun getBrewCountsByEquipmentForBean(beanId: Long): Flow<List<EquipmentCount>>

    @Query("""
        SELECT overallRating, COUNT(*) as cnt FROM brew_records
        WHERE beanId = :beanId AND overallRating > 0
        GROUP BY overallRating ORDER BY overallRating
    """)
    fun getBrewCountsByRatingForBean(beanId: Long): Flow<List<RatingCount>>

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

    @Query("SELECT AVG(overallRating * 1.0) FROM brew_records WHERE beanId = :beanId AND overallRating > 0")
    fun getAvgRatingForBean(beanId: Long): Flow<Double?>

    @Query("""
        SELECT COUNT(*) FROM brew_records
        WHERE beanId = :beanId AND dateTime >= :startTime
        GROUP BY strftime('%Y-%m', dateTime / 1000, 'unixepoch')
        ORDER BY strftime('%Y-%m', dateTime / 1000, 'unixepoch')
    """)
    fun getMonthlyBrewCountsForBean(beanId: Long, startTime: Long): Flow<List<Int>>

    // ===== Cross-Entity Stats =====

    @Query("""
        SELECT e.name AS equipmentName, AVG(br.overallRating * 1.0) as avgRating
        FROM brew_records br
        JOIN equipment e ON br.equipmentId = e.id
        WHERE br.overallRating > 0
        GROUP BY br.equipmentId
        ORDER BY avgRating DESC
    """)
    fun getAvgRatingByEquipment(): Flow<List<EquipmentRating>>

    @Query("SELECT COUNT(*) FROM brew_records WHERE dateTime >= :weekStart")
    fun getBrewCountThisWeek(weekStart: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM brew_records WHERE dateTime >= :lastWeekStart AND dateTime < :weekStart")
    fun getBrewCountLastWeek(lastWeekStart: Long, weekStart: Long): Flow<Int>

    @Query("SELECT origin, COUNT(*) as cnt FROM coffee_beans WHERE origin != '' GROUP BY origin ORDER BY cnt DESC")
    fun getBeanCountByOrigin(): Flow<List<OriginCount>>

    @Query("SELECT roastLevel, COUNT(*) as cnt FROM coffee_beans WHERE roastLevel != '' GROUP BY roastLevel ORDER BY cnt DESC")
    fun getBeanCountByRoastLevel(): Flow<List<RoastLevelCount>>

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
data class EquipmentCount(val equipmentName: String, val cnt: Int)
data class RatioCount(val coffeeWaterRatio: Double, val cnt: Int)
data class TempBucket(val bucket: Int, val cnt: Int)
data class RatingCount(val overallRating: Int, val cnt: Int)
data class EquipmentRating(val equipmentName: String, val avgRating: Double)
data class OriginCount(val origin: String, val cnt: Int)
data class RoastLevelCount(val roastLevel: String, val cnt: Int)
