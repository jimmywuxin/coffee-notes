package com.coffeelab.coffeenotes.util.engine

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * MiniMax VLM 视觉识别引擎
 *
 * 调用 api.minimaxi.com 的视觉识别能力，
 * 将咖啡豆包装照片转为结构化信息。
 */
class AiRecognitionEngine : RecognitionEngine {

    private val gson = Gson()

    companion object {
        // MiniMax VLM API（MiniMax Token Plan）
        private const val API_URL = "https://token-plan-cn.xiaomimomo.com/v1/coding_plan/vlm"

        // API Key（从应用设置中读取，或通过 BuildConfig 注入）
        private var apiKey: String? = null

        // OkHttpClient 单例
        private val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()
        }

        fun setApiKey(key: String?) {
            apiKey = key
        }

        // AI 识别提示词
        private val SYSTEM_PROMPT = """
你是一个咖啡豆信息提取工具。根据用户提供的咖啡豆包装照片，
提取以下信息并以 JSON 格式返回。

# 输出格式（只输出 JSON，不要 markdown、解释、代码块）：
{
  "roaster": "烘焙商/品牌名",
  "name": "豆名/产品名",
  "origin": "产地国家或地区",
  "estate": "庄园/农场/合作社",
  "variety": "品种（如瑰夏、SL28、74158等）",
  "process": "处理法（如日晒、水洗、蜜处理、厌氧等）",
  "roastLevel": "烘焙度（浅烘、中烘、深烘等）",
  "roastDate": "烘焙日期（如有，格式YYYY/MM/DD，没有则留空）",
  "flavors": ["风味1", "风味2", ...],
  "notes": "其他备注信息",
  "dose": "粉量（克数，数字，没有则留空）",
  "brewRatio": "粉水比（如 1:15、1:16 等，没有则留空）",
  "waterAmount": "注水量（毫升，数字，没有则留空）",
  "brewTime": "萃取时间（秒数，数字，没有则留空）",
  "waterTemp": "水温（摄氏度，数字，没有则留空）"
}

# 规则：
1. 只提取图片中明确可见或可合理推断的信息
2. 中文翻译优先（产地用中文译名，如 Ethiopia→埃塞俄比亚）
3. 处理法统一用中文：washed→水洗, natural→日晒, honey→蜜处理, anaerobic→厌氧
4. 烘焙度统一：浅度烘焙→浅烘, 中度烘焙→中烘, 深度烘焙→深烘
5. 风味词保留原始语言，但优先中文
6. 无法确定的信息留空字符串 ""
7. flavors 必须是字符串数组，风味词之间用常见分隔符（逗号、顿号、空格）分开
8. 如果包装上有多个品种，用 / 分隔，如 "SL28/SL34"
9. dose、waterAmount、brewTime、waterTemp 必须是数字（整数或小数），没有则留空

# 常见咖啡产地参考：
埃塞俄比亚(Ethiopia)、哥伦比亚(Colombia)、巴西(Brazil)、肯尼亚(Kenya)、
巴拿马(Panama)、哥斯达黎加(Costa Rica)、危地马拉(Guatemala)、
印尼(Indonesia)、卢旺达(Rwanda)、秘鲁(Peru)、中国云南(Yunnan)
""".trimIndent()
    }

    override suspend fun recognize(bitmap: Bitmap): RecognitionResult {
        return withContext(Dispatchers.IO) {
            try {
                val base64 = bitmapToBase64(bitmap)
                val requestBody = buildRequestBody(base64)
                val response = callApi(requestBody)
                parseResponse(response)
            } catch (e: Exception) {
                RecognitionResult(
                    success = false,
                    rawResponse = "识别失败: ${e.message}",
                    engineName = "MiniMax VLM"
                )
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to reasonable size for API
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun buildRequestBody(base64Image: String): String {
        val body = mapOf(
            "prompt" to SYSTEM_PROMPT,
            "image_url" to "data:image/jpeg;base64,$base64Image"
        )
        return gson.toJson(body)
    }

    private fun callApi(jsonBody: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(API_URL)
            .post(body)

        // Add API Key if available
        apiKey?.let { key ->
            requestBuilder.addHeader("Authorization", "Bearer $key")
        }

        val request = requestBuilder.build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: "{}"
    }

    private fun parseResponse(jsonResponse: String): RecognitionResult {
        return try {
            val root = gson.fromJson(jsonResponse, Map::class.java)
            // MiniMax VLM 返回格式：{"content": "..."}
            val content = root["content"] as? String ?: ""

            // Try to extract JSON from the response
            val jsonContent = extractJson(content)
            val result = gson.fromJson(jsonContent, BeanAIResult::class.java)

            RecognitionResult(
                roaster = result.roaster ?: "",
                name = result.name ?: "",
                origin = result.origin ?: "",
                estate = result.estate ?: "",
                variety = result.variety ?: "",
                process = result.process ?: "",
                roastLevel = result.roastLevel ?: "",
                roastDate = result.roastDate ?: "",
                flavors = result.flavors ?: emptyList(),
                notes = result.notes ?: "",
                dose = parseNumber(result.dose),
                brewRatio = result.brewRatio ?: "",
                waterAmount = parseNumber(result.waterAmount),
                brewTime = parseIntNumber(result.brewTime),
                waterTemp = parseIntNumber(result.waterTemp),
                rawResponse = content,
                success = true,
                engineName = "MiniMax VLM"
            )
        } catch (e: Exception) {
            RecognitionResult(
                success = false,
                rawResponse = "解析失败: ${e.message}",
                engineName = "MiniMax VLM"
            )
        }
    }

    /** 从 LLM 回复中提取 JSON 对象 */
    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            text.substring(start, end + 1)
        } else {
            "{}"
        }
    }

    /** 解析数字类型（可能是 Int/Float/Double/String），统一转为 Float? */
    private fun parseNumber(value: Any?): Float? = when (value) {
        is Number -> value.toFloat()
        is String -> value.toFloatOrNull()
        else -> null
    }

    /** 解析整数类型（可能是 Int/Float/Double/String），统一转为 Int? */
    private fun parseIntNumber(value: Any?): Int? = when (value) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }
}

/** MiMo Omni 返回的 JSON 映射 */
private data class BeanAIResult(
    val roaster: String? = null,
    val name: String? = null,
    val origin: String? = null,
    val estate: String? = null,
    val variety: String? = null,
    val process: String? = null,
    val roastLevel: String? = null,
    val roastDate: String? = null,
    val flavors: List<String>? = null,
    val notes: String? = null,
    val dose: Any? = null,
    val brewRatio: String? = null,
    val waterAmount: Any? = null,
    val brewTime: Any? = null,
    val waterTemp: Any? = null
)
