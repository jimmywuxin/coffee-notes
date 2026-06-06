package com.coffeelab.coffeenotes.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BitmapLoader {

    suspend fun loadFromUri(context: Context, uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                // First pass: get dimensions
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                // Calculate sample size to limit to ~1600px (higher res improves small-text OCR)
                options.inSampleSize = calculateInSampleSize(options, 1600, 1600)
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.ARGB_8888

                // Second pass: decode actual bitmap
                val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /** 预处理图片以提升 OCR 识别率。
     *  1. 灰度 + 对比度增强
     *  2. 3x3 锐化卷积核（补偿缩放导致的边缘模糊）
     *  返回新的 Bitmap，不回收 source。 */
    fun enhanceForOcr(source: Bitmap): Bitmap {
        val w = source.width; val h = source.height

        // Step 1: grayscale with enhanced contrast
        val base = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(base).apply {
            val paint = Paint()
            val colorMatrix = ColorMatrix().apply {
                setSaturation(0f)
                val contrast = 1.4f
                val translate = (-.5f * contrast + .5f) * 255f
                postConcat(ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            drawBitmap(source, 0f, 0f, paint)
        }

        // Step 2: 3x3 sharpen kernel (edge enhancement)
        return applySharpen(base)
    }

    /** 3x3 锐化核卷积，补偿缩放和压缩导致的边缘模糊。 */
    private fun applySharpen(bitmap: Bitmap): Bitmap {
        val w = bitmap.width; val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val ker = floatArrayOf(0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f)
        val dst = IntArray(w * h)
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            var r = 0f; var g = 0f; var b = 0f; var ki = 0
            for (ky in -1..1) for (kx in -1..1) {
                val p = src[(y + ky) * w + (x + kx)]
                val kr = ker[ki++]
                r += ((p shr 16) and 0xFF) * kr
                g += ((p shr 8) and 0xFF) * kr
                b += (p and 0xFF) * kr
            }
            dst[y * w + x] = (0xFF shl 24) or
                (r.coerceIn(0f, 255f).toInt() shl 16) or
                (g.coerceIn(0f, 255f).toInt() shl 8) or
                b.coerceIn(0f, 255f).toInt()
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        if (result !== bitmap) bitmap.recycle()
        return result
    }

    /**
     * Otsu 自适应二值化。仅推荐在图像前景/背景对比度明显时使用。
     * 对光照不均或渐变背景的包装袋照片可能不如灰度+对比度增强。
     */
    fun applyOtsuBinarization(source: Bitmap): Bitmap {
        val w = source.width; val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val gray = IntArray(w * h) { i ->
            val p = pixels[i]
            ((0.299 * (p shr 16 and 0xFF) + 0.587 * (p shr 8 and 0xFF) + 0.114 * (p and 0xFF)).toInt())
        }
        val t = otsuThreshold(gray)
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val dst = IntArray(w * h) { i -> if (gray[i] > t) 0xFFFFFFFF.toInt() else 0xFF000000.toInt() }
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * Gamma 校正。gamma < 1 提亮（如 0.8 适合暗光照片），gamma > 1 压暗。
     * 公式：out = (in / 255) ^ gamma * 255
     * 返回新 Bitmap，不回收 source。
     */
    fun applyGammaCorrection(source: Bitmap, gamma: Float): Bitmap {
        require(gamma > 0f) { "gamma must be positive, got $gamma" }
        val w = source.width; val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val lut = IntArray(256) { v ->
            (Math.pow(v / 255.0, gamma.toDouble()) * 255.0).toInt().coerceIn(0, 255)
        }
        val dst = IntArray(w * h) { i ->
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            val r = lut[(p shr 16) and 0xFF]
            val g = lut[(p shr 8) and 0xFF]
            val b = lut[p and 0xFF]
            (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    private fun otsuThreshold(gray: IntArray): Int {
        val hist = IntArray(256)
        for (v in gray) if (v in 0..255) hist[v]++
        val total = gray.size; var sum = 0.0
        for (i in 0 until 256) sum += i * hist[i]
        var sumB = 0.0; var wB = 0; var maxV = 0.0; var threshold = 0
        for (i in 0 until 256) {
            wB += hist[i]; if (wB == 0) continue
            val wF = total - wB; if (wF == 0) break
            sumB += i * hist[i]
            val mB = sumB / wB; val mF = (sum - sumB) / wF
            val between = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            if (between > maxV) { maxV = between; threshold = i }
        }
        return threshold
    }

    /**
     * 检测图像模糊程度。返回拉普拉斯算子（Laplacian）的方差值。
     * 经验阈值（针对 1600px 分辨率图像）：
     *   < 60   → 很模糊（建议拒绝）
     *   60-120 → 略模糊（可识别但准确率下降）
     *   > 120  → 清晰
     */
    fun detectBlur(bitmap: Bitmap): Float {
        val w = bitmap.width; val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val lap = floatArrayOf(0f, 1f, 0f, 1f, -4f, 1f, 0f, 1f, 0f)
        var sum = 0.0; var sumSq = 0.0; var count = 0
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            var value = 0f; var ki = 0
            for (ky in -1..1) for (kx in -1..1) {
                val p = pixels[(y + ky) * w + (x + kx)]
                val gray = 0.299f * (p shr 16 and 0xFF) + 0.587f * (p shr 8 and 0xFF) + 0.114f * (p and 0xFF)
                value += gray * lap[ki++]
            }
            sum += value; sumSq += value * value; count++
        }
        val mean = sum / count
        return ((sumSq / count) - (mean * mean)).toFloat()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
