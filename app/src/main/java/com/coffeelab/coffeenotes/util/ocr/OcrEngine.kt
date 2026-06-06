package com.coffeelab.coffeenotes.util.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * 双模型 OCR 引擎。
 *
 * - 中文模型 + 拉丁文模型并行跑同一张图
 * - 行级 bbox IoU > 0.5 视为同一物理行 → 合并文本（取较长者）、置信度取平均
 * - 元素级 confidence < [OcrConfidence.MIN_ELEMENT_CONFIDENCE] 的字符被丢弃
 * - 输出 `List<OcrLine>`，供 FieldExtractor 使用
 */
class OcrEngine {

    private val chineseRecognizer: TextRecognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )
    private val latinRecognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.Builder().build()
    )

    /**
     * 双模型识别，返回合并后的行列表。
     * 任一模型失败时不影响另一模型；两者都失败返回空列表。
     */
    suspend fun recognize(bitmap: Bitmap): List<OcrLine> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val zhLines = runCatching { chineseRecognizer.process(image).await() }
            .map { extractLines(it) }
            .getOrDefault(emptyList())
        val laLines = runCatching { latinRecognizer.process(image).await() }
            .map { extractLines(it) }
            .getOrDefault(emptyList())
        return mergeLines(zhLines, laLines)
    }

    /**
     * 关闭底层模型，释放资源。进程退出前调用。
     */
    fun close() {
        runCatching { chineseRecognizer.close() }
        runCatching { latinRecognizer.close() }
    }

    private fun extractLines(text: Text): List<OcrLine> {
        val out = mutableListOf<OcrLine>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val lineBbox = BoundingBox.from(line.boundingBox ?: Rect())
                val mergedText = StringBuilder()
                var sumConf = 0.0
                var confCount = 0
                for (element in line.elements) {
                    val c = element.confidence
                    // ML Kit 在未提供 confidence 的模型上返回 0.0；仅当 > 0 才参与均值与过滤
                    if (c > 0f && c < OcrConfidence.MIN_ELEMENT_CONFIDENCE) continue
                    if (mergedText.isNotEmpty()) mergedText.append(' ')
                    mergedText.append(element.text)
                    if (c > 0f) { sumConf += c; confCount++ }
                }
                val cleanText = mergedText.toString().trim()
                if (cleanText.isEmpty()) continue
                val conf = if (confCount > 0)
                    (sumConf / confCount).toFloat()
                else
                    OcrConfidence.DEFAULT_LINE_CONFIDENCE
                out.add(OcrLine(cleanText, lineBbox, conf))
            }
        }
        return out
    }

    private fun mergeLines(zh: List<OcrLine>, la: List<OcrLine>): List<OcrLine> {
        if (zh.isEmpty()) return la
        if (la.isEmpty()) return zh

        val consumed = BooleanArray(la.size)
        val out = mutableListOf<OcrLine>()

        for (z in zh) {
            var merged = z
            for (i in la.indices) {
                if (consumed[i]) continue
                val l = la[i]
                if (iou(merged.bbox, l.bbox) > MERGE_IOU_THRESHOLD) {
                    val (text, conf) = mergePair(merged, l)
                    merged = OcrLine(text, merged.bbox, conf)
                    consumed[i] = true
                }
            }
            out.add(merged)
        }
        for (i in la.indices) {
            if (!consumed[i]) out.add(la[i])
        }
        return out
    }

    private fun mergePair(a: OcrLine, b: OcrLine): Pair<String, Float> {
        val text = if (b.text.length > a.text.length) b.text else a.text
        val conf = (a.confidence + b.confidence) / 2f
        return text to conf
    }

    private fun iou(a: BoundingBox, b: BoundingBox): Float {
        val interLeft = maxOf(a.left, b.left)
        val interTop = maxOf(a.top, b.top)
        val interRight = minOf(a.right, b.right)
        val interBottom = minOf(a.bottom, b.bottom)
        val interW = (interRight - interLeft).coerceAtLeast(0)
        val interH = (interBottom - interTop).coerceAtLeast(0)
        val inter = interW * interH
        if (inter == 0) return 0f
        val union = a.width * a.height + b.width * b.height - inter
        return if (union <= 0) 0f else inter.toFloat() / union
    }

    companion object {
        private const val MERGE_IOU_THRESHOLD = 0.5f
    }
}
