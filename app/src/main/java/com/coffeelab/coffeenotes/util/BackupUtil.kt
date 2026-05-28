package com.coffeelab.coffeenotes.util

import android.content.Context
import android.net.Uri
import com.coffeelab.coffeenotes.data.AppDatabase
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object BackupUtil {

    private const val DB_NAME = "coffee_notes.db"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    /** 导出数据库到用户选择的目录 */
    fun exportDb(context: Context): File {
        val dbFile = context.getDatabasePath(DB_NAME)
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val fileName = "CoffeeNotes_Backup_${dateFormatter.format(LocalDateTime.now())}.db"
        val backupFile = File(backupDir, fileName)

        dbFile.inputStream().use { input ->
            backupFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return backupFile
    }

    /** 从 URI 导入数据库。先写入临时文件校验 SQLite 头，确认合法后再覆盖当前数据库 */
    fun importDb(context: Context, uri: Uri): Boolean {
        return try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val tmpFile = File(dbFile.parent, "${DB_NAME}.import_tmp")

            context.contentResolver.openInputStream(uri)?.use { input ->
                tmpFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return false

            // Validate: check SQLite header magic bytes ("SQLite format 3\0")
            if (!isValidSqliteFile(tmpFile)) {
                tmpFile.delete()
                return false
            }

            // Close Room database connection and reset singleton before replacing file
            AppDatabase.closeAndReset()

            // Remove WAL/SHM files from previous connection
            context.deleteDatabase("${DB_NAME}_wal")
            context.deleteDatabase("${DB_NAME}_shm")
            tmpFile.renameTo(dbFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** 检查文件是否以 SQLite3 魔数开头 */
    private fun isValidSqliteFile(file: File): Boolean {
        return try {
            val header = ByteArray(16)
            file.inputStream().use { it.read(header) }
            String(header, 0, 16, Charsets.US_ASCII).startsWith("SQLite format 3")
        } catch (e: Exception) { false }
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
