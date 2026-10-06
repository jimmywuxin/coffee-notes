package com.coffeelab.coffeenotes.util

import java.util.Locale

/**
 * 数值/文本格式化（原先散在 5 个文件里各写一遍，口径已漂移）。
 *
 * 关键点：**必须显式指定 [Locale]**。无参`String.format("%.1f", 15.0)` 在 de-DE 下
 * 产出 "15,0"，写进 brewRatio 后 `toDoubleOrNull` 解析失败，粉水比静默变空。
 */
object Format {

    /**
     * 数值展示：整数不带小数点，否则保留一位（240.0 → "240"，242.5 → "242.5"）。
     * NaN / Infinity 返回空串——由用户手输的字段可能出现，不该显示 "NaN"。
     */
    fun num(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return ""
        val rounded = Math.round(value * 10.0) / 10.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", rounded)
        }
    }

    /** 固定一位小数的评分展示（0.0 也要显示成 "0.0" 而非 "0"） */
    fun score(value: Double): String =
        if (value.isNaN() || value.isInfinite()) "" else String.format(Locale.US, "%.1f", value)
}
