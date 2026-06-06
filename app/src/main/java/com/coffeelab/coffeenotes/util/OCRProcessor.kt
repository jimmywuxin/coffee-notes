package com.coffeelab.coffeenotes.util

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.tasks.await

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
    val fullText: String = ""
)

object OCRProcessor {

    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    private val flavorKeywords = FlavorKeywords.keywords

    // ── 标签词典 ──

    private val roasterLabels = listOf(
        "烘焙商", "烘培商", "烘培", "烘焙", "品牌", "出品", "制造", "公司",
        "Roaster", "roaster", "ROASTER", "Roasted by", "roasted by",
        "Brand", "brand", "BRAND", "Producer", "producer", "Company"
    )

    private val originLabels = listOf(
        "原料产地", "生豆产地", "原产地", "原产国", "产地", "产自", "来自",
        "国家", "Origin", "origin", "Country", "country", "From", "from", "Source", "source"
    )

    private val regionLabels = listOf(
        "产区", "区域", "Region", "region"
    )

    private val varietyLabels = listOf(
        "豆种", "品种", "Variety", "variety", "Varietal", "varietal", "Cultivar", "cultivar"
    )

    private val processLabels = listOf(
        "处理方式", "处理", "精制", "Process", "process"
    )

    private val roastLevelLabels = listOf(
        "烘焙程度", "烘焙度", "烘焙", "烘培", "Roast Level", "Roast level", "Roast", "roast"
    )

    private val estateLabels = listOf(
        "庄园", "处理站", "农庄", "农场", "合作社", "Estate", "estate", "Farm", "farm"
    )

    private val knownVarieties = listOf(
        "瑰夏", "Geisha", "gesha", "波旁", "Bourbon", "bourbon",
        "卡杜拉", "Caturra", "caturra", "卡杜艾", "Catuai", "catuai",
        "铁皮卡", "Typica", "typica", "帕卡玛拉", "Pacamara", "pacamara",
        "SL28", "SL34", "SL14", "SL24", "74158", "74112",
        "黄波旁", "红波旁", "粉波旁", "黄卡杜艾", "艺伎"
    )

    private val coffeeNameKeywords = listOf(
        "咖啡", "coffee", "豆", "bean", "Blend", "blend", "单品"
    )

    private val commonOrigins = listOf(
        "埃塞俄比亚", "Ethiopia", "耶加雪菲", "西达摩", "古吉",
        "哥伦比亚", "Colombia", "巴西", "Brazil",
        "肯尼亚", "Kenya", "哥斯达黎加", "Costa Rica",
        "巴拿马", "Panama", "危地马拉", "Guatemala",
        "印尼", "Indonesia", "曼特宁", "苏门答腊", "Sumatra",
        "爪哇", "Java", "云南", "卢旺达", "Rwanda",
        "布隆迪", "Burundi", "秘鲁", "Peru", "洪都拉斯", "Honduras",
        "墨西哥", "Mexico", "牙买加", "Jamaica", "蓝山",
        "坦桑尼亚", "Tanzania", "厄瓜多尔", "Ecuador",
        "萨尔瓦多", "El Salvador", "尼加拉瓜", "Nicaragua",
        "巴布亚新几内亚", "PNG", "乌干达", "Uganda",
        "东帝汶", "Timor", "玻利维亚", "Bolivia",
        "赞比亚", "Zambia", "印度", "India"
    )

    private val knownProcesses = listOf(
        "水洗", "日晒", "蜜处理", "厌氧", "湿刨", "washed", "natural", "honey"
    )

    // ── 核心识别 ──

