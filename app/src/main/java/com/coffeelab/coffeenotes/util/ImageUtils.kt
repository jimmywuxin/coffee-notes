package com.coffeelab.coffeenotes.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {

    private const val IMAGE_DIR = "coffee_images"

    fun getImageDir(context: Context): File {
        val dir = File(context.filesDir, IMAGE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
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
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.CHINA)
    private val dateTimeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA)

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))
    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    // 获取某天所在周的周一（以周一为一周开始）
    fun getWeekStart(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // 周一 = 2（在 Calendar 中，周日=1，周一=2，...）
        val daysToMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
        cal.add(Calendar.DAY_OF_MONTH, -daysToMonday)
        return cal.timeInMillis
    }

    fun getWeekLabel(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val weekStart = getWeekStart(now)
        val thisWeekStart = getWeekStart(weekStart)
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
        val weekStart = getWeekStart(now)
        val thisWeekStart = getWeekStart(weekStart)
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
