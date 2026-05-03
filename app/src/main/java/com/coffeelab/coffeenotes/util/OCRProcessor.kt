package com.coffeelab.coffeenotes.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import com.google.mlkit.vision.common.InputImage
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
    val flavors: List<String> = emptyList(),
    val fullText: String = ""
)

object OCRProcessor {

    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    // Flavor keyword dictionary - grouped by category for better matching
    private val flavorKeywords = listOf(
        // 花香/果香
        "花香", "花香味", "floral", "flower",
        "莓果", "浆果", "berry", "berries",
        "柑橘", "柠檬", "橙子", "citrus", "lemon", "orange", "tangerine",
        "热带水果", "tropical fruit", "passion fruit", "百香果",
        "葡萄", "grape", "wine", "葡萄酒",
        "荔枝", "lychee",
        "桃子", "peach", "苹果", "apple", "芒果", "mango",
        "草莓", "strawberry", "樱桃", "cherry", "banana",
        // 甜感
        "焦糖", "caramel", "蜂蜜", "honey", "红糖", "brown sugar",
        "奶油", "butter", "creamy", "牛奶巧克力", "牛奶巧",
        "黑巧克力", "黑巧", "巧克力", "chocolate", "cocoa",
        // 坚果/烘焙
        "坚果", "杏仁", "榛子", "nut", "almond", "hazelnut", "peanut", "花生",
        "可可", "榛果",
        // 特殊风味
        "香料", "spice", "肉桂", "cinnamon",
        "烟草", "tobacco", "木质", "wood", " cedar", "檀木",
        "茶", "茶感", "tea", "红茶", "伯爵",
        "威士忌", "whiskey", "朗姆", "rum",
        // 其他
        "芝麻", "sesame", "麝香", "muscat"
    )

    // Roast level keywords (order matters - longer/more specific first)
    private val roastLevels = listOf(
        "浅度烘焙", "中度烘焙", "中深度烘焙", "深度烘焙",
        "浅烘焙", "中烘焙", "深烘焙",
        "light roast", "medium roast", "dark roast",
        "light", "medium", "dark",
        "浅烘", "中烘", "深烘", "浅中烘", "中深烘",
        "浅", "中", "深"
    )

    // Process keywords (order matters)
    private val processKeywords = listOf(
        "厌氧发酵", "厌氧日晒", "厌氧水洗", "酒桶发酵", "橡木桶发酵",
        "蜜处理", "黑蜜", "红蜜", "黄蜜", "白蜜",
        "水洗", "washed", "wet process",
        "日晒", "natural", "dry process",
        "半水洗", "湿刨法", "湿刨",
        "honey", "anaerobic", "wine", "barrel"
    )

    // Roaster label patterns
    private val roasterPatterns = listOf(
        "烘焙商", "烘焙", "品牌", "Roaster", "roaster", "ROASTER",
        "Brand", "brand", "BRAND", "Producer", "producer"
    )

    // Origin label patterns
    private val originLabels = listOf(
        "产地", "产区", "国家", "Origin", "origin", "Country", "country",
        "Region", "region"
    )

    // All common coffee origins (for direct mention without label)
    private val commonOrigins = listOf(
        "埃塞俄比亚", "Ethiopia", "耶加雪菲", "西达摩", "古吉", "科契尔",
        "哥伦比亚", "Colombia", "娜玲峡谷",
        "巴西", "Brazil", "喜拉多", "圣保罗",
        "肯尼亚", "Kenya", "涅里", "麒麟区",
        "哥斯达黎加", "Costa Rica", "塔拉珠", "赫尔德",
        "巴拿马", "Panama", "波奎特", "翡翠庄园",
        "危地马拉", "Guatemala", "安提瓜", "薇薇特南果",
        "印尼", "Indonesia", "曼特宁", "爪哇", "苏门答腊",
        "云南", "Yunnan", "中国云南",
        "卢旺达", "Rwanda", "布隆迪", "Burundi",
        "秘鲁", "Peru", "洪都拉斯", "Honduras",
        "墨西哥", "Mexico", "牙买加", "Jamaica", "蓝山",
        "危地马拉", "坦桑尼亚", "Tanzania", "厄瓜多尔", "Ecuador"
    )

    // Coffee name keywords (line should contain one of these)
    private val coffeeNameKeywords = listOf(
        "咖啡", "coffee", "豆", "bean", "Blend", "blend", "单品"
    )

