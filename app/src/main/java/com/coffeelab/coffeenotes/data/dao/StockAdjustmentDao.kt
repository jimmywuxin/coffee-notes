package com.coffeelab.coffeenotes.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.coffeelab.coffeenotes.data.entity.StockAdjustment
import kotlinx.coroutines.flow.Flow

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments WHERE beanId = :beanId ORDER BY createdAt DESC")
    fun getForBean(beanId: Long): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments WHERE beanId = :beanId ORDER BY createdAt DESC")
    suspend fun getForBeanOnce(beanId: Long): List<StockAdjustment>

    @Insert
    suspend fun insert(adjustment: StockAdjustment): Long

    @Delete
    suspend fun delete(adjustment: StockAdjustment)

    @Query("DELETE FROM stock_adjustments WHERE beanId = :beanId")
    suspend fun deleteForBean(beanId: Long)

    @Query("DELETE FROM stock_adjustments")
    suspend fun deleteAll()
}
