package com.coffeelab.coffeenotes.util.ocr

import android.graphics.Rect
import com.coffeelab.coffeenotes.util.OCRResult
import com.coffeelab.coffeenotes.util.ocr.dictionary.CoffeeDictionary
import com.coffeelab.coffeenotes.util.ocr.OcrConfidence

/**
 * 字段抽取器：从双模型合并后的 `List<OcrLine>` 中按同行/垂直/词典模糊三策略
 * 解析出咖啡豆信息，同时记录每字段的证据强度。
 *
 * 证据强度（[OcrConfidence]）按策略递减：
 *  - 同行 "标签: 值" → [OcrConfidence.HORIZONTAL_LABEL_VALUE]
 *  - 垂直 "标签 / 值" → [OcrConfidence.VERTICAL_LABEL_VALUE]
 *  - 词典子串精确 → [OcrConfidence.DICT_EXACT_CONTAINS]
 *  - 模糊相似度 > 0.85 → [OcrConfidence.DICT_FUZZY_HIGH]
 *  - 模糊相似度 0.7~0.85 → [OcrConfidence.DICT_FUZZY_MID]
 *  - 兜底行级猜测 → [OcrConfidence.FALLBACK]
 *
 * 低于 [OcrConfidence.LOW_CONFIDENCE_THRESHOLD] 的字段被记入 `lowConfidenceFields`。
 */
object FieldExtractor {

