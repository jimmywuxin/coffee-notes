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
}
