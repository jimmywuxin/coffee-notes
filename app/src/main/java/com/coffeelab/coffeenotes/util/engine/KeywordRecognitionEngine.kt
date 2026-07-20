package com.coffeelab.coffeenotes.util.engine

import android.graphics.Bitmap
import com.coffeelab.coffeenotes.data.entity.OcrCorrection
import com.coffeelab.coffeenotes.util.OCRProcessor

/**
 * 本地关键词识别引擎（离线可用）
 *
 * 使用 ML Kit OCR + 关键词规则匹配，无需联网。
 */
class KeywordRecognitionEngine : RecognitionEngine {

    /** 带 OCR 纠错回流的识别：历史纠错命中时自动替换字段值。 */
    suspend fun recognize(
        bitmap: Bitmap,
        corrections: List<OcrCorrection>
    ): RecognitionResult {
        return try {
            val ocrResult = OCRProcessor.processBitmap(bitmap, corrections)
            RecognitionResult(
                roaster = ocrResult.roaster,
                name = ocrResult.name,
                origin = ocrResult.origin,
                estate = ocrResult.estate,
                variety = ocrResult.variety,
                process = ocrResult.process,
                roastLevel = ocrResult.roastLevel,
                roastDate = ocrResult.roastDate,
                flavors = ocrResult.flavors,
                notes = "",
                dose = ocrResult.dose,
                brewRatio = ocrResult.brewRatio,
                waterAmount = ocrResult.waterAmount,
                brewTime = ocrResult.brewTime,
                waterTemp = ocrResult.waterTemp,
                rawResponse = ocrResult.fullText,
                success = true,
                engineName = "本地关键词",
                lowConfidenceFields = ocrResult.lowConfidenceFields
            )
        } catch (e: Exception) {
            RecognitionResult(
                success = false,
                rawResponse = "离线识别失败: ${e.message}",
                engineName = "本地关键词"
            )
        }
    }

    /** 向后兼容的无纠错版本。 */
    override suspend fun recognize(bitmap: Bitmap): RecognitionResult = recognize(bitmap, emptyList())
}