    fun extract(lines: List<OcrLine>, rawText: String): OCRResult {
        val fieldValues = mutableMapOf<String, String>()
        val fieldConfidences = mutableMapOf<String, Float>()

        fun fill(name: String, value: String, confidence: Float) {
            if (name in fieldValues) return
            if (value.isEmpty()) return
            fieldValues[name] = value
            fieldConfidences[name] = confidence
        }

        fun filled(name: String) = name in fieldValues

        val labelFields = listOf(
            Triple("roaster",    CoffeeDictionary.roasterLabels,    OcrConfidence.HORIZONTAL_LABEL_VALUE),
            Triple("origin",     CoffeeDictionary.originLabels,     OcrConfidence.HORIZONTAL_LABEL_VALUE),
            Triple("region",     CoffeeDictionary.regionLabels,     OcrConfidence.HORIZONTAL_LABEL_VALUE),
            Triple("variety",    CoffeeDictionary.varietyLabels,    OcrConfidence.HORIZONTAL_LABEL_VALUE),
            Triple("process",    CoffeeDictionary.processLabels,    OcrConfidence.HORIZONTAL_LABEL_VALUE),
            Triple("roastLevel", CoffeeDictionary.roastLevelLabels, OcrConfidence.HORIZONTAL_LABEL_VALUE),
            Triple("estate",     CoffeeDictionary.estateLabels,     OcrConfidence.HORIZONTAL_LABEL_VALUE)
        )

        for (line in lines) {
            val lineText = line.text
            if (lineText.isEmpty()) continue

            for ((fieldName, labels, _) in labelFields) {
                if (filled(fieldName)) continue
                for (label in labels) {
                    val value = extractValueAfterLabel(lineText, label)
                    if (value != null) {
                        fill(fieldName, value, OcrConfidence.HORIZONTAL_LABEL_VALUE)
                        break
                    }
                }
            }
        }

        for (i in lines.indices) {
            val currentLine = lines[i]
            val lineText = currentLine.text
            if (lineText.isEmpty()) continue

            for ((fieldName, labels, _) in labelFields) {
                if (filled(fieldName)) continue
                val matchedLabel = labels.find {
                    lineText.equals(it, ignoreCase = true) ||
                            (lineText.startsWith(it, ignoreCase = true) && isLabelBoundary(lineText, it.length))
                }
                if (matchedLabel != null) {
                    val valueLine = findVerticalValueLine(currentLine, lines, i + 1)
                    if (valueLine != null) {
                        fill(fieldName, valueLine.text, OcrConfidence.VERTICAL_LABEL_VALUE)
                    }
                    break
                }
            }
        }

        val lineTexts = lines.map { it.text }

        if (!filled("variety")) {
            val m = DictionaryMatcher.fuzzyMatchWithScore(
                lineTexts, CoffeeDictionary.knownVarieties, 0.7f
            )
            if (m.word.isNotEmpty()) {
                val conf = when {
                    m.score >= 0.999f -> OcrConfidence.DICT_EXACT_CONTAINS
                    m.score > 0.85f -> OcrConfidence.DICT_FUZZY_HIGH
                    else -> OcrConfidence.DICT_FUZZY_MID
                }
                fill("variety", m.word, conf)
            } else {
                for (lineText in lineTexts) {
                    for (vp in CoffeeDictionary.knownVarieties) {
                        val idx = lineText.indexOf(vp, ignoreCase = true)
                        if (idx >= 0) {
                            fill("variety", lineText.substring(idx, idx + vp.length), OcrConfidence.DICT_EXACT_CONTAINS)
                            break
                        }
                    }
                    if (filled("variety")) break
                }
            }
        }

        if (!filled("origin")) {
            for (lineText in lineTexts) {
                var matchedOrigin = ""
                var matchedRegion = ""
                for (o in CoffeeDictionary.commonOrigins) {
                    if (o.length < 2) continue
                    val idx = lineText.indexOf(o, ignoreCase = true)
                    if (idx >= 0) {
                        matchedOrigin = o
                        val rest = lineText.replace(o, "", ignoreCase = true).trim()
                        if (rest.isNotEmpty() && !filled("region") &&
                            !rest.contains("烘焙") && !rest.contains("处理") &&
                            !rest.contains("风味") && rest.length < 20
                        ) {
                            matchedRegion = rest
                        }
                        break
                    }
                }
                if (matchedOrigin.isNotEmpty()) {
                    fill("origin", matchedOrigin, OcrConfidence.DICT_EXACT_CONTAINS)
                    if (matchedRegion.isNotEmpty()) {
                        fill("region", matchedRegion, OcrConfidence.DICT_EXACT_CONTAINS)
                    }
                    break
                }
            }
        }

        if (!filled("process")) {
            val m = DictionaryMatcher.fuzzyMatchWithScore(
                lineTexts, CoffeeDictionary.knownProcesses, 0.7f
            )
            if (m.word.isNotEmpty()) {
                val conf = when {
                    m.score >= 0.999f -> OcrConfidence.DICT_EXACT_CONTAINS
                    m.score > 0.85f -> OcrConfidence.DICT_FUZZY_HIGH
                    else -> OcrConfidence.DICT_FUZZY_MID
                }
                fill("process", m.word, conf)
            } else {
                for (lineText in lineTexts) {
                    for (kp in CoffeeDictionary.knownProcesses) {
                        if (lineText.contains(kp, ignoreCase = true)) {
                            fill("process", kp, OcrConfidence.DICT_EXACT_CONTAINS)
                            break
                        }
                    }
                    if (filled("process")) break
                }
            }
        }

        if (!filled("roastLevel")) {
            val m = DictionaryMatcher.fuzzyMatchWithScore(
                lineTexts, CoffeeDictionary.knownRoastLevels, 0.6f
            )
            if (m.word.isNotEmpty()) {
                val conf = when {
                    m.score >= 0.999f -> OcrConfidence.DICT_EXACT_CONTAINS
                    m.score > 0.85f -> OcrConfidence.DICT_FUZZY_HIGH
                    else -> OcrConfidence.DICT_FUZZY_MID
                }
                fill("roastLevel", m.word, conf)
            } else {
                for (lineText in lineTexts) {
                    if (lineText.contains("浅", ignoreCase = true) &&
                        (lineText.contains("烘") || lineText.contains("焙"))) {
                        fill("roastLevel", "浅烘", OcrConfidence.FALLBACK)
                        break
                    }
                    if (lineText.contains("中", ignoreCase = true) &&
                        (lineText.contains("烘") || lineText.contains("焙"))) {
                        fill("roastLevel", "中烘", OcrConfidence.FALLBACK)
                        break
                    }
                    if (lineText.contains("深", ignoreCase = true) &&
                        (lineText.contains("烘") || lineText.contains("焙"))) {
                        fill("roastLevel", "深烘", OcrConfidence.FALLBACK)
                        break
                    }
                }
            }
        }

        if (!filled("name")) {
            val cleaned = scoreNameCandidate(lines, fieldValues.values.toList())
            if (cleaned != null) {
                fill("name", cleaned, OcrConfidence.FALLBACK)
            }
        }

        val excludedTerms = fieldValues.values.filter { it.length >= 2 }.toSet()
        val flavors = mutableListOf<String>()
        FlavorExtractor.extract(rawText, flavors, excludedTerms)

        // 后处理：若 origin 是 "国家 + 产区" 形式（"埃塞俄比亚 耶加雪菲"），
        // 拆分为 origin = "埃塞俄比亚"、region = "耶加雪菲"
        fieldValues["origin"]?.let { originVal ->
            for (o in CoffeeDictionary.commonOrigins) {
                if (o.length < 2) continue
                val idx = originVal.indexOf(o, ignoreCase = true)
                if (idx == 0) {
                    val rest = originVal.substring(o.length).trim()
                    if (rest.isNotEmpty() && !fieldValues.containsKey("region") &&
                        !rest.contains("烘焙") && !rest.contains("处理") &&
                        !rest.contains("风味") && rest.length < 20
                    ) {
                        fieldValues["region"] = rest
                    }
                    // 裁剪 origin 到国别
                    fieldValues["origin"] = o
                    fieldConfidences["origin"] = OcrConfidence.DICT_EXACT_CONTAINS
                    break
                }
            }
        }

        var roastLevel = fieldValues["roastLevel"].orEmpty()
        roastLevel = when {
            roastLevel.contains("浅") -> "浅烘"
            roastLevel.contains("深") -> "深烘"
            roastLevel.contains("中") && !roastLevel.contains("浅") && !roastLevel.contains("深") -> "中烘"
            else -> roastLevel
        }
        if (roastLevel != fieldValues["roastLevel"].orEmpty() && roastLevel.isNotEmpty()) {
            fieldValues["roastLevel"] = roastLevel
        }

        val lowConf = fieldConfidences.filterValues { it < OcrConfidence.LOW_CONFIDENCE_THRESHOLD }.keys

        return OCRResult(
            roaster = fieldValues["roaster"].orEmpty(),
            name = fieldValues["name"].orEmpty(),
            origin = fieldValues["origin"].orEmpty(),
            region = fieldValues["region"].orEmpty(),
            variety = fieldValues["variety"].orEmpty(),
            process = fieldValues["process"].orEmpty(),
            roastLevel = fieldValues["roastLevel"].orEmpty(),
            estate = fieldValues["estate"].orEmpty(),
            flavors = flavors.distinct(),
            fullText = rawText,
            fieldConfidence = fieldConfidences.toMap(),
            lowConfidenceFields = lowConf
        )
    }

