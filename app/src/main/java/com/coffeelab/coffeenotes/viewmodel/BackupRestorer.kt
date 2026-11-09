package com.coffeelab.coffeenotes.viewmodel

import android.content.Context
import androidx.room.withTransaction
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.*
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import com.coffeelab.coffeenotes.util.AppConstants
import com.coffeelab.coffeenotes.util.BackupBuilder
import com.coffeelab.coffeenotes.util.BackupGuard
import com.coffeelab.coffeenotes.util.ImageUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import com.coffeelab.coffeenotes.viewmodel.BackupViewModel.BackupState

/** 状态类型仍归 [BackupViewModel]（UI 侧按 `BackupViewModel.BackupState` 消费），此处用别名引用 */

/**
 * 备份恢复（从 BackupViewModel 拆出）。
 *
 * 拆它的理由：原类706 行里约 480 行都是这一个方法的具体流程，与「云端上传 / 本地导出 /
 * UI 状态」三类无关的事混在一起。本类是纯逻辑、无 UI 状态，构造时注入 Context。
 *
 * 安全性：这是全app 唯一「先全表清空再插入」的路径，动手前有三道闸（见 BackupGuard）：
 * 结构校验、导入前自动快照、照片事务后再落盘。
 */
class BackupRestorer(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val repository = CoffeeRepository(AppDatabase.getInstance(context))

    companion object {
        // 备份格式常量统一由 BackupBuilder 持有
        const val MANIFEST_FILE = BackupBuilder.MANIFEST_FILE
        const val DATA_FILE = BackupBuilder.DATA_FILE
        const val BEAN_PHOTOS_DIR = BackupBuilder.BEAN_PHOTOS_DIR
    }

    /**
     * 从备份字节流恢复数据（本地文件恢复与云端下载恢复共用）。
     *
     * 返回本次恢复的实际结果，由调用方决定 UI 怎么显示 —— 不要在这里写 _backupState，
     * 否则云端路径会拿到一个「已被内层覆盖的 Error」却仍报成功。
     */
    suspend fun restore(allBytes: ByteArray): BackupState {
        return withContext(Dispatchers.IO) {
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
                        // zip-slip 防护：条目名必须是 zip 内的相对路径。
                        // 损坏/恶意备份里出现 ../ 或绝对路径时，直接拒绝整个文件。
                        if (!BackupGuard.isSafeEntryName(name)) {
                            zipInputStream.close()
                            return@withContext BackupState.Error("无效备份文件：包含非法路径条目")
                        }
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
                        return@withContext BackupState.Error("无效备份文件：缺少 manifest.json")
                    }

                    val manifest = gson.fromJson(manifestContent, Map::class.java)
                    manifestVersion = manifest["version"] as? String ?: "unknown"
                    exportDate = manifest["exportDate"] as? String ?: "unknown"

                    if (dataContent == null) {
                        return@withContext BackupState.Error("无效备份文件：缺少 data.json")
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
                            return@withContext BackupState.Error("无效备份文件：格式无法识别")
                        }
                    } catch (e: Exception) {
                        return@withContext BackupState.Error("无效备份文件：${e.message}")
                    }
                }

                // Parse data
                val backup = gson.fromJson(dataContent, Map::class.java)
                val data = backup["data"] as? Map<*, *> ?: backup

                // 【闸1】结构校验：data.json 异构时上面会把外层当 data，
                // beans/records 双双变成空列表。此时若继续，全库清空后插入 0 条 = 用户数据归零。
                when (val v = BackupGuard.validateData(data)) {
                    is BackupGuard.Verdict.Reject -> return@withContext BackupState.Error(v.message)
                    is BackupGuard.Verdict.Ok -> Unit
                }

                // 【闸2】导入前自动快照当前库：恢复是「先全清再插入」的唯一入口，
                // 一步走错不可逆。快照失败不阻断恢复（用户可能手动清了存储），只记日志。
                val snapshotDir = context.filesDir.resolve("pre_restore_snapshots").apply { mkdirs() }
                val snapshotFile = snapshotDir.resolve(
                    "${AppConstants.BACKUP_FILE_PREFIX}before_restore_${System.currentTimeMillis()}${AppConstants.BACKUP_FILE_EXT}"
                )
                runCatching {
                    val (snapBytes, _) = BackupBuilder.buildZipBytes(context, repository)
                    snapshotFile.writeBytes(snapBytes)
                }.onFailure { e ->
                    android.util.Log.w("BackupViewModel", "恢复前快照失败: ${e.message}")
                }

                // 【闸3】照片先解压到临时目录，事务提交成功后才原子替换 bean_photos。
                // 原来直接写进 photosDir：事务失败回滚后 DB 是旧数据、磁盘照片已被新备份覆盖。
                val photosDir = ImageUtils.getBeanPhotosDir(context)
                val stagingDir = File(context.cacheDir, "restore_photos_staging_${System.currentTimeMillis()}")
                stagingDir.mkdirs()
                photoFiles.forEach { (relativePath, content) ->
                    val file = File(stagingDir, relativePath)
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos -> fos.write(content) }
                }

                // Clear existing data (full replace)
                val counts: Map<*, *> = if (manifestContent != null) {
                    runCatching {
                        (gson.fromJson(manifestContent, Map::class.java)["counts"] as? Map<*, *>)
                            ?: emptyMap<Any, Any>()
                    }.getOrDefault(emptyMap<Any, Any>())
                } else {
                    emptyMap<Any, Any>()
                }
                val db = AppDatabase.getInstance(context)
                // 统计量定义在事务外：withTransaction 的 lambda 内声明的变量出作用域后不可见
                var beansImported = 0
                var recordsImported = 0
                var recordsSkipped = 0
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

                // 事务 + 照片搬运整体包在 try 里：任何异常都要保证暂存目录被清掉
                try {
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
                // ocr_corrections 无外键、不在任何 CASCADE 里，漏删会让恢复后仍残留
                // 旧库的 OCR 自动纠错（跨库套用到新豆子上）
                db.ocrCorrectionDao().deleteAll()

                // Import impression tags (before beans so bean_impression_tags FK resolves)
                val impressionTagIdMap = mutableMapOf<Long, Long>()
                val impressionTagData: List<*> = (data["impressionTags"] ?: emptyList<Any>()) as List<*>
                val existingImps = repository.getAllImpressionTagsOnce().associateBy { it.name }
                for (it in impressionTagData) {
                    val m = it as Map<*, *>
                    val oldId = (m["id"] as? Double)?.toLong() ?: continue
                    val name = m["name"] as? String ?: continue
                    val matched = existingImps[name]
                    val newId = if (matched != null) {
                        matched.id
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
                    beansImported++

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
                    val matched = existingEq[name]
                    val newId = if (matched != null) {
                        matched.id
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
                    val matched = existingGr[name]
                    val newId = if (matched != null) {
                        matched.id
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
                    val newBeanId = beanIdMap[oldBeanId]
                    if (newBeanId == null) {
                        // 找不到归属豆子 → 计入跳过数，不静默丢弃
                        recordsSkipped++
                        continue
                    }

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
                        bedShape = r["bedShape"] as? String ?: "",
                        imageUri = r["imageUri"] as? String ?: "",
                        isIced = (r["isIced"] as? Boolean) ?: false,
                        iceAmount = (r["iceAmount"] as? Double)?.toInt() ?: 0,
                        bypassAmount = (r["bypassAmount"] as? Double)?.toInt() ?: 0,
                        createdAt = (r["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (r["updatedAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                    )
                    repository.insertRecord(record)
                    recordsImported++
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

                // 事务已提交，现在才把照片从暂存目录搬进正式目录
                if (stagingDir.exists() && stagingDir.listFiles()?.isNotEmpty() == true) {
                    photosDir.mkdirs()
                    photosDir.listFiles()?.forEach { it.delete() }   // 清掉上次残留，避免孤儿照片堆积
                    stagingDir.listFiles()?.forEach { it.renameTo(File(photosDir, it.name)) }
                }
                } finally {
                    stagingDir.deleteRecursively()
                }

                // 回报实际写入量，而不是 manifest 里的自称值。
                // 顺带把 manifest 声称值作为对照，不一致时明示，避免「看起来完整」的假象。
                val note = BackupGuard.buildResultNote(
                    beansImported = beansImported,
                    recordsImported = recordsImported,
                    recordsSkipped = recordsSkipped,
                    claimedBeans = (counts["beans"] as? Double)?.toInt(),
                    claimedRecords = (counts["records"] as? Double)?.toInt()
                )
                BackupState.Success(
                    title = "恢复成功",
                    message = note,
                    exportDate = if (manifestContent != null) exportDate else "unknown",
                    version = manifestVersion,
                    beansCount = beansImported,
                    recordsCount = recordsImported
                )
            }
    }}
