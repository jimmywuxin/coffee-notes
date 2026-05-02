package com.coffeelab.coffeenotes.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
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
    val flavors: List<String> = emptyList(),
    val fullText: String = ""
)

object OCRProcessor {

    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    // Flavor keyword dictionary
    private val flavorKeywords = listOf(
        "花香", "花香味", "floral",
        "莓果", "浆果", "berry", "berries",
        "柑橘", "柠檬", "橙子", "citrus", "lemon", "orange",
        "坚果", "杏仁", "榛子", "nut", "almond", "hazelnut",
        "巧克力", "可可", "chocolate", "cocoa",
        "焦糖", "caramel",
        "蜂蜜", "honey",
        "水果", "热带水果", "tropical fruit",
        "葡萄", "葡萄酒", "grape", "wine",
        "茶", "茶感", "tea",
        "红糖", "brown sugar",
        "奶油", "butter", "creamy",
        "香料", "spice",
        "烟草", "tobacco",
        "木质", "wood",
        "威士忌", "whiskey",
        "荔枝", "lychee",
        "桃子", "peach",
        "苹果", "apple",
        "芒果", "mango",
        "百香果", "passion fruit",
        "草莓", "strawberry"
    )

    // Roaster keywords
    private val roasterPatterns = listOf(
        "烘焙商", "烘焙", "品牌", "Brand", "roaster", "Roaster", "ROASTER"
    )

    // Origin patterns - labels and common coffee origins
    private val originLabels = listOf("产地", "产区", "国家", "Origin", "origin", "Country")
    private val commonOrigins = listOf(
        "埃塞俄比亚", "Ethiopia", "哥伦比亚", "Colombia", "巴西", "Brazil",
        "肯尼亚", "Kenya", "哥斯达黎加", "Costa Rica", "巴拿马", "Panama",
        "危地马拉", "Guatemala", "印尼", "Indonesia", "云南", "Yunnan",
        "卢旺达", "Rwanda", "秘鲁", "Peru", "洪都拉斯", "Honduras"
    )

    // Process keywords
    private val processKeywords = listOf(
        "水洗", "日晒", "蜜处理", "厌氧", "酒桶发酵",
        "washed", "natural", "honey", "anaerobic", "Washed", "Natural"
    )

    // Roast level keywords
    private val roastLevels = listOf(
        "浅烘", "中烘", "深烘", "浅中烘", "中深烘",
        "light", "medium", "dark", "Light", "Medium", "Dark"
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

    private fun parseText(text: String): OCRResult {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        var roaster = ""
        var name = ""
        var origin = ""
        var variety = ""
        var process = ""
        var roastLevel = ""
        val flavors = mutableListOf<String>()

        // Helper: extract value after colon
        fun extractAfterColon(line: String): String {
            val parts = line.split("[:：]".toRegex(), limit = 2)
            return if (parts.size >= 2) parts[1].trim() else ""
        }

        // Helper: check if line contains a label
        fun hasLabel(line: String, labels: List<String>): Boolean =
            labels.any { line.contains(it, ignoreCase = true) }

        // Parse by common coffee label patterns
        for (line in lines) {
            val lower = line.lowercase()

            // Roaster
            if (roaster.isEmpty() && hasLabel(line, roasterPatterns)) {
                roaster = extractAfterColon(line)
            }

            // Origin
            if (origin.isEmpty()) {
                if (hasLabel(line, originLabels)) {
                    origin = extractAfterColon(line)
                }
            }
            // Also check for origin keywords directly in text
            for (o in commonOrigins) {
                if (line.contains(o, ignoreCase = true) && !hasLabel(line, roasterPatterns)) {
                    if (origin.isEmpty()) {
                        origin = extractAfterColon(line).ifEmpty { line.take(20) }
                    }
                }
            }

            // Variety
            if (variety.isEmpty()) {
                if (line.contains("品种", ignoreCase = true) || line.contains("variety", ignoreCase = true)) {
                    variety = extractAfterColon(line)
                }
            }

            // Process - look for "处理" or "处理法" labels, or direct process keywords
            if (process.isEmpty()) {
                if (line.contains("处理", ignoreCase = true) || line.contains("process", ignoreCase = true)) {
                    process = extractAfterColon(line)
                }
                if (process.isEmpty()) {
                    for (kw in processKeywords) {
                        if (line.contains(kw, ignoreCase = true)) {
                            process = kw
                            break
                        }
                    }
                }
            }

            // Roast level - look for "烘焙度" or direct keywords
            if (roastLevel.isEmpty()) {
                if (line.contains("烘焙度", ignoreCase = true) || line.contains("Roast", ignoreCase = true)) {
                    roastLevel = extractAfterColon(line)
                }
                if (roastLevel.isEmpty()) {
                    for (rl in roastLevels) {
                        if (line.contains(rl, ignoreCase = true)) {
                            roastLevel = rl
                            break
                        }
                    }
                }
                // Also check for "浅度烘焙" etc.
                if (roastLevel.isEmpty()) {
                    for (rl in listOf("浅度烘焙", "中度烘焙", "中深度烘焙", "深度烘焙", 
                        "浅度", "中度", "深度", "Light Roast", "Medium Roast", "Dark Roast")) {
                        if (line.contains(rl, ignoreCase = true)) {
                            roastLevel = rl.replace("度烘焙", "烘").replace(" Roast", "")
                            break
                        }
                    }
                }
            }

            // Coffee name - look for lines containing "咖啡" or "coffee", or "豆名"
            if (name.isEmpty()) {
                if (line.contains("咖啡", ignoreCase = true) || line.contains("coffee", ignoreCase = true)
                    || line.contains("豆名", ignoreCase = true) || line.contains("品名", ignoreCase = true)) {
                    name = extractAfterColon(line).ifEmpty { line }
                }
            }

            // Flavors - look for "风味" label or direct flavor keywords
            if (line.contains("风味", ignoreCase = true) || line.contains("flavor", ignoreCase = true)) {
                val flavorText = extractAfterColon(line)
                if (flavorText.isNotEmpty()) {
                    // Split by common separators
                    flavorText.split("[,，、\\s]+".toRegex()).forEach { word ->
                        val w = word.trim().take(10)
                        if (w.isNotEmpty() && !flavors.contains(w)) flavors.add(w)
                    }
                }
            } else {
                for (kw in flavorKeywords) {
                    if (line.contains(kw, ignoreCase = true)) {
                        val word = kw.trim().take(10)
                        if (!flavors.contains(word)) flavors.add(word)
                    }
                }
            }
        }

        // Fallback: if no name found, use the first short line that's not a label
        if (name.isEmpty() && lines.isNotEmpty()) {
            for (line in lines) {
                if (line.length in 3..40 && 
                    !line.contains("http") &&
                    !hasLabel(line, roasterPatterns + originLabels + listOf("处理", "烘焙", "品种", "风味", "咖啡"))
                ) {
                    name = line
                    break
                }
            }
        }

        return OCRResult(
            roaster = roaster, name = name, origin = origin, variety = variety,
            process = process, roastLevel = roastLevel,
            flavors = flavors.distinct(), fullText = text
        )
    }
}