    private fun extractValueAfterLabel(lineText: String, label: String): String? {
        for (sep in listOf("：", ":")) {
            val colonIdx = lineText.indexOf(sep)
            if (colonIdx < 0) continue
            val prefix = lineText.substring(0, colonIdx).trim()
            if (prefix.equals(label, ignoreCase = true) || prefix.endsWith(label, ignoreCase = true)) {
                val value = lineText.substring(colonIdx + sep.length).trim()
                if (value.isNotEmpty()) return value
            }
        }
        if (lineText.startsWith(label, ignoreCase = true)) {
            val afterStart = label.length
            if (afterStart >= lineText.length) return null
            val nextChar = lineText[afterStart]
            // 紧邻字符必须是非字母数字（避免 "品种" 误匹配 "品种信息"）
            if (nextChar.isLetterOrDigit() || isCjk(nextChar)) return null
            val after = lineText.substring(afterStart).trim()
            if (after.isNotEmpty()) return after
        }
        return null
    }

    private fun findVerticalValueLine(
        labelLine: OcrLine, allLines: List<OcrLine>, startIdx: Int
    ): OcrLine? {
        val labelBbox = labelLine.bbox
        val labelBottom = labelBbox.bottom
        val maxYDist = maxOf(labelBbox.height * 3, 60)

        for (i in startIdx until minOf(startIdx + 3, allLines.size)) {
            val candidate = allLines[i]
            val cBbox = candidate.bbox
            val yDist = cBbox.top - labelBottom
            if (yDist < -10) continue
            if (yDist > maxYDist) break

            val overlap = maxOf(0, minOf(labelBbox.right, cBbox.right) - maxOf(labelBbox.left, cBbox.left))
            if (overlap > 0) return candidate
        }
        return null
    }

