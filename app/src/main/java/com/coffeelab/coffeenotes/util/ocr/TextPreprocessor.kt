package com.coffeelab.coffeenotes.util.ocr

import android.graphics.Bitmap
import com.coffeelab.coffeenotes.util.BitmapLoader

/**
 * 多策略图像预处理。
 *
 * 策略列表：
 *  - ORIGINAL          原图直送
 *  - GRAY_CONTRAST     灰度 + 对比度 1.4 + 3×3 锐化（首选 / 降级兜底）
 *  - OTSU_BIN          Otsu 自适应二值化（高对比度包装袋）
 *  - GAMMA_0_8         γ=0.8 提亮（暗光照片）
 *
 * 降级模式：先跑 GRAY_CONTRAST，若行级平均置信度 < [DEGRADE_THRESHOLD]，
 * 再跑其余 3 策略，取平均置信度最高的结果。
 *
 * 最坏情况 4 次 ML Kit 调用（计划指标）；典型 1 次。
 *
 * 倾斜矫正（3.3）与文字密集区裁剪（3.2）暂未接入，留作未来扩展。
 */
class TextPreprocessor(
    private val ocrEngine: OcrEngine
) {

    enum class Strategy(val displayName: String) {
        ORIGINAL("原图"),
        GRAY_CONTRAST("灰度+对比度"),
        OTSU_BIN("Otsu二值化"),
        GAMMA_0_8("Gamma提亮")
    }

    suspend fun process(bitmap: Bitmap): List<OcrLine> {
        val firstLines = ocrEngine.recognize(BitmapLoader.enhanceForOcr(bitmap))
        if (avgConfidence(firstLines) >= DEGRADE_THRESHOLD) {
            return firstLines
        }
        val candidates = mutableListOf<Pair<List<OcrLine>, Strategy>>()
        candidates.add(firstLines to Strategy.GRAY_CONTRAST)
        candidates.add(ocrEngine.recognize(bitmap) to Strategy.ORIGINAL)
        candidates.add(ocrEngine.recognize(BitmapLoader.applyOtsuBinarization(bitmap)) to Strategy.OTSU_BIN)
        candidates.add(ocrEngine.recognize(BitmapLoader.applyGammaCorrection(bitmap, GAMMA_VALUE)) to Strategy.GAMMA_0_8)
        return candidates.maxBy { avgConfidence(it.first) }.first
    }

    private fun avgConfidence(lines: List<OcrLine>): Float {
        if (lines.isEmpty()) return 0f
        return lines.map { it.confidence }.average().toFloat()
    }

    companion object {
        const val DEGRADE_THRESHOLD = 0.75f
        const val GAMMA_VALUE = 0.8f
    }
}
