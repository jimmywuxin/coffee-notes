package com.coffeelab.coffeenotes.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

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

    /**
     * 基于 bounding box 的空间分析：
     * 将所有 TextElement 配对，检查垂直距离和 X 重叠，判断标签-值关系。
     * 同时保留原有规则匹配作为兜底。
     */
    private fun parseWithBoundingBox(text: Text, rawText: String): OCRResult {
        // ── 提取所有 elements（带 pos + 文本）──
        data class Elem(val text: String, val rect: Rect)
        val elements = mutableListOf<Elem>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (elem in line.elements) {
                    val t = elem.text.trim()
                    if (t.isNotEmpty()) {
                        elements.add(Elem(t, elem.boundingBox ?: Rect()))
                    }
                }
            }
        }

        var roaster = ""
        var name = ""
        var origin = ""
        var region = ""
        var variety = ""
        var process = ""
        var roastLevel = ""
        var estate = ""
        val flavors = mutableListOf<String>()

        // ── 用 bounding box 空间分析配对标签-值 ──
        // 判断两个 element 是否在同一列：X 区间中心点接近 或 bounding box 水平重叠 > 30%
        fun isSameColumn(a: Rect, b: Rect): Boolean {
            val aL = a.left; val aR = a.right
            val bL = b.left; val bR = b.right
            val overlap = maxOf(0, minOf(aR, bR) - maxOf(aL, bL))
            val aW = aR - aL; val bW = bR - bL
            return overlap.toFloat() / maxOf(aW, bW) > 0.3f
        }

        // 检查 elem 文本是否匹配某个标签列表
        fun matchesLabel(elemText: String, labels: List<String>): Boolean {
            return labels.any { elemText.equals(it, ignoreCase = true) || elemText.contains(it, ignoreCase = true) }
        }

        // 标签到字段的映射 + 关键词排除
        data class LabelMapping(
            val labels: List<String>,
            val setter: (String) -> Unit,
            val getter: () -> String
        )

        val mappings = listOf(
            LabelMapping(roasterLabels, { roaster = it }, { roaster }),
            LabelMapping(originLabels, { origin = it }, { origin }),
            LabelMapping(regionLabels, { region = it }, { region }),
            LabelMapping(varietyLabels, { variety = it }, { variety }),
            LabelMapping(processLabels, { process = it }, { process }),
            LabelMapping(roastLevelLabels, { roastLevel = it }, { roastLevel }),
            LabelMapping(estateLabels, { estate = it }, { estate })
        )

        for ((idx, elem) in elements.withIndex()) {
            if (idx >= elements.size - 1) continue
            val next = elements[idx + 1]
            if (!isSameColumn(elem.rect, next.rect)) continue
            // 检查 elem 是否是某一个标签
            for (mapping in mappings) {
                if (mapping.getter().isEmpty() && matchesLabel(elem.text, mapping.labels)) {
                    mapping.setter(next.text)
                    break
                }
            }
        }

        // ── 兜底：原有文本规则 ──
        val blocks = rawText.split("\n".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }

        // 品种模糊匹配
        if (variety.isEmpty()) {
            for (line in blocks) {
                for (vp in knownVarieties) {
                    if (line.contains(vp, ignoreCase = true)) {
                        variety = line.trim()
                        break
                    }
                }
                if (variety.isNotEmpty()) break
            }
        }

        // 产地模糊匹配
        if (origin.isEmpty()) {
            for (line in blocks) {
                for (o in commonOrigins) {
                    if (o.length < 2) continue
                    if (line.contains(o, ignoreCase = true)) {
                        origin = o
                        val rest = line.replace(o, "").trim()
                        if (rest.isNotEmpty() && region.isEmpty()) region = rest
                        break
                    }
                }
                if (origin.isNotEmpty()) break
            }
        }

        // 处理法模糊匹配
        if (process.isEmpty()) {
            val knownProcesses = listOf(
                "水洗", "日晒", "蜜处理", "厌氧", "湿刨", "washed", "natural", "honey"
            )
            for (line in blocks) {
                for (kp in knownProcesses) {
                    if (line.contains(kp, ignoreCase = true)) { process = kp; break }
                }
                if (process.isNotEmpty()) break
            }
        }

        // 烘焙度模糊匹配
        if (roastLevel.isEmpty()) {
            val roastLevels = listOf("浅度烘焙","中度烘焙","深度烘焙","中深度烘焙","浅烘焙","中烘焙","深烘焙","浅烘","中烘","深烘","浅","中","深")
            for (line in blocks) {
                for (rl in roastLevels) {
                    if (line.contains(rl, ignoreCase = true)) { roastLevel = rl; break }
                }
                if (roastLevel.isNotEmpty()) break
            }
        }

        // 豆名
        val nameCandidates = mutableListOf<String>()
        if (name.isEmpty()) {
            for (line in blocks) {
                if (line.length in 3..50 && coffeeNameKeywords.any { line.contains(it, ignoreCase = true) }) {
                    nameCandidates.add(line)
                }
            }
            if (nameCandidates.isNotEmpty()) {
                name = nameCandidates.minByOrNull { it.length } ?: nameCandidates.first()
                name = name.replace(Regex("咖啡豆?|豆|coffee|Bean", RegexOption.IGNORE_CASE), "").trim()
            }
        }
        if (name.isEmpty() && blocks.isNotEmpty()) {
            for (line in blocks) {
                if (line.length in 2..30 &&
                    !roasterLabels.any { line.contains(it, ignoreCase = true) } &&
                    !originLabels.any { line.contains(it, ignoreCase = true) } &&
                    !line.contains("处理") && !line.contains("烘焙") &&
                    !line.contains("品种") && !line.contains("风味")
                ) { name = line; break }
            }
        }

        // 风味词
        extractFlavors(rawText, flavors)

        // 烘焙度标准化
        roastLevel = when {
            roastLevel.contains("浅") -> "浅烘"
            roastLevel.contains("深") -> "深烘"
            roastLevel.contains("中") -> "中烘"
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

    private fun extractFlavors(text: String, flavors: MutableList<String>) {
        val sorted = flavorKeywords.sortedByDescending { it.length }
        val matches = mutableListOf<Triple<Int, Int, String>>()
        for (kw in sorted) {
            if (kw.length < 2) continue
            var pos = 0
            while (true) {
                val idx = text.indexOf(kw, pos, ignoreCase = true)
                if (idx < 0) break
                matches.add(Triple(idx, idx + kw.length, kw)); pos = idx + 1
            }
        }
        val selected = mutableListOf<Triple<Int, Int, String>>()
        for (m in matches.sortedByDescending { it.second - it.first }) {
            if (selected.none { m.first < it.second && m.second > it.first }) selected.add(m)
        }
        for (m in selected.sortedBy { it.first }) {
            if (!flavors.contains(m.third)) flavors.add(m.third)
        }
    }
}