    /**
     * 豆名打分：在所有行中按"行高 + 位置 + 中文比例 + 数字密度"加权打分。
     * 命中标签词典会大幅减分。
     *
     * 公式：
     *   score = 0.40 * min(height/80, 1)
     *         + 0.30 * (1 - centerY / imageH)
     *         + 0.20 * chineseRatio
     *         + 0.10 * (1 - digitRatio)
     *         - 0.50 * (matchesLabel ? 1 : 0)
     */
    private fun scoreNameCandidate(lines: List<OcrLine>, alreadyExtracted: List<String>): String? {
        if (lines.isEmpty()) return null
        val imageH = lines.maxOf { it.bbox.bottom }.toFloat().coerceAtLeast(1f)
        val allLabelWords = CoffeeDictionary.nameSkipPrefixes

        data class Scored(val text: String, val raw: String, val score: Float)
        val scored = mutableListOf<Scored>()

        for (line in lines) {
            val t = line.text.trim()
            if (t.isEmpty()) continue
            if (t.length < 2 || t.length > 30) continue
            // 豆名不应含数字（克数、批号等都排除）
            if (t.any { it.isDigit() }) continue
            if (alreadyExtracted.any { it.length > 2 && t.contains(it, ignoreCase = true) }) continue
            if (allLabelWords.any { p -> t.startsWith(p, ignoreCase = true) || t.contains(p, ignoreCase = true) }) continue

            val height = line.height().toFloat()
            val centerY = line.centerY()
            val chineseRatio = t.count { isCjk(it) }.toFloat() / t.length
            val digitRatio = t.count { it.isDigit() }.toFloat() / t.length

            val heightScore = (height / 80f).coerceAtMost(1f)
            val yScore = (1f - centerY / imageH).coerceIn(0f, 1f)
            val score = 0.40f * heightScore +
                        0.30f * yScore +
                        0.20f * chineseRatio +
                        0.10f * (1f - digitRatio)

            val cleaned = t
                .replace(Regex("""^[【「《\[].+?[】」》\]]"""), "")
                .replace(Regex("咖啡豆?|coffee|Bean|Blend|单品", RegexOption.IGNORE_CASE), "")
                .trim()
            if (cleaned.isEmpty()) continue
            scored.add(Scored(cleaned, t, score))
        }
        return scored.maxByOrNull { it.score }?.text
    }

    private fun isCjk(c: Char): Boolean {
        val code = c.code
        return code in 0x4E00..0x9FFF ||
               code in 0x3400..0x4DBF ||
               code in 0x20000..0x2A6DF
    }

    /** 标签后必须是非字母数字字符（避免 "Roast" 误匹配 "Roaster"）。 */
    private fun isLabelBoundary(text: String, labelEnd: Int): Boolean {
        if (labelEnd >= text.length) return true
        val c = text[labelEnd]
        return !c.isLetterOrDigit() && !isCjk(c)
    }
}
