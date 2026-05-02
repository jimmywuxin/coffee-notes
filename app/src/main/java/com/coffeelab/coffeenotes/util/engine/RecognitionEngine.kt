package com.coffeelab.coffeenotes.util.engine

import android.graphics.Bitmap

/**
 * 可插拔识别引擎接口
 * 
 * 支持多种实现：
 * - MiMoRecognition: 免费云端AI识别（小米 MiMo Omni）
 * - KeywordRecognition: 纯本地关键词匹配（ML Kit OCR + 规则）
 * - 未来可扩展其他免费模型
 */
interface RecognitionEngine {
    /**
     * 从图片识别咖啡豆信息
     * @return 识别结果，包含烘焙商、豆名、产地、处理法等
     */
    suspend fun recognize(bitmap: Bitmap): RecognitionResult
}

/**
 * 识别结果
 */
data class RecognitionResult(
    val roaster: String = "",
    val name: String = "",
    val origin: String = "",
    val estate: String = "",
    val variety: String = "",
    val process: String = "",
    val roastLevel: String = "",
    val roastDate: String = "",
    val flavors: List<String> = emptyList(),
    val notes: String = "",
    val rawResponse: String = "",
    val success: Boolean = true,
    val engineName: String = ""
)
