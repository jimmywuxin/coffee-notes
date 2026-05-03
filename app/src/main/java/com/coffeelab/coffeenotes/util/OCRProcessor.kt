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

    // Flavor keyword dictionary - comprehensive list grouped by category
    // Multi-word entries MUST come before their sub-components (e.g., "dark chocolate" before "chocolate")
    private val flavorKeywords = listOf(
        // 花香/茶香（多词优先）
        "伯爵茶", "伯爵红茶", "Earl Grey",
        "茉莉花茶", "茉莉绿茶", "Jasmine",
        "乌龙茶", "oolong",
        "红茶", "black tea",
        "绿茶", "green tea",
        "花草", "floral", "flower", "花香", "花香味",
        "玫瑰", "白玫瑰", "洛神玫瑰", "rose",
        "薰衣草", "lavender",
        "洋甘菊", "chamomile",
        "佛手柑", "bergamot",
        "茉莉花", "jasmine flower",
        "接骨木花", "elderflower",
        "香柠", "fragrant lemon", "香水柠檬",

        // 果香（多词优先）
        "热带水果", "tropical fruit", "passion fruit", "百香果",
        "深色水果", "dark fruit", "dark berry",
        "莓果类", "berries", "mixed berries", "综合莓果",
        "蓝莓", "blueberry", "黑莓", "blackberry", "覆盆子", "raspberry",
        "草莓", "strawberry",
        "甜橙", "sweet orange", "血橙", "blood orange",
        "青柚", "green grapefruit", "蜜柚", "grapefruit",
        "柑橘", "citrus", "tangerine",
        "柠檬", "lemon", "lime", "青柠",
        "橙子", "orange", "柚子", "pomelo",
        "葡萄", "grape", "葡萄汁", "葡萄柚", "wine", "葡萄酒", "winey",
        "苹果", "apple", "梨", "pear",
        "桃子", "peach", "油桃", "nectarine", "杏子", "apricot",
        "蜜瓜", "honeydew", "哈密瓜", "cantaloupe",
        "芒果", "mango", "菠萝", "pineapple", "荔枝", "lychee",
        "樱桃", "cherry", "卡西斯", "blackcurrant", "黑醋栗",
        "香蕉", "banana", "番石榴", "guava",
        "车厘子", "cherry", "树莓", "raspberry",
        "杏桃", "黄杏", "apricot",
        "水蜜桃", "juicy peach", "脆心苹果", "crisp apple", "糖心苹果", "苹果",

        // 甜感/焦糖（多词优先）
        "焦糖布丁", "caramel pudding",
        "太妃糖", "toffee", "奶油糖", "butterscotch",
        "焦糖", "caramel", "黄糖", "brown sugar", "黑糖", "muscovado",
        "枫糖", "maple syrup", "maple", "枫糖浆",
        "蜂蜜", "honey", "蜜糖", "honeyed",
        "糖蜜", "molasses",
        "奶油", "butter", "buttery", "creamy", "cream",
        "牛奶巧克力", "milk chocolate", "牛奶巧",
        "白巧克力", "white chocolate",
        "黑巧克力", "dark chocolate", "黑巧",
        "巧克力", "chocolate", "可可", "cocoa",

        // 坚果/可可（多词优先）
        "榛果巧克力", "hazelnut chocolate",
        "杏仁巧克力", "almond chocolate",
        "坚果", "坚果类", "nuts", "mixed nuts",
        "杏仁", "almond", "榛子", "hazelnut", "花生", "peanut", "walnut", "核桃",
        "开心果", "pistachio", "腰果", "cashew",
        "可可粉", "cocoa powder", "可可碎", "cocoa nibs",

        // 香料/草本（多词优先）
        "肉桂卷", "cinnamon roll",
        "香料", "spice", "spices",
        "肉桂", "cinnamon", "桂皮",
        "丁香", "clove", "多香果", "allspice",
        "香草", "vanilla", "香草荚", "vanilla bean",
        "茴香", "fennel", "八角", "star anise",
        "薄荷", "mint", "桉树", "eucalyptus",

        // 烘焙/谷物（多词优先）
        "烤吐司", "toast", "烤面包", "bread crust",
        "烤坚果", "roasted nuts", "烘烤", "roasted",
        "麦芽", "malt", "谷物", "cereal", "grain",
        "饼干", "biscuit", "曲奇", "cookie",
        "燕麦", "oat", "燕麦片",

        // 木质/烟熏（多词优先）
        "木质", "木香", "wood", "木本",
        "雪松", "cedar", "檀木", "sandalwood", "沉香", "agarwood",
        "橡木", "oak", "橡木桶", "barrel",
        "烟草", "tobacco", "烟熏", "smoky", "smoke",
        "皮革", "leather", "麝香", "musk", "muscat",

        // 发酵/酒香（多词优先）
        "威士忌", "whiskey", "whisky",
        "朗姆", "rum", "白兰地", "brandy",
        "香槟", "champagne", "气泡", "bubbly",
        "葡萄酒", "wine", "红酒", "red wine", "热红酒", "mulled wine",
        "酵母", "yeast", "发酵", "ferment",

        // 其他（多词优先）
        "芝麻", "sesame", "杏仁饼", "marzipan",
        "杏脯", "dried apricot", "葡萄干", "raisin",
        "无花果", "fig", "枣子", "date",
        "腐植土", "earthy", "土壤", "soil",
        "动物", "animal", "野味", "game",
        "麝香", "muscat", "猫尿", "cat urine" // 有些特殊处理法风味
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

    // 从文本中提取所有匹配的风味词（贪婪最长匹配，不重叠）
    private fun extractFlavorFromLine(text: String, flavors: MutableList<String>) {
        // 1. 找出所有匹配的关键词及其位置
        val sortedKeywords = flavorKeywords.sortedByDescending { it.length }
        val matches = mutableListOf<Triple<Int, Int, String>>() // (start, end, keyword)

        for (kw in sortedKeywords) {
            if (kw.length < 2) continue
            var searchStart = 0
            while (true) {
                val idx = text.indexOf(kw, searchStart, ignoreCase = true)
                if (idx < 0) break
                matches.add(Triple(idx, idx + kw.length, kw))
                searchStart = idx + 1
            }
        }

        // 2. 按长度降序，贪婪选取不重叠的最佳匹配
        val selected = mutableListOf<Triple<Int, Int, String>>()
        for (match in matches.sortedByDescending { it.second - it.first }) {
            val start = match.first
            val end = match.second
            // 检查是否与已选中的区间重叠
            val overlaps = selected.any { existing -> start < existing.second && end > existing.first }
            if (!overlaps) {
                selected.add(match)
            }
        }

        // 3. 加入结果（按原文顺序排列）
        for (match in selected.sortedBy { it.first }) {
            val kw = match.third
            if (!flavors.contains(kw)) {
                flavors.add(kw)
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
