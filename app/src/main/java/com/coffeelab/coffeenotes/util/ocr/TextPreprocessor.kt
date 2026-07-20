package com.coffeelab.coffeenotes.util.ocr

import android.graphics.Bitmap
import com.coffeelab.coffeenotes.util.BitmapLoader
import kotlin.math.abs

/**
 * 多策略图像预处理。
 *
 * 策略列表：
 *  - ORIGINAL          原图直送
 *  - GRAY_CONTRAST     灰度 + 对比度 1.4 + 3×3 锐化（首选 / 降级兜底）
 *  - OTSU_BIN          Otsu 自适应二值化（高对比度包装袋）
 *  - GAMMA_0_8         γ=0.8 提亮（暗光照片）
 *
 * 倾斜矫正：首轮识别后取 ML Kit 行级平均角度，绝对值 > [SKEW_THRESHOLD_DEG] 时
 * 反向旋转原图重跑，取置信度更高的结果。
 *
 * 降级模式：若（矫正后）行级平均置信度 < [DEGRADE_THRESHOLD]，
 * 再跑其余 3 策略，取平均置信度最高的结果。
 *
 * 文字密集区裁剪暂未接入，留作未来扩展。
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
        val (firstLines, avgAngle) = ocrEngine.recognizeWithAngle(BitmapLoader.enhanceForOcr(bitmap))

        // 倾斜矫正：平均角度超阈值时，反向旋转原图重跑，取置信度更高者
        var bestLines = firstLines
        if (abs(avgAngle) > SKEW_THRESHOLD_DEG) {
            val rotated = BitmapLoader.rotateBitmap(bitmap, -avgAngle)
            val rotatedEnhanced = BitmapLoader.enhanceForOcr(rotated)
            val rotatedLines = ocrEngine.recognize(rotatedEnhanced)
            if (avgConfidence(rotatedLines) > avgConfidence(bestLines)) {
                bestLines = rotatedLines
            }
        }

        if (avgConfidence(bestLines) >= DEGRADE_THRESHOLD) {
            return bestLines
        }
        val candidates = mutableListOf<Pair<List<OcrLine>, Strategy>>()
        candidates.add(bestLines to Strategy.GRAY_CONTRAST)
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
        /** 倾斜角度阈值（度），超过此值触发旋转矫正 */
        const val SKEW_THRESHOLD_DEG = 2f
    }
}
