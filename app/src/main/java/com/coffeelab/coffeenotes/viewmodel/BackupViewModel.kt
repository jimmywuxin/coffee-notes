package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.*
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import com.coffeelab.coffeenotes.util.ImageUtils
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
import java.text.SimpleDateFormat
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

    suspend fun exportBackup(context: Context, uri: Uri) {
        _backupState.value = BackupState.Loading

        try {
            withContext(Dispatchers.IO) {
                val beans = repository.allBeans.first()
                val methods = repository.allMethods.first()
                val records = repository.allRecords.first()
                val equipment = repository.getAllEquipmentOnce()
                val grinders = repository.getAllGrindersOnce()

                // Collect tags for each bean
                val beansWithTags = beans.map { bean ->
                    val tags = repository.getTagsForBeanOnce(bean.id)
                    mapOf(
                        "bean" to bean,
                        "tags" to tags.map { it.name }
                    )
                }

                // Records include pouringDurationSeconds
                val recordsData = records.map { record ->
                    mapOf(
                        "beanId" to record.beanId,
                        "methodId" to record.methodId,
                        "dateTime" to record.dateTime,
                        "equipment" to record.equipment,
                        "coffeeWeight" to record.coffeeWeight,
                        "coffeeWaterRatio" to record.coffeeWaterRatio,
                        "waterAmount" to record.waterAmount,
                        "waterTemp" to record.waterTemp,
                        "grinder" to record.grinder,
                        "grindSize" to record.grindSize,
                        "extractionTime" to record.extractionTime,
                        "pouringDurationSeconds" to record.pouringDurationSeconds,
                        "acidity" to record.acidity,
                        "sweetness" to record.sweetness,
                        "bitterness" to record.bitterness,
                        "mouthfeel" to record.mouthfeel,
                        "aftertaste" to record.aftertaste,
                        "overallRating" to record.overallRating,
                        "flavorNotes" to record.flavorNotes,
                        "imageUri" to record.imageUri,
                        "isIced" to record.isIced,
                        "iceAmount" to record.iceAmount,
                        "bypassAmount" to record.bypassAmount,
                        "createdAt" to record.createdAt,
                        "updatedAt" to record.updatedAt
                    )
                }

                // Build data.json content
                val dataMap = mapOf(
                    "beans" to beansWithTags,
                    "records" to recordsData,
                    "methods" to methods,
                    "equipment" to equipment,
                    "grinders" to grinders
                )
                val dataJson = gson.toJson(dataMap)

                // Build manifest
                val manifest = mapOf(
                    "version" to BACKUP_VERSION,
                    "exportDate" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault()).format(Date()),
                    "appVersion" to "1.2.0",
                    "counts" to mapOf(
                        "beans" to beans.size,
                        "records" to records.size,
                        "methods" to methods.size,
                        "equipment" to equipment.size,
                        "grinders" to grinders.size
                    )
                )
                val manifestJson = gson.toJson(manifest)

                // Write ZIP
                val zos = ZipOutputStream(context.contentResolver.openOutputStream(uri))

                // Write manifest.json
                zos.putNextEntry(ZipEntry(MANIFEST_FILE))
                zos.write(manifestJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Write data.json
                zos.putNextEntry(ZipEntry(DATA_FILE))
                zos.write(dataJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Write bean photos
                val photosDir = ImageUtils.getBeanPhotosDir(context)
                if (photosDir.exists()) {
                    photosDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.endsWith(".jpg")) {
                            zos.putNextEntry(ZipEntry("$BEAN_PHOTOS_DIR/${file.name}"))
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }

                zos.finish()
                zos.close()

                withContext(Dispatchers.Main) {
                    _backupState.value = BackupState.Success(
                        message = "备份成功！\n豆子: ${beans.size} 个\n冲煮记录: ${records.size} 条\n冲煮手法: ${methods.size} 个\n器具: ${equipment.size} 个\n磨豆机: ${grinders.size} 个",
                        exportDate = "刚刚",
                        version = BACKUP_VERSION,
                        beansCount = beans.size,
                        recordsCount = records.size
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
            withContext(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("无法打开文件")

                // Read all bytes first to detect format
                val allBytes = inputStream.readBytes()
                inputStream.close()

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

                // Delete all records first (cascade will delete them), then beans, methods, equipment, grinders
                recordDao.deleteAll()
                beanDao.deleteAll()
                methodDao.deleteAll()
                equipmentDao.deleteAll()
                grinderDao.deleteAll()

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
                        updatedAt = (beanMap["updatedAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                    )

                    val newId = repository.insertBean(newBean)
                    beanIdMap[oldId] = newId

                    val tagList = tagsList.filterIsInstance<String>()
                    if (tagList.isNotEmpty()) {
                        repository.saveTagsForBean(newId, tagList)
                    }
                }

                // Import methods
                val methodsList: List<*> = data["methods"] as? List<*> ?: emptyList<Any>()
                val methodIdMap = mutableMapOf<Long, Long>()
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
                        equipment = r["equipment"] as? String ?: "",
                        coffeeWeight = (r["coffeeWeight"] as? Double) ?: 0.0,
                        coffeeWaterRatio = (r["coffeeWaterRatio"] as? Double) ?: 0.0,
                        waterAmount = (r["waterAmount"] as? Double) ?: 0.0,
                        waterTemp = (r["waterTemp"] as? Double) ?: 0.0,
                        grinder = r["grinder"] as? String ?: "",
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

                // Import equipment
                val equipmentList: List<*> = data["equipment"] as? List<*> ?: emptyList<Any>()
                for (eq in equipmentList) {
                    val m = eq as Map<*, *>
                    val name = m["name"] as? String ?: continue
                    repository.insertEquipment(Equipment(
                        name = name,
                        sortOrder = (m["sortOrder"] as? Double)?.toInt() ?: 0
                    ))
                }

                // Import grinders
                val grinderList: List<*> = data["grinders"] as? List<*> ?: emptyList<Any>()
                for (gr in grinderList) {
                    val m = gr as Map<*, *>
                    val name = m["name"] as? String ?: continue
                    repository.insertGrinder(Grinder(
                        name = name,
                        sortOrder = (m["sortOrder"] as? Double)?.toInt() ?: 0
                    ))
                }

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
                        message = if (manifestContent != null) "恢复成功！" else "恢复成功！",
                        exportDate = if (manifestContent != null) exportDate else "unknown",
                        version = manifestVersion,
                        beansCount = beansCountVal,
                        recordsCount = recordsCountVal
                    )
                }
            }
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("恢复失败: ${e.message}")
        }
    }
}