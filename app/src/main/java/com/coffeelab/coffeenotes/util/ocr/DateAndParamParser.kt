package com.coffeelab.coffeenotes.util.ocr

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * 日期与萃取参数抽取。
 *
 * 全部走纯正则 + 标准库解析，不依赖 Android framework，便于单测。
 */
object DateAndParamParser {

    data class BrewParams(
        val dose: Float? = null,
        val brewRatio: String = "",
        val waterAmount: Float? = null,
        val brewTime: Int? = null,
        val waterTemp: Int? = null
    )

    private const val BBE_LOOKBACK_YEARS = 1L

    // ── 日期模式 ──

    private val ISO_DATE = Regex("""(\d{4})[/.年-](\d{1,2})[/.月-](\d{1,2})""")
    private val DAY_MONTH_YEAR = Regex(
        """(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{4})""",
        RegexOption.IGNORE_CASE
    )
    private val MONTH_DAY_YEAR = Regex(
        """(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{1,2}),?\s+(\d{4})""",
        RegexOption.IGNORE_CASE
    )
    private val LABELED_DATE = Regex(
        """(?:Roasted|烘焙日期|Roast Date|Roast date)[:：\s]+(\d{4}[-/.年]\d{1,2}[-/.月]\d{1,2})""",
        RegexOption.IGNORE_CASE
    )
    private val BBE_DATE = Regex(
        """(?:BBE|Best Before|赏味期|保质期)[:：\s]+(\d{4}[-/.年]\d{1,2}[-/.月]\d{1,2})""",
        RegexOption.IGNORE_CASE
    )

    private val OUTPUT_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.US)

    // ── 参数模式 ──
    // 用 (?!\d|[a-zA-Z]|克|毫) 替代 \b，CJK 字符 \b 失效
    private val DOSE = Regex(
        """(\d+(?:\.\d+)?)\s*(?:g|克)(?!\d|[a-zA-Z]|克|毫)""",
        RegexOption.IGNORE_CASE
    )
    private val BREW_RATIO = Regex("""1\s*[:：]\s*(\d+(?:\.\d+)?)""")
    private val WATER_AMOUNT = Regex(
        """(\d+(?:\.\d+)?)\s*(?:ml|mL|毫升)(?!\d|[a-zA-Z])""",
        RegexOption.IGNORE_CASE
    )
    private val BREW_TIME = Regex(
        """(\d+)\s*(?:s|sec|秒|"|''|”)(?!\d)""",
        RegexOption.IGNORE_CASE
    )
    private val WATER_TEMP = Regex(
        """(\d{2,3})\s*(?:°C|℃|度|degrees?)(?!\d)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * 解析日期和萃取参数。
     *
     * @return `(roastDate, BrewParams)` - 命中则填充，未命中字段为 null/空
     */
    fun parse(rawText: String): Pair<String?, BrewParams> {
        val roastDate = extractRoastDate(rawText)
        val params = extractBrewParams(rawText)
        return roastDate to params
    }

    // ── 日期抽取 ──

    internal fun extractRoastDate(text: String): String? {
        // 优先级：带 "Roasted/烘焙日期" 标签 > BBE 减 1 年 > 通用 ISO / 英文
        // 命中任一优先级即返回，不混入其他候选
        LABELED_DATE.find(text)?.groupValues?.get(1)?.let { dateStr ->
            parseDate(dateStr)?.let { return it.format(OUTPUT_FMT) }
        }
        BBE_DATE.find(text)?.groupValues?.get(1)?.let { dateStr ->
            parseDate(dateStr)?.let { return it.minusYears(BBE_LOOKBACK_YEARS).format(OUTPUT_FMT) }
        }

        val candidates = mutableListOf<LocalDate>()
        for (m in ISO_DATE.findAll(text)) {
            parseDate(m.value)?.let { candidates.add(it) }
        }
        for (m in DAY_MONTH_YEAR.findAll(text)) {
            parseEnglishDate(m.groupValues[1].toInt(), m.groupValues[2], m.groupValues[3].toInt())?.let { candidates.add(it) }
        }
        for (m in MONTH_DAY_YEAR.findAll(text)) {
            parseEnglishDate(m.groupValues[2].toInt(), m.groupValues[1], m.groupValues[3].toInt())?.let { candidates.add(it) }
        }

        if (candidates.isEmpty()) return null
        val mostRecent = candidates.maxByOrNull { it }
        return mostRecent?.format(OUTPUT_FMT)
    }

    private fun parseDate(dateStr: String): LocalDate? {
        val patterns = listOf(
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy年M月d日")
        )
        for (p in patterns) {
            try {
                return LocalDate.parse(dateStr.replace("年", "-").replace("月", "-").replace("日", ""), p)
            } catch (_: DateTimeParseException) { /* try next */ }
        }
        return null
    }

    private fun parseEnglishDate(day: Int, mon: String, year: Int): LocalDate? {
        val month = when (mon.lowercase().take(3)) {
            "jan" -> 1; "feb" -> 2; "mar" -> 3; "apr" -> 4
            "may" -> 5; "jun" -> 6; "jul" -> 7; "aug" -> 8
            "sep" -> 9; "oct" -> 10; "nov" -> 11; "dec" -> 12
            else -> return null
        }
        return try { LocalDate.of(year, month, day) } catch (_: Exception) { null }
    }

    // ── 萃取参数抽取 ──

    internal fun extractBrewParams(text: String): BrewParams {
        var dose: Float? = null
        var brewRatio = ""
        var waterAmount: Float? = null
        var brewTime: Int? = null
        var waterTemp: Int? = null

        DOSE.find(text)?.groupValues?.get(1)?.toFloatOrNull()?.let { dose = it }
        BREW_RATIO.find(text)?.groupValues?.get(1)?.let { brewRatio = "1:$it" }
        WATER_AMOUNT.find(text)?.groupValues?.get(1)?.toFloatOrNull()?.let { waterAmount = it }
        BREW_TIME.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { brewTime = it }
        WATER_TEMP.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { waterTemp = it }

        return BrewParams(dose, brewRatio, waterAmount, brewTime, waterTemp)
    }
}
