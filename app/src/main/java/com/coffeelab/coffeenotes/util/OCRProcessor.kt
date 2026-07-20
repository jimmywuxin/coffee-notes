package com.coffeelab.coffeenotes.util

import android.graphics.Bitmap
import com.coffeelab.coffeenotes.data.entity.OcrCorrection
import com.coffeelab.coffeenotes.util.ocr.DateAndParamParser
import com.coffeelab.coffeenotes.util.ocr.FieldExtractor
import com.coffeelab.coffeenotes.util.ocr.OcrEngine
import com.coffeelab.coffeenotes.util.ocr.TextPreprocessor

data class OCRResult(
    val roaster: String = "",
    val name: String = "",
    val origin: String = "",
    val variety: String = "",
    val process: String = "",
    val roastLevel: String = "",
    val estate: String = "",
    val region: String = "",
    val flavors: List<String> = emptyList(),
    val fullText: String = "",
    /** 字段名 -> 证据强度（0.0-1.0）。字段未抽取则不在 map 中。 */
    val fieldConfidence: Map<String, Float> = emptyMap(),
    /** 证据强度 < [com.coffeelab.coffeenotes.util.ocr.OcrConfidence.LOW_CONFIDENCE_THRESHOLD] 的字段名集合。 */
    val lowConfidenceFields: Set<String> = emptySet(),
    /** 萃取参数（Phase 4.2） */
    val dose: Float? = null,
    val brewRatio: String = "",
    val waterAmount: Float? = null,
    val brewTime: Int? = null,
    val waterTemp: Int? = null,
    /** 烘焙日期（Phase 4.1） */
    val roastDate: String = ""
)

/**
 * OCR 入口 facade。
 *
 * 内部按 Phase 顺序接入：双模型合并（Phase 2）→ 多策略预处理（Phase 3）→
 * 日期/参数抽取（Phase 4）→ 豆名打分（Phase 4.3）→ 历史纠错回流（Phase 5）。
 *
 * 对外签名 `processBitmap(bitmap: Bitmap, corrections)` 保持向后兼容。
 */
object OCRProcessor {

    private val engine = OcrEngine()
    private val preprocessor = TextPreprocessor(engine)

    suspend fun processBitmap(
        bitmap: Bitmap,
        corrections: List<OcrCorrection> = emptyList()
    ): OCRResult {
        return try {
            val lines = preprocessor.process(bitmap)
            val rawText = lines.joinToString("\n") { it.text }
            val extracted = FieldExtractor.extract(lines, rawText)
            val parsed = DateAndParamParser.parse(rawText)
            val date = parsed.first
            val params = parsed.second
            var result = extracted.copy(
                roastDate = date ?: extracted.roastDate,
                dose = params.dose ?: extracted.dose,
                brewRatio = params.brewRatio.ifEmpty { extracted.brewRatio },
                waterAmount = params.waterAmount ?: extracted.waterAmount,
                brewTime = params.brewTime ?: extracted.brewTime,
                waterTemp = params.waterTemp ?: extracted.waterTemp
            )
            if (corrections.isNotEmpty()) {
                result = applyCorrections(result, corrections)
            }
            result
        } catch (e: Exception) {
            OCRResult(fullText = "识别失败: ${e.message}")
        }
    }

    /**
     * 应用历史纠错：若某字段 OCR 识别值（归一化后）命中历史 ocrRaw，替换为用户修正值。
     * 归一化：trim + lowercase + 压缩连续空白，避免大小写/首尾空格差异导致漏匹配。
     */
    private fun applyCorrections(result: OCRResult, corrections: List<OcrCorrection>): OCRResult {
        // field -> (ocrRaw_norm -> userValue)，同一 key 多条取 hitCount 最高
        val index: Map<String, Map<String, String>> = corrections
            .groupBy { it.field }
            .mapValues { (_, list) ->
                list.sortedByDescending { it.hitCount }
                    .associate { normalize(it.ocrRaw) to it.userValue }
            }

        fun correct(field: String, value: String): String {
            if (value.isBlank()) return value
            return index[field]?.get(normalize(value)) ?: value
        }

        return result.copy(
            roaster = correct("roaster", result.roaster),
            name = correct("name", result.name),
            origin = correct("origin", result.origin),
            region = correct("region", result.region),
            estate = correct("estate", result.estate),
            variety = correct("variety", result.variety),
            roastDate = correct("roastDate", result.roastDate)
        )
    }

    private fun normalize(s: String): String =
        s.trim().lowercase().replace(Regex("\\s+"), " ")
}
