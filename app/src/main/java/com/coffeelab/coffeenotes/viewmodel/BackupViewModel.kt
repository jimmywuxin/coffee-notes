package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.*
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import com.coffeelab.coffeenotes.util.BackupBuilder
import com.coffeelab.coffeenotes.util.CloudBackupPrefs
import com.coffeelab.coffeenotes.util.WebDavClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))

    /** 恢复逻辑已拆出（原类 706 行里有 480 行是这一个方法） */
    private val restorer by lazy { BackupRestorer(application) }
    companion object {
        // 备份格式常量统一由 BackupBuilder 持有，避免升级 BACKUP_VERSION 时漏改一处导致解包失败。
        const val BACKUP_VERSION = BackupBuilder.BACKUP_VERSION
    }

    sealed class BackupState {
        object Idle : BackupState()
        object Loading : BackupState()
        data class Success(
            val title: String,
            val message: String,
            val exportDate: String,
            val version: String,
            val beansCount: Int,
            val recordsCount: Int
        ) : BackupState()
        data class Error(val message: String) : BackupState()
    }

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    fun resetState() {
        _backupState.value = BackupState.Idle
    }

    // ===================== 云端备份（WebDAV） =====================

    private val _cloudState = MutableStateFlow<CloudState>(CloudState.Idle)
    val cloudState: StateFlow<CloudState> = _cloudState.asStateFlow()

    sealed class CloudState {
        object Idle : CloudState()
        object Loading : CloudState()
        data class Success(val message: String) : CloudState()
        data class Error(val message: String) : CloudState()
        data class Files(val files: List<String>) : CloudState()
    }

    fun resetCloudState() {
        _cloudState.value = CloudState.Idle
    }

    private fun requireCloudClient(context: Context): WebDavClient? {
        val config = CloudBackupPrefs.getConfig(context) ?: run {
            _cloudState.value = CloudState.Error("请先在下方填写云端配置（地址/账号/应用密码）")
            return null
        }
        return WebDavClient(config.baseUrl, config.username, config.password)
    }

    fun testCloudConnection(context: Context) {
        viewModelScope.launch {
            _cloudState.value = CloudState.Loading
            val client = requireCloudClient(context) ?: return@launch
            val ok = client.testConnection()
            _cloudState.value = if (ok) CloudState.Success("连接成功，可以开始备份")
            else CloudState.Error("连接失败：请检查地址、账号和应用密码")
        }
    }

    fun uploadToCloud(context: Context) {
        viewModelScope.launch {
            _cloudState.value = CloudState.Loading
            try {
                val client = requireCloudClient(context) ?: return@launch
                val (bytes, counts) = BackupBuilder.buildZipBytes(context, repository)
                val fileName = WebDavClient.buildFileName()
                val dirReady = client.ensureDirectory(WebDavClient.CLOUD_DIR)
                if (!dirReady) {
                    _cloudState.value = CloudState.Error("无法创建云端目录，请检查 WebDAV 写权限")
                    return@launch
                }
                val uploaded = client.upload("${WebDavClient.CLOUD_DIR}/$fileName", bytes)
                if (!uploaded) {
                    _cloudState.value = CloudState.Error("上传失败：请检查网络或云端配置")
                    return@launch
                }
                cleanupOldBackups(client)
                _cloudState.value = CloudState.Success(
                    "已上传 $fileName\n豆子 ${counts.beans} 个 / 记录 ${counts.records} 条"
                )
            } catch (e: Exception) {
                _cloudState.value = CloudState.Error("上传失败: ${e.message}")
            }
        }
    }

    private suspend fun cleanupOldBackups(client: WebDavClient) {
        runCatching { client.pruneOldBackups() }
    }
    fun listCloudFiles(context: Context) {
        viewModelScope.launch {
            _cloudState.value = CloudState.Loading
            val client = requireCloudClient(context) ?: return@launch
            val files = client.list(WebDavClient.CLOUD_DIR)
                .filter { it.endsWith(".zip") }
                .sortedDescending()
            _cloudState.value = CloudState.Files(files)
        }
    }

    fun deleteCloudFile(context: Context, fileName: String) {
        viewModelScope.launch {
            val client = requireCloudClient(context) ?: return@launch
            client.delete("${WebDavClient.CLOUD_DIR}/$fileName")
            listCloudFiles(context)
        }
    }

    /** 从云端下载备份并恢复（覆盖当前数据，调用方需先确认）。 */
    fun restoreFromCloud(context: Context, fileName: String) {
        viewModelScope.launch {
            _cloudState.value = CloudState.Loading
            try {
                val client = requireCloudClient(context) ?: return@launch
                val bytes = client.download("${WebDavClient.CLOUD_DIR}/$fileName")
                if (bytes == null) {
                    _cloudState.value = CloudState.Error("下载失败，请检查网络后重试")
                    return@launch
                }
                // 必须按importBackupBytes 的真实结果报状态：它校验失败时只返回 Error，
                // 曾经在这里无条件写 Success，用户看到「恢复成功」实际零条数据落库。
                when (val r = restorer.restore(bytes)) {
                    is BackupState.Success -> _cloudState.value = CloudState.Success("已从云端恢复 $fileName")
                    is BackupState.Error -> _cloudState.value = CloudState.Error(r.message)
                    else -> _cloudState.value = CloudState.Error("恢复失败")
                }
            } catch (e: Exception) {
                _cloudState.value = CloudState.Error("恢复失败: ${e.message}")
            }
        }
    }

    suspend fun exportBackup(context: Context, uri: Uri) {
        _backupState.value = BackupState.Loading

        try {
            withContext(Dispatchers.IO) {
                val (bytes, counts) = BackupBuilder.buildZipBytes(context, repository)
                val os = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("无法写入备份文件")
                os.use { it.write(bytes) }

                withContext(Dispatchers.Main) {
                    _backupState.value = BackupState.Success(
                        title = "备份成功",
                        message = "豆子 ${counts.beans} 个 · 冲煮记录 ${counts.records} 条 · 手法 ${counts.methods} 个 · 器具 ${counts.equipment} 个 · 磨豆机 ${counts.grinders} 个",
                        exportDate = "刚刚",
                        version = BACKUP_VERSION,
                        beansCount = counts.beans,
                        recordsCount = counts.records
                    )
                }
            }
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("备份失败: ${e.message}")
        }
    }

    suspend fun importBackup(context: Context, uri: Uri) {
        _backupState.value = BackupState.Loading

        try {
            // Read all bytes first to detect format
            val allBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IOException("无法打开文件")
            _backupState.value = restorer.restore(allBytes)
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("恢复失败: ${e.message}")
        }
    }

}
