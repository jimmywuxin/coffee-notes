package com.coffeelab.coffeenotes.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.coffeelab.coffeenotes.data.entity.OcrCorrection

@Dao
interface OcrCorrectionDao {
    @Query("SELECT * FROM ocr_corrections")
    suspend fun getAllOnce(): List<OcrCorrection>

    @Query("SELECT * FROM ocr_corrections WHERE field = :field")
    suspend fun getByFieldOnce(field: String): List<OcrCorrection>

    /**
     * Upsert：同一 (field, ocrRaw) 存在则更新 userValue 并 hitCount+1，否则新增。
     * 利用 (field, ocrRaw) 唯一索引做 ON CONFLICT。
     */
    @Query("""
        INSERT INTO ocr_corrections (field, ocrRaw, userValue, beanId, hitCount, createdAt, updatedAt)
        VALUES (:field, :ocrRaw, :userValue, :beanId, 1, :now, :now)
        ON CONFLICT(field, ocrRaw) DO UPDATE SET
            userValue = :userValue,
            beanId = :beanId,
            hitCount = hitCount + 1,
            updatedAt = :now
    """)
    suspend fun upsert(field: String, ocrRaw: String, userValue: String, beanId: Long?, now: Long)

    @Query("DELETE FROM ocr_corrections")
    suspend fun deleteAll()
}
