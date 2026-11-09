package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.PurchaseRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseRecordDao {
    @Query("SELECT * FROM purchase_records WHERE beanId = :beanId ORDER BY date DESC")
    fun getByBeanId(beanId: Long): Flow<List<PurchaseRecord>>

    @Query("SELECT * FROM purchase_records WHERE beanId = :beanId ORDER BY date DESC")
    suspend fun getByBeanIdOnce(beanId: Long): List<PurchaseRecord>

    @Query("SELECT * FROM purchase_records WHERE id = :id")
    suspend fun getById(id: Long): PurchaseRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PurchaseRecord): Long

    @Update
    suspend fun update(record: PurchaseRecord)

    @Delete
    suspend fun delete(record: PurchaseRecord)

    @Query("DELETE FROM purchase_records WHERE beanId = :beanId")
    suspend fun deleteByBeanId(beanId: Long)

    @Query("SELECT * FROM purchase_records ORDER BY date DESC")
    suspend fun getAllOnce(): List<PurchaseRecord>

    // 每个豆子 date 最大的购买记录（相关子查询、无 GROUP BY；同 bean 同日多条会全部返回，由 Repository 侧 .associate 取末条）
    @Query("""
        SELECT * FROM purchase_records pr
        WHERE pr.date = (
            SELECT MAX(date) FROM purchase_records WHERE beanId = pr.beanId
        )
    """)
    suspend fun getLatestForAllBeansOnce(): List<PurchaseRecord>

    @Query("DELETE FROM purchase_records")
    suspend fun deleteAll()
}
