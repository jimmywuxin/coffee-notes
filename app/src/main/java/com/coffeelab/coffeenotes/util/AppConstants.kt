package com.coffeelab.coffeenotes.util

/**
 * 跨文件重复出现的魔法数字集中于此（审计 #4.7）。
 * 只收「同一含义在多个文件里各写一遍」的常量，一次性阈值留在原处更易读。
 */
object AppConstants {

    /** 一天的毫秒数。周/月区间计算用（原各处写 `7 * 24 * 60 * 60 * 1000L`） */
    const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    /** 搜索输入防抖时长（毫秒）。豆子/冲煮记录/首页搜索共用 */
    const val SEARCH_DEBOUNCE_MS = 200L

    /** 图片模糊判定阈值：模糊分低于此值提示重拍（BlurDetector 输出） */
    const val BLUR_WARNING_THRESHOLD = 60f

    /** OCR 前 Bitmap 预采样最长边（像素）。高分辨率有利于小字识别 */
    const val OCR_MAX_DIM = 1600
}