    suspend fun processBitmap(bitmap: Bitmap): OCRResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val result = recognizer.process(image).await()
            val fullText = result.text
            parseText(fullText)
        } catch (e: Exception) {
            OCRResult(fullText = "识别失败: ${e.message}")
        }
    }

    /**
     * 解析文本，提取咖啡豆信息
     * 采用多行综合分析策略：
     * 1. 将文本按行/文本块拆分
     * 2. 先识别所有标签-值对
     * 3. 再做模糊匹配（无标签的产地、豆名等）
     */
    private fun parseText(text: String): OCRResult {
        // 获取所有识别的文本块（ML Kit 支持检测段落/行）
        val blocks = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        var roaster = ""
        var name = ""
        var origin = ""
        var variety = ""
        var process = ""
        var roastLevel = ""
        val flavors = mutableListOf<String>()

        // 用于暂存可能的豆名候选（当没有明确标签时）
        val nameCandidates = mutableListOf<String>()

        // ==========================================
        // 第一轮：精确标签匹配
        // ==========================================
        for (line in blocks) {
            val hasColon = line.contains(":") || line.contains("：")

            // 烘焙商检测
            if (roaster.isEmpty() && hasColon && roasterPatterns.any { line.contains(it, ignoreCase = true) }) {
                roaster = extractAfterColon(line)
            }

            // 产地标签检测
            if (origin.isEmpty() && hasColon && originLabels.any { line.contains(it, ignoreCase = true) }) {
                origin = extractAfterColon(line)
            }

            // 品种标签检测
            if (variety.isEmpty() && line.contains("品种", ignoreCase = true)) {
                variety = extractAfterColon(line)
            }

            // 处理法标签检测
            if (process.isEmpty() && (line.contains("处理", ignoreCase = true) || line.contains("process", ignoreCase = true))) {
                val value = extractAfterColon(line)
                if (value.isNotEmpty()) {
                    process = value
                }
            }

            // 烘焙度标签检测
            if (roastLevel.isEmpty() && (line.contains("烘焙度", ignoreCase = true) || line.contains("Roast", ignoreCase = true))) {
                roastLevel = extractAfterColon(line)
            }

            // 风味标签检测
            if (line.contains("风味", ignoreCase = true) || line.contains("flavor", ignoreCase = true) || line.contains("tasting", ignoreCase = true)) {
                val flavorText = extractAfterColon(line)
                if (flavorText.isEmpty()) {
                    // 风味标签后没有冒号，直接从整行提取风味词
                    extractFlavorFromLine(line, flavors)
                } else {
                    extractFlavorFromLine(flavorText, flavors)
                }
            }
        }

        // ==========================================
        // 第二轮：处理法关键词匹配（无标签）
        // ==========================================
        if (process.isEmpty()) {
            for (line in blocks) {
                if (line.length > 30) continue  // 处理法通常是短词
                for (kw in processKeywords) {
                    if (line.contains(kw, ignoreCase = true)) {
                        process = kw
                        break
                    }
                }
                if (process.isNotEmpty()) break
            }
        }

        // ==========================================
        // 第三轮：烘焙度关键词匹配（无标签）
        // ==========================================
        if (roastLevel.isEmpty()) {
            for (line in blocks) {
                if (line.length > 30) continue
                for (rl in roastLevels) {
                    if (line.contains(rl, ignoreCase = true)) {
                        roastLevel = rl
                        break
                    }
                }
                if (roastLevel.isNotEmpty()) break
            }
        }

        // ==========================================
        // 第四轮：产地模糊匹配（无标签）
        // ==========================================
        if (origin.isEmpty()) {
            for (line in blocks) {
                if (line.length > 40) continue
                for (o in commonOrigins) {
                    if (line.contains(o, ignoreCase = true)) {
                        // 如果行中没有冒号/等号，直接取匹配词前后几个字
                        origin = extractAroundMatch(line, o)
                        break
                    }
                }
                if (origin.isNotEmpty()) break
            }
        }

        // ==========================================
        // 第五轮：品种模糊匹配（无标签）
        // ==========================================
        if (variety.isEmpty()) {
            val varietyPatterns = listOf(
                "瑰夏", "Geisha", "gesha", "SL28", "SL34", "74158", "74112",
                "波旁", "Bourbon", "bourbon", "卡杜拉", "Caturra", "catuai",
                "铁皮卡", "Typica", "typica", "帕卡斯", "Pacas", "pacamara",
                "黄波旁", "黄卡杜艾", "卡杜艾", "艺伎"
            )
            for (line in blocks) {
                for (vp in varietyPatterns) {
                    if (line.contains(vp, ignoreCase = true)) {
                        variety = vp
                        break
                    }
                }
                if (variety.isNotEmpty()) break
            }
        }

        // ==========================================
        // 第六轮：豆名识别
        // ==========================================
        if (name.isEmpty()) {
            // 策略1：包含咖啡/豆/bean 关键词的行
            for (line in blocks) {
                if (line.length < 3 || line.length > 50) continue
                if (line.contains("http", ignoreCase = true)) continue
                if (coffeeNameKeywords.any { line.contains(it, ignoreCase = true) }) {
                    nameCandidates.add(line)
                }
            }

            // 选择最短的候选（最可能是豆名）
            if (nameCandidates.isNotEmpty()) {
                name = nameCandidates.minByOrNull { it.length } ?: nameCandidates.first()
                name = name.replace(Regex("咖啡豆?|豆|coffee|Bean"), "").trim()
            }
        }

        // 策略2：如果还没找到，取第一行短文本（排除已知标签行）
        if (name.isEmpty() && blocks.isNotEmpty()) {
            for (line in blocks) {
                if (line.length in 2..30 &&
                    !line.contains("http", ignoreCase = true) &&
                    !roasterPatterns.any { line.contains(it, ignoreCase = true) } &&
                    !originLabels.any { line.contains(it, ignoreCase = true) } &&
                    !line.contains("处理", ignoreCase = true) &&
                    !line.contains("烘焙", ignoreCase = true) &&
                    !line.contains("品种", ignoreCase = true) &&
                    !line.contains("风味", ignoreCase = true) &&
                    !line.contains("产地", ignoreCase = true) &&
                    !containsFlavorKeyword(line)
                ) {
                    name = line
                    break
                }
            }
        }

        // ==========================================
        // 第七轮：风味词全局搜索（跨行）
        // ==========================================
        extractFlavorFromLine(text, flavors)

        return OCRResult(
            roaster = roaster,
            name = name,
            origin = origin,
            variety = variety,
            process = process,
            roastLevel = normalizeRoastLevel(roastLevel),
            flavors = flavors.distinct(),
            fullText = text
        )
    }

    // 从行中提取风味词
    private fun extractFlavorFromLine(text: String, flavors: MutableList<String>) {
        // 先找风味标签后的内容
        val flavorText = text
            .replace(Regex("风味[：:]\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("flavor[：:]?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("tasting notes?[：:]?\\s*", RegexOption.IGNORE_CASE), "")

        // 用分隔符拆分
        val parts = flavorText.split(Regex("[,，、｜|\\s]+"))
        for (part in parts) {
            val word = part.trim().take(10)
            if (word.isNotEmpty()) {
                // 检查是否匹配已知风味词
                val matched = flavorKeywords.find { word.contains(it, ignoreCase = true) }
                if (matched != null && !flavors.contains(matched)) {
                    flavors.add(matched)
                } else if (matched == null && word.length >= 2 && word.all { it.isLetterOrDigit() || it in " '" }) {
                    // 没有精确匹配，但词看起来像风味词（英文/中文）
                    if (!flavors.contains(word)) {
                        flavors.add(word)
                    }
                }
            }
        }

        // 额外：全文扫描英文风味词（有些包装直接写英文）
        for (kw in flavorKeywords) {
            if (kw.length < 3) continue
            if (text.contains(kw, ignoreCase = true) && !flavors.any { text.contains(it, ignoreCase = true) }) {
                // 防止重复
            }
        }
    }

    // 检查行中是否包含风味关键词
    private fun containsFlavorKeyword(line: String): Boolean {
        return flavorKeywords.any { line.contains(it, ignoreCase = true) }
    }

    // 提取冒号后的值
    private fun extractAfterColon(line: String): String {
        val parts = line.split(Regex("[:：]"), limit = 2)
        return if (parts.size >= 2) parts[1].trim() else ""
    }

    // 在匹配词周围提取上下文
    private fun extractAroundMatch(line: String, match: String): String {
        val idx = line.indexOf(match, ignoreCase = true)
        if (idx < 0) return match
        val start = maxOf(0, idx - 5)
        val end = minOf(line.length, idx + match.length + 10)
        return line.substring(start, end).trim()
    }

    // 标准化烘焙度
    private fun normalizeRoastLevel(level: String): String {
        val l = level.lowercase()
        return when {
            l.contains("浅烘") || l.contains("light") -> "浅烘"
            l.contains("深烘") || l.contains("dark") -> "深烘"
            l.contains("中烘") || l.contains("medium") -> "中烘"
            l.contains("浅中") -> "浅中烘"
            l.contains("中深") -> "中深烘"
            else -> level
        }
    }
}