    suspend fun processBitmap(bitmap: Bitmap): OCRResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val result = recognizer.process(image).await()
            val fullText = result.text
            parseWithBoundingBox(result, fullText)
        } catch (e: Exception) {
            OCRResult(fullText = "识别失败: ${e.message}")
        }
    }

    // ── 数据结构 ──

    private data class TextLine(val text: String, val rect: Rect)

    private data class FieldMapping(
        val labels: List<String>,
        val setter: (String) -> Unit,
        val name: String
    )

    // ── 行级解析 ──

    private fun parseWithBoundingBox(text: Text, rawText: String): OCRResult {
        // 1. 从 ML Kit 结果中提取行级文本 + 位置
        val lines = mutableListOf<TextLine>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val t = line.text.trim()
                if (t.isNotEmpty()) {
                    lines.add(TextLine(t, line.boundingBox ?: Rect()))
                }
            }
        }

        var roaster = ""; var name = ""; var origin = ""
        var region = ""; var variety = ""; var process = ""
        var roastLevel = ""; var estate = ""
        val flavors = mutableListOf<String>()
        val setFields = mutableSetOf<String>() // 记录已填充字段，同字段仅填充一次

        val allMappings = listOf(
            FieldMapping(roasterLabels, { roaster = it; setFields.add("roaster") }, "roaster"),
            FieldMapping(originLabels, { origin = it; setFields.add("origin") }, "origin"),
            FieldMapping(regionLabels, { region = it; setFields.add("region") }, "region"),
            FieldMapping(varietyLabels, { variety = it; setFields.add("variety") }, "variety"),
            FieldMapping(processLabels, { process = it; setFields.add("process") }, "process"),
            FieldMapping(roastLevelLabels, { roastLevel = it; setFields.add("roastLevel") }, "roastLevel"),
            FieldMapping(estateLabels, { estate = it; setFields.add("estate") }, "estate")
        )

        // ── 2. 水平匹配：同行的"标签: 值"模式 ──
        for (line in lines) {
            val lineText = line.text
            if (lineText.isEmpty()) continue

            for (mapping in allMappings) {
                if (mapping.name in setFields) continue
                for (label in mapping.labels) {
                    val value = extractValueAfterLabel(lineText, label)
                    if (value != null) {
                        mapping.setter(value)
                        break
                    }
                }
            }
        }

        // ── 3. 垂直匹配：标签行在下方的值行 ──
        for (i in lines.indices) {
            val currentLine = lines[i]
            val lineText = currentLine.text
            if (lineText.isEmpty()) continue

            for (mapping in allMappings) {
                if (mapping.name in setFields) continue
                // 检查当前行是否精确匹配某个标签（值应单独在下一行）
                val matchedLabel = mapping.labels.find {
                    lineText.equals(it, ignoreCase = true) ||
                    lineText.startsWith(it, ignoreCase = true)
                }
                if (matchedLabel != null) {
                    val valueLine = findVerticalValueLine(currentLine, lines, i + 1)
                    if (valueLine != null) {
                        mapping.setter(valueLine.text)
                    }
                    break
                }
            }
        }

        // ── 4. 兜底：品种 + 产地模糊匹配 ──
        val lineTexts = lines.map { it.text }

        // 品种（Levenshtein 模糊 + 精确子串）
        if ("variety" !in setFields) {
            variety = fuzzyMatchFirst(lineTexts, knownVarieties, 0.7f)
            if (variety.isEmpty()) {
                // 精确子串匹配（仅提取匹配部分，非整行）
                for (lineText in lineTexts) {
                    for (vp in knownVarieties) {
                        val idx = lineText.indexOf(vp, ignoreCase = true)
                        if (idx >= 0) {
                            variety = lineText.substring(idx, idx + vp.length)
                            break
                        }
                    }
                    if (variety.isNotEmpty()) break
                }
            }
        }

        // 产地模糊匹配
        if ("origin" !in setFields) {
            for (lineText in lineTexts) {
                var matchedOrigin = ""
                var matchedRegion = ""
                for (o in commonOrigins) {
                    if (o.length < 2) continue
                    val idx = lineText.indexOf(o, ignoreCase = true)
                    if (idx >= 0) {
                        matchedOrigin = o
                        val rest = lineText.replace(o, "", ignoreCase = true).trim()
                        // 仅当剩余部分看起来像是产区/地名时才设为 region
                        if (rest.isNotEmpty() && region.isEmpty() &&
                            !rest.contains("烘焙") && !rest.contains("处理") &&
                            !rest.contains("风味") && rest.length < 20
                        ) {
                            matchedRegion = rest
                        }
                        break
                    }
                }
                if (matchedOrigin.isNotEmpty()) {
                    origin = matchedOrigin
                    if (matchedRegion.isNotEmpty()) region = matchedRegion
                    break
                }
            }
        }

        // 处理法模糊匹配
        if ("process" !in setFields) {
            process = fuzzyMatchFirst(lineTexts, knownProcesses, 0.7f)
            if (process.isEmpty()) {
                for (lineText in lineTexts) {
                    for (kp in knownProcesses) {
                        if (lineText.contains(kp, ignoreCase = true)) { process = kp; break }
                    }
                    if (process.isNotEmpty()) break
                }
            }
        }

        // 烘焙度模糊匹配
        if ("roastLevel" !in setFields) {
            val roastLevels = listOf(
                "浅度烘焙", "中度烘焙", "深度烘焙", "中深度烘焙",
                "浅烘焙", "中烘焙", "深烘焙", "浅烘", "中烘", "深烘"
            )
            roastLevel = fuzzyMatchFirst(lineTexts, roastLevels, 0.6f)
            if (roastLevel.isEmpty()) {
                for (lineText in lineTexts) {
                    if (lineText.contains("浅", ignoreCase = true) &&
                        (lineText.contains("烘") || lineText.contains("焙"))) {
                        roastLevel = "浅烘"; break
                    }
                    if (lineText.contains("中", ignoreCase = true) &&
                        (lineText.contains("烘") || lineText.contains("焙"))) {
                        roastLevel = "中烘"; break
                    }
                    if (lineText.contains("深", ignoreCase = true) &&
                        (lineText.contains("烘") || lineText.contains("焙"))) {
                        roastLevel = "深烘"; break
                    }
                }
            }
        }

        // 豆名：含咖啡关键词的行，无则取第一个非标签短行
        if (name.isEmpty()) {
            val candidates = lineTexts.filter {
                it.length in 3..50 && coffeeNameKeywords.any { kw -> it.contains(kw, ignoreCase = true) }
            }
            if (candidates.isNotEmpty()) {
                name = candidates.minByOrNull { it.length } ?: candidates.first()
                name = name.replace(Regex("咖啡豆?|豆|coffee|Bean", RegexOption.IGNORE_CASE), "").trim()
            }
        }
        if (name.isEmpty()) {
            val skipPrefixes = (roasterLabels + originLabels + regionLabels + varietyLabels +
                    processLabels + roastLevelLabels + estateLabels + listOf("处理", "烘焙", "风味", "品种"))
            for (lineText in lineTexts) {
                if (lineText.length in 2..30 &&
                    skipPrefixes.none { lineText.startsWith(it, ignoreCase = true) || lineText.contains(it, ignoreCase = true) } &&
                    !lineText.any { it.isDigit() }
                ) {
                    // 确认不是已经被提取为值的内容
                    val alreadyExtracted = listOfNotNull(roaster, origin, variety, process, roastLevel, estate)
                    if (alreadyExtracted.none { lineText.contains(it, ignoreCase = true) && it.length > 2 }) {
                        name = lineText
                        break
                    }
                }
            }
        }

        // 风味提取（排除已匹配字段中的词）
        val excludedTerms = listOfNotNull(roaster, name, origin, variety, process, roastLevel, estate)
            .filter { it.length >= 2 }.toSet()
        extractFlavors(rawText, flavors, excludedTerms)

        // 烘焙度标准化
        roastLevel = when {
            roastLevel.contains("浅") -> "浅烘"
            roastLevel.contains("深") -> "深烘"
            roastLevel.contains("中") && !roastLevel.contains("浅") && !roastLevel.contains("深") -> "中烘"
            else -> roastLevel
        }

        return OCRResult(
            roaster = roaster, name = name,
            origin = origin, region = region,
            variety = variety, process = process,
            roastLevel = roastLevel, estate = estate,
            flavors = flavors.distinct(), fullText = rawText
        )
    }

    // ── 水平配对：从行文本中提取标签后的值 ──

    private fun extractValueAfterLabel(lineText: String, label: String): String? {
        // 冒号分隔
        for (sep in listOf("：", ":")) {
            val colonIdx = lineText.indexOf(sep)
            if (colonIdx < 0) continue
            val prefix = lineText.substring(0, colonIdx).trim()
            if (prefix.equals(label, ignoreCase = true) || prefix.endsWith(label, ignoreCase = true)) {
                val value = lineText.substring(colonIdx + sep.length).trim()
                if (value.isNotEmpty()) return value
            }
        }
        // 标签后紧跟空格分隔（无冒号）
        if (lineText.startsWith(label, ignoreCase = true)) {
            val after = lineText.substring(label.length).trim()
            if (after.isNotEmpty()) return after
        }
        return null
    }

    // ── 垂直配对：在标签行下方寻找值行 ──

    private fun findVerticalValueLine(labelLine: TextLine, allLines: List<TextLine>, startIdx: Int): TextLine? {
        val labelRect = labelLine.rect
        val labelBottom = labelRect.bottom
        val maxYDist = maxOf(labelRect.height() * 3, 60) // 最多向下 3 倍行高

        for (i in startIdx until minOf(startIdx + 3, allLines.size)) {
            val candidate = allLines[i]
            val cRect = candidate.rect
            val yDist = cRect.top - labelBottom
            if (yDist < -10) continue            // 在上方，跳过
            if (yDist > maxYDist) break           // 太远，终止搜索

            // X 方向有重叠即可视为垂直关联
            val overlap = maxOf(0, minOf(labelRect.right, cRect.right) - maxOf(labelRect.left, cRect.left))
            if (overlap > 0) return candidate
        }
        return null
    }

    // ── Levenshtein 模糊匹配 ──

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length; val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) for (j in 1..n) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
        }
        return dp[m][n]
    }

    private fun levenshteinSimilarity(s1: String, s2: String): Float {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1f
        return 1f - levenshteinDistance(s1.lowercase(), s2.lowercase()).toFloat() / maxLen
    }

    /** 从文本行列表中模糊匹配第一个命中已知词条的行。 */
    private fun fuzzyMatchFirst(lines: List<String>, dictionary: List<String>, threshold: Float): String {
        for (lineText in lines) {
            // 把行按分隔符拆成单个词，对每个词做编辑距离匹配
            val words = lineText.split(Regex("[\\s,，、：:()（）]")).filter { it.length >= 2 }
            var bestWord = ""; var bestScore = threshold
            for (word in words) {
                for (dictWord in dictionary) {
                    val score = levenshteinSimilarity(word, dictWord)
                    if (score > bestScore) {
                        bestScore = score
                        bestWord = dictWord
                    }
                }
            }
            if (bestWord.isNotEmpty()) return bestWord
        }
        return ""
    }

    // ── 风味提取 ──

    private fun extractFlavors(text: String, flavors: MutableList<String>, excludedTerms: Set<String> = emptySet()) {
        val sorted = flavorKeywords.sortedByDescending { it.length }
        val matches = mutableListOf<Triple<Int, Int, String>>()

        for (kw in sorted) {
            if (kw.length < 2) continue
            // 跳过已匹配字段中包含的风味词（避免风味标签被产地/豆名字段污染）
            if (excludedTerms.any { excluded ->
                    excluded.length >= 2 && (excluded.contains(kw, ignoreCase = true) || kw.contains(excluded, ignoreCase = true))
                }) continue

            var pos = 0
            while (true) {
                val idx = text.indexOf(kw, pos, ignoreCase = true)
                if (idx < 0) break
                matches.add(Triple(idx, idx + kw.length, kw))
                pos = idx + 1
            }
        }

        // 去重：同位置长匹配优先
        val selected = mutableListOf<Triple<Int, Int, String>>()
        for (m in matches.sortedByDescending { it.second - it.first }) {
            if (selected.none { m.first < it.second && m.second > it.first }) selected.add(m)
        }
        for (m in selected.sortedBy { it.first }) {
            if (!flavors.contains(m.third)) flavors.add(m.third)
        }
    }
}
