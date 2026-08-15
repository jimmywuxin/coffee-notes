package com.coffeelab.coffeenotes.util

import com.coffeelab.coffeenotes.data.dao.OcrCorrectionDao

/**
 * OCR 纠错回流记录（纯逻辑，可独立测试）。
 *
 * 保存豆子时把「OCR 识别值 vs 用户手改值」的差异记入 ocr_corrections，
 * 供后续 OCR 识别做后处理替换（引擎从数据库拉历史纠错）。
 */
object OcrCorrectionRecorder {

    /** 归一化比较：去首尾空白、小写、压缩连续空白 */
    fun normalizeForCompare(s: String): String =
        s.trim().lowercase().replace(Regex("\\s+"), " ")

    /**
     * 逐字段记录纠错：
     * - ocrRaw 空白 / finalValues 缺字段 / 两者归一化后相等（用户没改）→ 跳过
     * - 单条失败不影响其它字段（runCatching）
     */
    suspend fun record(
        dao: OcrCorrectionDao,
        beanId: Long,
        ocrSnapshot: Map<String, String>,
        finalValues: Map<String, String>,
        now: Long = System.currentTimeMillis()
    ) {
        if (ocrSnapshot.isEmpty()) return
        ocrSnapshot.forEach { (field, ocrRaw) ->
            if (ocrRaw.isBlank()) return@forEach
            val userValue = finalValues[field] ?: return@forEach
            if (userValue.isBlank()) return@forEach
            if (normalizeForCompare(ocrRaw) == normalizeForCompare(userValue)) return@forEach
            runCatching { dao.upsert(field, ocrRaw, userValue, beanId, now) }
        }
    }
}
