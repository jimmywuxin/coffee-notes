package com.coffeelab.coffeenotes.util.engine

import android.graphics.Bitmap
import com.coffeelab.coffeenotes.util.OCRProcessor

/**
 * 本地关键词识别引擎（离线可用）
 * 
 * 使用 ML Kit OCR + 关键词规则匹配，无需联网。
 */
class KeywordRecognitionEngine : RecognitionEngine {

    override suspend fun recognize(bitmap: Bitmap): RecognitionResult {
        return try {
            val ocrResult = OCRProcessor.processBitmap(bitmap)
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
}
