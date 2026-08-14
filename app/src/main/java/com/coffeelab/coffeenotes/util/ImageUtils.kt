package com.coffeelab.coffeenotes.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

object ImageUtils {

    private const val IMAGE_DIR = "coffee_images"
    private const val BEAN_PHOTOS_DIR = "bean_photos"
    private const val COMPRESS_MAX_DIM = 1920
    private const val COMPRESS_QUALITY = 80

    fun getImageDir(context: Context): File {
        val dir = File(context.filesDir, IMAGE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getBeanPhotosDir(context: Context): File {
        val dir = File(context.filesDir, BEAN_PHOTOS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Compress and save a bean photo from a Uri.
     * Returns the relative path (uuid.jpg) on success, null on failure.
     */
    fun compressAndSaveBeanPhoto(context: Context, uri: Uri): String? {
        return try {
            // 1. 先 inJustDecodeBounds 只读尺寸（不加载像素），算采样率防大图 OOM
            val boundsOptions = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { ins ->
                android.graphics.BitmapFactory.decodeStream(ins, null, boundsOptions)
            }
            val boundsWidth = boundsOptions.outWidth
            val boundsHeight = boundsOptions.outHeight
            if (boundsWidth <= 0 || boundsHeight <= 0) return null
            val sampleSize = calculateInSampleSize(boundsWidth, boundsHeight, COMPRESS_MAX_DIM)

            // 2. 按采样率解码（内存占用约降 sampleSize² 倍）
            val decodeOptions = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream.close()
            if (originalBitmap == null) return null

            val (width, height) = originalBitmap.width to originalBitmap.height
            val scale = if (width > height) {
                if (width > COMPRESS_MAX_DIM) COMPRESS_MAX_DIM.toFloat() / width else 1f
            } else {
                if (height > COMPRESS_MAX_DIM) COMPRESS_MAX_DIM.toFloat() / height else 1f
            }
            val bitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(originalBitmap, (width * scale).toInt(), (height * scale).toInt(), true).also { originalBitmap.recycle() }
            } else {
                originalBitmap
            }

            val uuid = UUID.randomUUID().toString()
            val fileName = "$uuid.jpg"
            val dir = getBeanPhotosDir(context)
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, out)
            }
            bitmap.recycle()
            fileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 计算 inSampleSize（2 的幂）：使采样后最长边不超过 maxDim 的 2 倍（由调用方再精确缩放）。
     */
    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= maxDim || height / (sampleSize * 2) >= maxDim) {
            sampleSize *= 2
        }
        return sampleSize
    }

    fun getBeanPhotoFile(context: Context, relativePath: String): File {
        return File(getBeanPhotosDir(context), relativePath)
    }

    fun deleteBeanPhoto(relativePath: String, context: Context) {
        val file = getBeanPhotoFile(context, relativePath)
        if (file.exists()) file.delete()
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap): String {
        val dir = getImageDir(context)
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    fun getFileForUri(context: Context, uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val dir = getImageDir(context)
        val fileName = "IMG_${System.currentTimeMillis()}_gallery.jpg"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            inputStream.copyTo(out)
        }
        inputStream.close()
        return file
    }
}

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    private val zoneId = ZoneId.of("Asia/Shanghai")

    fun formatDate(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        return dateFormatter.format(instant.atZone(zoneId).toLocalDate())
    }

    fun formatDateTime(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        return dateTimeFormatter.format(instant.atZone(zoneId).toLocalDateTime())
    }

    fun parseDate(dateStr: String): Long? {
        return try {
            val localDate = LocalDate.parse(dateStr, dateFormatter)
            localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        } catch (e: Exception) { null }
    }

    // 获取某天所在周的周一（以周一为一周开始）
    fun getWeekStart(timestamp: Long): Long {
        val instant = Instant.ofEpochMilli(timestamp)
        val localDate = instant.atZone(zoneId).toLocalDate()
        val dayOfWeek = localDate.dayOfWeek.value // 1=Mon, 7=Sun
        val monday = localDate.minusDays((dayOfWeek - 1).toLong())
        return monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun getWeekLabel(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val thisWeekStart = getWeekStart(now)
        val lastWeekStart = thisWeekStart - 7 * 24 * 60 * 60 * 1000L
        val twoWeeksAgoStart = lastWeekStart - 7 * 24 * 60 * 60 * 1000L

        val ts = getWeekStart(timestamp)
        return when {
            ts >= thisWeekStart -> "本周"
            ts >= lastWeekStart -> "上周"
            ts >= twoWeeksAgoStart -> "前周"
            else -> "更早"
        }
    }

    fun filterByWeekRange(records: List<BrewRecord>, range: String): List<BrewRecord> {
        val now = System.currentTimeMillis()
        val thisWeekStart = getWeekStart(now)
        val lastWeekStart = thisWeekStart - 7 * 24 * 60 * 60 * 1000L
        val twoWeeksAgoStart = lastWeekStart - 7 * 24 * 60 * 60 * 1000L

        return when (range) {
            "本周" -> records.filter { it.dateTime >= thisWeekStart }
            "上周" -> records.filter { it.dateTime >= lastWeekStart && it.dateTime < thisWeekStart }
            "两周前" -> records.filter { it.dateTime >= twoWeeksAgoStart && it.dateTime < lastWeekStart }
            "更早" -> records.filter { it.dateTime < twoWeeksAgoStart }
            else -> records // "全部"
        }
    }
}
