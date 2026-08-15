package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.*
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import com.coffeelab.coffeenotes.util.BackupBuilder
import com.coffeelab.coffeenotes.util.CloudBackupPrefs
import com.coffeelab.coffeenotes.util.ImageUtils
import com.coffeelab.coffeenotes.util.WebDavClient
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        const val MANIFEST_FILE = "manifest.json"
        const val DATA_FILE = "data.json"
        const val BEAN_PHOTOS_DIR = "images/bean_photos"
        const val BACKUP_VERSION = "1.2.0"
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
        val files = client.list(WebDavClient.CLOUD_DIR)
            .filter { it.startsWith("CoffeeNotes_") && it.endsWith(".zip") }
            .sortedDescending()
        files.drop(WebDavClient.MAX_KEEP).forEach { fileName ->
            client.delete("${WebDavClient.CLOUD_DIR}/$fileName")
        }
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
                importBackupBytes(context, bytes)
                _cloudState.value = CloudState.Success("已从云端恢复 $fileName")
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
            importBackupBytes(context, allBytes)
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("恢复失败: ${e.message}")
        }
    }

    /**
     * 从备份字节流恢复数据（本地文件恢复与云端下载恢复共用）。
     * 内部逻辑沿用原 importBackup 实现；异常由调用方 catch。
     */
    private suspend fun importBackupBytes(context: Context, allBytes: ByteArray) {
        withContext(Dispatchers.IO) {
            var manifestContent: String? = null
                var dataContent: String? = null
                var exportDate = "unknown"
                var manifestVersion = "unknown"
                val photoFiles = mutableListOf<Pair<String, ByteArray>>()

                // Try to detect if this is a ZIP or legacy JSON
                val isZip = allBytes.size >= 4 &&
                    allBytes[0] == 0x50.toByte() && // P
                    allBytes[1] == 0x4B.toByte() && // K
                    allBytes[2] == 0x03.toByte() && // ZIP signature start
                    allBytes[3] == 0x04.toByte()

                if (isZip) {
                    // New ZIP format
                    val zipInputStream = ZipInputStream(ByteArrayInputStream(allBytes))
                    var entry = zipInputStream.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        val content = zipInputStream.readBytes()

                        when {
                            name == MANIFEST_FILE -> manifestContent = String(content, Charsets.UTF_8)
                            name == DATA_FILE -> dataContent = String(content, Charsets.UTF_8)
                            name.startsWith("$BEAN_PHOTOS_DIR/") && name.endsWith(".jpg") -> {
                                val relativePath = name.removePrefix("$BEAN_PHOTOS_DIR/")
                                photoFiles.add(relativePath to content)
                            }
                        }

                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }
                    zipInputStream.close()

                    // Validate manifest
                    if (manifestContent == null) {
                        withContext(Dispatchers.Main) {
                            _backupState.value = BackupState.Error("无效备份文件：缺少 manifest.json")
                        }
                        return@withContext
                    }

                    val manifest = gson.fromJson(manifestContent, Map::class.java)
                    manifestVersion = manifest["version"] as? String ?: "unknown"
                    exportDate = manifest["exportDate"] as? String ?: "unknown"

                    if (dataContent == null) {
                        withContext(Dispatchers.Main) {
                            _backupState.value = BackupState.Error("无效备份文件：缺少 data.json")
                        }
                        return@withContext
                    }
                } else {
                    // Legacy JSON format (no ZIP, no manifest)
                    try {
                        val jsonStr = String(allBytes, Charsets.UTF_8)
                        val jsonMap = gson.fromJson(jsonStr, Map::class.java)

                        // Check if it's a legacy backup by looking for top-level keys
                        if (jsonMap.containsKey("beans") || jsonMap.containsKey("records")) {
                            dataContent = jsonStr
                        } else {
                            withContext(Dispatchers.Main) {
                                _backupState.value = BackupState.Error("无效备份文件：格式无法识别")
                            }
                            return@withContext
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            _backupState.value = BackupState.Error("无效备份文件：${e.message}")
                        }
                        return@withContext
                    }
                }

                // Extract bean photos first
                val photosDir = ImageUtils.getBeanPhotosDir(context)
                if (!photosDir.exists()) photosDir.mkdirs()
                photoFiles.forEach { (relativePath, content) ->
                    val file = File(photosDir, relativePath)
                    FileOutputStream(file).use { fos ->
                        fos.write(content)
                    }
                }

                // Parse and import data
                val backup = gson.fromJson(dataContent, Map::class.java)
                val data = backup["data"] as? Map<*, *> ?: backup

                // Clear existing data (full replace)
                val db = AppDatabase.getInstance(context)
                val beanDao = db.coffeeBeanDao()
                val recordDao = db.brewRecordDao()
                val methodDao = db.brewMethodDao()
                val equipmentDao = db.equipmentDao()
                val grinderDao = db.grinderDao()
                val roastDegreeDao = db.roastDegreeDao()
                val processMethodDao = db.processMethodDao()
                val restPeriodConfigDao = db.restPeriodConfigDao()
                val peakFlavorConfigDao = db.peakFlavorConfigDao()
                val purchaseRecordDao = db.purchaseRecordDao()
                val impressionTagDao = db.impressionTagDao()

                db.withTransaction {
                // Delete in FK-safe order: child/assoc tables first, then parent tables
                recordDao.deleteAll()
                purchaseRecordDao.deleteAll()
                repository.deleteAllStockAdjustments()
                beanDao.deleteAll()          // CASCADE deletes flavor_tags, bean_impression_tags
                methodDao.deleteAll()
                equipmentDao.deleteAll()
                grinderDao.deleteAll()
                restPeriodConfigDao.deleteAll()   // FK to roast_degrees
                peakFlavorConfigDao.deleteAll()   // FK to roast_degrees
                roastDegreeDao.deleteAll()
                processMethodDao.deleteAll()
                impressionTagDao.deleteAll()      // independent table

                // Import impression tags (before beans so bean_impression_tags FK resolves)
                val impressionTagIdMap = mutableMapOf<Long, Long>()
                val impressionTagData: List<*> = (data["impressionTags"] ?: emptyList<Any>()) as List<*>
                val existingImps = repository.getAllImpressionTagsOnce().associateBy { it.name }
                for (it in impressionTagData) {
                    val m = it as Map<*, *>
                    val oldId = (m["id"] as? Double)?.toLong() ?: continue
                    val name = m["name"] as? String ?: continue
                    val newId = if (name in existingImps) {
                        existingImps[name]!!.id
                    } else {
                        repository.insertImpressionTag(ImpressionTag(
                            name = name,
                            sortOrder = (m["sortOrder"] as? Double)?.toInt() ?: 0
                        ))
                    }
                    impressionTagIdMap[oldId] = newId
                }

                // Import beans
                val beansList: List<*> = data["beans"] as? List<*> ?: emptyList<Any>()
                val beanIdMap = mutableMapOf<Long, Long>()

                for (beanEntry in beansList) {
                    val entry = beanEntry as Map<*, *>
                    val beanMap = entry["bean"] as Map<*, *>
                    val tagsList: List<*> = entry["tags"] as? List<*> ?: emptyList<Any>()

                    val oldId = (beanMap["id"] as? Double)?.toLong() ?: 0L
                    val newBean = CoffeeBean(
                        roaster = beanMap["roaster"] as? String ?: "",
                        name = beanMap["name"] as? String ?: "",
                        origin = beanMap["origin"] as? String ?: "",
                        region = beanMap["region"] as? String ?: "",
                        estate = beanMap["estate"] as? String ?: "",
                        variety = beanMap["variety"] as? String ?: "",
                        process = beanMap["process"] as? String ?: "",
                        roastLevel = beanMap["roastLevel"] as? String ?: "",
                        grindSize = beanMap["grindSize"] as? String ?: "",
                        roastDate = (beanMap["roastDate"] as? Double)?.toLong(),
                        notes = beanMap["notes"] as? String ?: "",
                        localPhotoPaths = (beanMap["localPhotoPaths"] as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList(),
                        imageUri = beanMap["imageUri"] as? String ?: "",
                        isFavorite = (beanMap["isFavorite"] as? Boolean) ?: false,
                        isArchived = (beanMap["isArchived"] as? Boolean) ?: false,
                        sortOrder = (beanMap["sortOrder"] as? Double)?.toInt() ?: 0,
                        dose = (beanMap["dose"] as? Double)?.toFloat(),
                        brewRatio = beanMap["brewRatio"] as? String,
                        waterAmount = (beanMap["waterAmount"] as? Double)?.toFloat(),
                        brewTime = (beanMap["brewTime"] as? Double)?.toInt(),
                        waterTemp = (beanMap["waterTemp"] as? Double)?.toInt(),
                        pouringDurationSeconds = (beanMap["pouringDurationSeconds"] as? Double)?.toInt(),
                        createdAt = (beanMap["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (beanMap["updatedAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                        restDays = (beanMap["restDays"] as? Double)?.toInt(),
                        peakFlavorDays = (beanMap["peakFlavorDays"] as? Double)?.toInt()
                    )

                    val newId = repository.insertBean(newBean)
                    beanIdMap[oldId] = newId

                    val tagList = tagsList.filterIsInstance<String>()
                    if (tagList.isNotEmpty()) {
                        repository.saveTagsForBean(newId, tagList)
                    }

                    // Import impression tags for this bean
                    val beanImpTagIds: List<*> = entry["impressionTagIds"] as? List<*> ?: emptyList<Any>()
                    if (beanImpTagIds.isNotEmpty()) {
                        repository.saveImpressionTagsForBean(
                            newId,
                            beanImpTagIds.mapNotNull { (it as? Double)?.toLong()?.let { oldId -> impressionTagIdMap[oldId] } }
                        )
                    }
                }

                // Import methods
                val methodsList: List<*> = data["methods"] as? List<*> ?: emptyList<Any>()
                val methodIdMap = mutableMapOf<Long, Long>()

                // 恢复完成后，对所有有 roastDate 但缺 restDays/peakFlavorDays 的豆子，从烘焙度配置自动填充
                val allBeansAfterRestore = beanDao.getAllBeans().first()
                for (bean in allBeansAfterRestore) {
                    if (bean.roastDate != null && (bean.restDays == null || bean.peakFlavorDays == null) && bean.roastLevel.isNotEmpty()) {
                        val roastDegree = roastDegreeDao.getAllOnce().find { it.name == bean.roastLevel }
                        if (roastDegree != null) {
                            val restD = bean.restDays ?: restPeriodConfigDao.getByRoastDegreeId(roastDegree.id)?.restDays
                            val peakD = bean.peakFlavorDays ?: peakFlavorConfigDao.getByRoastDegreeId(roastDegree.id)?.peakFlavorDays
                            beanDao.update(bean.copy(restDays = restD, peakFlavorDays = peakD))
                        }
                    }
                }

                for (methodEntry in methodsList) {
                    val m = methodEntry as Map<*, *>
                    val name = m["name"] as? String ?: continue
                    val oldId = (m["id"] as? Double)?.toLong() ?: 0L
                    val method = BrewMethod(
                        name = name,
                        isPreset = (m["isPreset"] as? Boolean) ?: false,
                        steps = m["steps"] as? String,
                        coffeeWeight = (m["coffeeWeight"] as? Double),
                        coffeeWaterRatio = (m["coffeeWaterRatio"] as? Double),
                        waterTemp = (m["waterTemp"] as? Double)?.toInt(),
                        sortOrder = (m["sortOrder"] as? Double)?.toInt() ?: 0,
                        createdAt = (m["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (m["updatedAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                    )
                    val newId = repository.insertMethod(method)
                    methodIdMap[oldId] = newId
                }

                // Import equipment (build id map, skip duplicate names)
                val equipmentIdMap = mutableMapOf<Long, Long>()
                val equipmentList: List<*> = data["equipment"] as? List<*> ?: emptyList<Any>()
                val existingEq = repository.getAllEquipmentOnce().associateBy { it.name }
                for (eq in equipmentList) {
                    val m = eq as Map<*, *>
                    val oldId = (m["id"] as? Double)?.toLong() ?: continue
                    val name = m["name"] as? String ?: continue
                    val newId = if (name in existingEq) {
                        existingEq[name]!!.id
                    } else {
                        repository.insertEquipment(Equipment(
                            name = name,
                            sortOrder = (m["sortOrder"] as? Double)?.toInt() ?: 0
                        ))
                    }
                    equipmentIdMap[oldId] = newId
                }


                // Import grinders (build id map, skip duplicate names)
                val grinderIdMap = mutableMapOf<Long, Long>()
                val grinderList: List<*> = data["grinders"] as? List<*> ?: emptyList<Any>()
                val existingGr = repository.getAllGrindersOnce().associateBy { it.name }
                for (gr in grinderList) {
                    val m = gr as Map<*, *>
                    val oldId = (m["id"] as? Double)?.toLong() ?: continue
                    val name = m["name"] as? String ?: continue
                    val newId = if (name in existingGr) {
                        existingGr[name]!!.id
                    } else {
                        repository.insertGrinder(Grinder(
                            name = name,
                            sortOrder = (m["sortOrder"] as? Double)?.toInt() ?: 0
                        ))
                    }
                    grinderIdMap[oldId] = newId
                }


                // Import records
                val recordsList: List<*> = data["records"] as? List<*> ?: emptyList<Any>()
                for (recordMap in recordsList) {
                    val r = recordMap as Map<*, *>
                    val oldBeanId = (r["beanId"] as? Double)?.toLong() ?: 0L
                    val newBeanId = beanIdMap[oldBeanId] ?: continue

                    val oldMethodId = (r["methodId"] as? Double)?.toLong()
                        ?: (r["recipeId"] as? Double)?.toLong()
                    val newMethodId = oldMethodId?.let { methodIdMap[it] }

                    val record = BrewRecord(
                        beanId = newBeanId,
                        methodId = newMethodId,
                        dateTime = (r["dateTime"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                        equipmentId = (r["equipmentId"] as? Double)?.toLong()?.let { equipmentIdMap[it] }
                            // Legacy support: if old backup has equipment name string, match by name
                            ?: (r["equipment"] as? String)?.takeIf { it.isNotEmpty() }?.let { name ->
                                repository.getAllEquipmentOnce().find { it.name == name }?.id
                                    ?: repository.insertEquipment(Equipment(name = name, sortOrder = 0))
                            },
                        coffeeWeight = (r["coffeeWeight"] as? Double) ?: 0.0,
                        coffeeWaterRatio = (r["coffeeWaterRatio"] as? Double) ?: 0.0,
                        waterAmount = (r["waterAmount"] as? Double) ?: 0.0,
                        waterTemp = (r["waterTemp"] as? Double) ?: 0.0,
                        grinderId = (r["grinderId"] as? Double)?.toLong()?.let { grinderIdMap[it] }
                            // Legacy support: if old backup has grinder name string, match by name
                            ?: (r["grinder"] as? String)?.takeIf { it.isNotEmpty() }?.let { name ->
                                repository.getAllGrindersOnce().find { it.name == name }?.id
                                    ?: repository.insertGrinder(Grinder(name = name, sortOrder = 0))
                            },
                        grindSize = r["grindSize"] as? String ?: "",
                        extractionTime = (r["extractionTime"] as? Double)?.toInt() ?: 0,
                        pouringDurationSeconds = (r["pouringDurationSeconds"] as? Double)?.toInt(),
                        acidity = (r["acidity"] as? Double)?.toInt() ?: 0,
                        sweetness = (r["sweetness"] as? Double)?.toInt() ?: 0,
                        bitterness = (r["bitterness"] as? Double)?.toInt() ?: 0,
                        mouthfeel = (r["mouthfeel"] as? Double)?.toInt() ?: 0,
                        aftertaste = (r["aftertaste"] as? Double)?.toInt() ?: 0,
                        overallRating = (r["overallRating"] as? Double)?.toInt() ?: 0,
                        flavorNotes = r["flavorNotes"] as? String ?: "",
                        imageUri = r["imageUri"] as? String ?: "",
                        isIced = (r["isIced"] as? Boolean) ?: false,
                        iceAmount = (r["iceAmount"] as? Double)?.toInt() ?: 0,
                        bypassAmount = (r["bypassAmount"] as? Double)?.toInt() ?: 0,
                        createdAt = (r["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (r["updatedAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                    )
                    repository.insertRecord(record)
                }

                // Import roast degrees (with ID mapping for RestPeriod/PeakFlavor configs)
                val roastDegreeIdMap = mutableMapOf<Long, Long>()
                val roastDegreeList: List<*> = data["roastDegrees"] as? List<*> ?: emptyList<Any>()
                for (rd in roastDegreeList) {
                    val m = rd as Map<*, *>
                    val oldId = (m["id"] as? Double)?.toLong() ?: continue
                    val name = m["name"] as? String ?: continue
                    val newId = repository.insertRoastDegree(RoastDegree(
                        name = name,
                        sortOrder = (m["sortOrder"] as? Double)?.toInt() ?: 0
                    ))
                    roastDegreeIdMap[oldId] = newId
                }

                // Import process methods
                val processMethodList: List<*> = data["processMethods"] as? List<*> ?: emptyList<Any>()
                for (pm in processMethodList) {
                    val m = pm as Map<*, *>
                    val name = m["name"] as? String ?: continue
                    repository.insertProcessMethod(ProcessMethod(
                        name = name,
                        sortOrder = (m["sortOrder"] as? Double)?.toInt() ?: 0
                    ))
                }

                // Import rest period configs (with mapped roastDegreeId)
                val restPeriodList: List<*> = data["restPeriodConfigs"] as? List<*> ?: emptyList<Any>()
                for (rp in restPeriodList) {
                    val m = rp as Map<*, *>
                    val oldRoastDegreeId = (m["roastDegreeId"] as? Double)?.toLong() ?: continue
                    val newRoastDegreeId = roastDegreeIdMap[oldRoastDegreeId] ?: continue
                    val restDays = (m["restDays"] as? Double)?.toInt() ?: 0
                    repository.insertRestPeriodConfig(RestPeriodConfig(
                        roastDegreeId = newRoastDegreeId,
                        restDays = restDays
                    ))
                }

                // Import peak flavor configs (with mapped roastDegreeId)
                val peakFlavorList: List<*> = data["peakFlavorConfigs"] as? List<*> ?: emptyList<Any>()
                for (pf in peakFlavorList) {
                    val m = pf as Map<*, *>
                    val oldRoastDegreeId = (m["roastDegreeId"] as? Double)?.toLong() ?: continue
                    val newRoastDegreeId = roastDegreeIdMap[oldRoastDegreeId] ?: continue
                    val peakDays = (m["peakFlavorDays"] as? Double)?.toInt() ?: 0
                    repository.insertPeakFlavorConfig(PeakFlavorConfig(
                        roastDegreeId = newRoastDegreeId,
                        peakFlavorDays = peakDays
                    ))
                }

                // Import purchase records (with mapped beanId)
                val purchaseRecordList: List<*> = data["purchaseRecords"] as? List<*> ?: emptyList<Any>()
                for (pr in purchaseRecordList) {
                    val m = pr as Map<*, *>
                    val oldBeanId = (m["beanId"] as? Double)?.toLong() ?: continue
                    val newBeanId = beanIdMap[oldBeanId] ?: continue
                    val date = (m["date"] as? Double)?.toLong() ?: System.currentTimeMillis()
                    val price = (m["price"] as? Double)?.toFloat() ?: 0f
                    val weightGrams = (m["weightGrams"] as? Double)?.toInt() ?: 0
                    val roastDate = (m["roastDate"] as? Double)?.toLong()
                    repository.insertPurchaseRecord(PurchaseRecord(
                        beanId = newBeanId,
                        date = date,
                        price = price,
                        weightGrams = weightGrams,
                        roastDate = roastDate
                    ))
                }

                // Import stock adjustments (with mapped beanId; legacy backups have no key, skip)
                val stockAdjustmentList: List<*> = data["stockAdjustments"] as? List<*> ?: emptyList<Any>()
                for (sa in stockAdjustmentList) {
                    val m = sa as Map<*, *>
                    val oldBeanId = (m["beanId"] as? Double)?.toLong() ?: continue
                    val newBeanId = beanIdMap[oldBeanId] ?: continue
                    val changeGrams = (m["changeGrams"] as? Double) ?: 0.0
                    repository.insertStockAdjustment(StockAdjustment(
                        beanId = newBeanId,
                        changeGrams = changeGrams,
                        note = m["note"] as? String ?: "",
                        createdAt = (m["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                    ))
                }
                } // end withTransaction

                val counts: Map<*, *> = if (manifestContent != null) {
                    try {
                        val manifest = gson.fromJson(manifestContent, Map::class.java)
                        manifest["counts"] as? Map<*, *> ?: emptyMap<Any, Any>()
                    } catch (e: Exception) {
                        emptyMap<Any, Any>()
                    }
                } else {
                    emptyMap<Any, Any>()
                }
                val beansCountVal = (counts["beans"] as? Double)?.toInt() ?: 0
                val recordsCountVal = (counts["records"] as? Double)?.toInt() ?: 0
                withContext(Dispatchers.Main) {
                    _backupState.value = BackupState.Success(
                        title = "恢复成功",
                        message = if (manifestContent != null) "数据已从备份恢复" else "数据已从备份恢复",
                        exportDate = if (manifestContent != null) exportDate else "unknown",
                        version = manifestVersion,
                        beansCount = beansCountVal,
                        recordsCount = recordsCountVal
                    )
                }
            }
    }
}