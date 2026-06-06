package com.coffeelab.coffeenotes.util.ocr

import android.graphics.Rect

/**
 * 字段证据强度类型与常量。
 *
 * 不同抽取策略对应不同置信度，最后取最小值代表字段的整体可信度。
 * 阈值 [LOW_CONFIDENCE_THRESHOLD] 以下会被记入 `lowConfidenceFields`。
 */
object OcrConfidence {

    /** 命中"标签: 值"同行（冒号 / 空格分隔） */
    const val HORIZONTAL_LABEL_VALUE = 1.0f

    /** 命中垂直"标签 / 值" */
    const val VERTICAL_LABEL_VALUE = 0.9f

    /** 命中词典子串精确匹配（indexOf） */
    const val DICT_EXACT_CONTAINS = 0.8f

    /** Levenshtein 相似度 > 0.85 */
    const val DICT_FUZZY_HIGH = 0.7f

    /** Levenshtein 相似度 0.7-0.85 */
    const val DICT_FUZZY_MID = 0.5f

    /** 兜底无证据（行级猜测） */
    const val FALLBACK = 0.3f

    /** 低于此值的字段会被标记为"低置信" */
    const val LOW_CONFIDENCE_THRESHOLD = 0.6f

    /** ML Kit 未提供 confidence 时的默认值 */
    const val DEFAULT_LINE_CONFIDENCE = 0.75f

    /** 丢弃字符级低置信元素的阈值 */
    const val MIN_ELEMENT_CONFIDENCE = 0.5f
}

/**
 * 纯 Kotlin 实现的行级包围盒。避免在 JVM 单测中触碰 android.graphics.Rect 的 stub 方法。
 */
data class BoundingBox(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerY: Float get() = (top + bottom) / 2f

    companion object {
        fun from(rect: Rect): BoundingBox = BoundingBox(rect.left, rect.top, rect.right, rect.bottom)
    }
}

/**
 * 经双模型合并 + 字符级置信度过滤后的一行文本。
 *
 * @param text 文本内容
 * @param bbox 行级包围盒（纯 Kotlin，不依赖 android.graphics.Rect）
 * @param confidence 行级置信度（已取 zh / la 的均值；任一未提供时用 [OcrConfidence.DEFAULT_LINE_CONFIDENCE]）
 */
data class OcrLine(
    val text: String,
    val bbox: BoundingBox,
    val confidence: Float
) {
    fun centerY(): Float = bbox.centerY
    fun height(): Int = bbox.height
}
