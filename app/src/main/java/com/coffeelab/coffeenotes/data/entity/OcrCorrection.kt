package com.coffeelab.coffeenotes.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * OCR 纠错回流记录。
 *
 * 当用户对 OCR 识别结果做了修改后，保存豆子时把「OCR 原始值 → 用户修正值」写入此表。
 * 后续 OCR 识别时，若某字段识别值命中历史 ocrRaw，则自动替换为 userValue，
 * 实现用得越久越准。
 *
 * 唯一键 (field, ocrRaw)：同一误识模式只记一条，userValue 取最新，hitCount 累加。
 */
@Entity(
    tableName = "ocr_corrections",
    indices = [Index(value = ["field", "ocrRaw"], unique = true)]
)
data class OcrCorrection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 字段名：roaster / name / origin / region / estate / variety / roastDate 等 */
    val field: String,
    /** OCR 原始识别值 */
    val ocrRaw: String,
    /** 用户修正后的值 */
    val userValue: String,
    /** 关联豆子 id（可空） */
    val beanId: Long? = null,
    /** 此纠错被命中/写入的次数，用于排序取最常用 */
    val hitCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
