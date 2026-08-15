package com.coffeelab.coffeenotes.util

import android.content.Context
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 备份 ZIP 生成（纯逻辑，导出与云端上传共用）。
 *
 * ZIP 结构：manifest.json + data.json + images/bean_photos 下的 jpg 照片
 */
object BackupBuilder {

    const val MANIFEST_FILE = "manifest.json"
    const val DATA_FILE = "data.json"
    const val BEAN_PHOTOS_DIR = "images/bean_photos"
    const val BACKUP_VERSION = "1.2.0"

    data class BackupCounts(
        val beans: Int,
        val records: Int,
        val methods: Int,
        val equipment: Int,
        val grinders: Int
    )

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /** 生成备份 ZIP 字节流，返回 (字节, 各类数量统计)。 */
    suspend fun buildZipBytes(context: Context, repository: CoffeeRepository): Pair<ByteArray, BackupCounts> =
        withContext(Dispatchers.IO) {
            val beans = repository.allBeans.first()
            val methods = repository.allMethods.first()
            val records = repository.allRecords.first()
            val equipment = repository.getAllEquipmentOnce()
            val grinders = repository.getAllGrindersOnce()
            val roastDegrees = repository.getAllRoastDegreesOnce()
            val processMethods = repository.getAllProcessMethodsOnce()
            val restPeriodConfigs = repository.getAllRestPeriodConfigsOnce()
            val peakFlavorConfigs = repository.getAllPeakFlavorConfigsOnce()
            val purchaseRecords = repository.getAllPurchaseRecordsOnce()
            val impressionTags = repository.getAllImpressionTagsOnce()

            // 每个豆子的库存调整（用旧 beanId，导入时经 beanIdMap 重映射）
            val adjustmentsByBean = beans.associate { bean ->
                bean.id to repository.getAdjustmentsForBeanOnce(bean.id)
            }

            // 每个豆子的风味标签 + 印象标签
            val beansWithTags = beans.map { bean ->
                val tags = repository.getTagsForBeanOnce(bean.id)
                val impTags = repository.getImpressionTagsForBeanOnce(bean.id)
                mapOf(
                    "bean" to bean,
                    "tags" to tags.map { it.name },
                    "impressionTagIds" to impTags.map { it.id }
                )
            }

            val stockAdjustmentsData = beans.flatMap { bean ->
                adjustmentsByBean[bean.id].orEmpty().map { adj ->
                    mapOf(
                        "beanId" to bean.id,
                        "changeGrams" to adj.changeGrams,
                        "note" to adj.note,
                        "createdAt" to adj.createdAt
                    )
                }
            }

            val recordsData = records.map { record ->
                mapOf(
                    "beanId" to record.beanId,
                    "methodId" to record.methodId,
                    "dateTime" to record.dateTime,
                    "equipmentId" to record.equipmentId,
                    "coffeeWeight" to record.coffeeWeight,
                    "coffeeWaterRatio" to record.coffeeWaterRatio,
                    "waterAmount" to record.waterAmount,
                    "waterTemp" to record.waterTemp,
                    "grinderId" to record.grinderId,
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

            val dataJson = gson.toJson(
                mapOf(
                    "beans" to beansWithTags,
                    "records" to recordsData,
                    "methods" to methods,
                    "equipment" to equipment,
                    "grinders" to grinders,
                    "roastDegrees" to roastDegrees,
                    "processMethods" to processMethods,
                    "restPeriodConfigs" to restPeriodConfigs,
                    "peakFlavorConfigs" to peakFlavorConfigs,
                    "purchaseRecords" to purchaseRecords,
                    "impressionTags" to impressionTags,
                    "stockAdjustments" to stockAdjustmentsData
                )
            )

            val manifestJson = gson.toJson(
                mapOf(
                    "version" to BACKUP_VERSION,
                    "exportDate" to ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")),
                    "appVersion" to "1.2.0",
                    "counts" to mapOf(
                        "beans" to beans.size,
                        "records" to records.size,
                        "methods" to methods.size,
                        "equipment" to equipment.size,
                        "grinders" to grinders.size,
                        "impressionTags" to impressionTags.size
                    )
                )
            )

            val output = ByteArrayOutputStream()
            ZipOutputStream(output).use { zos ->
                zos.putNextEntry(ZipEntry(MANIFEST_FILE))
                zos.write(manifestJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                zos.putNextEntry(ZipEntry(DATA_FILE))
                zos.write(dataJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                val photosDir = ImageUtils.getBeanPhotosDir(context)
                if (photosDir.exists()) {
                    photosDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.endsWith(".jpg")) {
                            zos.putNextEntry(ZipEntry("$BEAN_PHOTOS_DIR/${file.name}"))
                            FileInputStream(file).use { fis -> fis.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
                zos.finish()
            }

            val counts = BackupCounts(
                beans = beans.size,
                records = records.size,
                methods = methods.size,
                equipment = equipment.size,
                grinders = grinders.size
            )
            output.toByteArray() to counts
        }
}
