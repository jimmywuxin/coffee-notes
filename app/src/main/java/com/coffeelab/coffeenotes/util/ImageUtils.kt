package com.coffeelab.coffeenotes.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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
}
