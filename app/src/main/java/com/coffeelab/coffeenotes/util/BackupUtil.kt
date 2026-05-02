package com.coffeelab.coffeenotes.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object BackupUtil {

    private const val DB_NAME = "coffee_notes.db"
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)

    /** 导出数据库到用户选择的目录 */
    fun exportDb(context: Context): File {
        val dbFile = context.getDatabasePath(DB_NAME)
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val fileName = "CoffeeNotes_Backup_${dateFormat.format(Date())}.db"
        val backupFile = File(backupDir, fileName)

        dbFile.inputStream().use { input ->
            backupFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return backupFile
    }

    /** 从 URI 导入数据库（会覆盖当前数据库） */
    fun importDb(context: Context, uri: Uri): Boolean {
        return try {
            val dbFile = context.getDatabasePath(DB_NAME)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            // Force close database connections
            context.deleteDatabase("${DB_NAME}_wal")
            context.deleteDatabase("${DB_NAME}_shm")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** 获取备份文件列表 */
    fun getBackupFiles(context: Context): List<File> {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles()?.filter { it.name.endsWith(".db") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /** 删除备份文件 */
    fun deleteBackup(file: File): Boolean = file.delete()
}
