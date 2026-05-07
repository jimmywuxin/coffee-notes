package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.*
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    sealed class BackupState {
        object Idle : BackupState()
        object Loading : BackupState()
        data class Success(val message: String) : BackupState()
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

                val backup = mapOf(
                    "exportDate" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault()).format(Date()),
                    "appVersion" to "1.1.0",
                    "data" to mapOf(
                        "beans" to beansWithTags,
                        "records" to records,
                        "methods" to methods,
                        "equipment" to equipment,
                        "grinders" to grinders
                    )
                )

                val json = gson.toJson(backup)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                }

                _backupState.value = BackupState.Success(
                    "备份成功！\n豆子: ${beans.size} 个\n冲煮记录: ${records.size} 条\n冲煮手法: ${methods.size} 个\n器具: ${equipment.size} 个\n磨豆机: ${grinders.size} 个"
                )
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
                val reader = BufferedReader(InputStreamReader(inputStream))
                val json = reader.readText()
                reader.close()

                val backup = gson.fromJson(json, Map::class.java)
                val data = backup["data"] as Map<*, *>

                // Import beans
                val beans = data["beans"] as List<*>
                val beanIdMap = mutableMapOf<Long, Long>()

                for (beanEntry in beans) {
                    val entry = beanEntry as Map<*, *>
                    val beanMap = entry["bean"] as Map<*, *>
                    val tags = entry["tags"] as List<*>

                    val oldId = (beanMap["id"] as Double).toLong()
                    val newBean = CoffeeBean(
                        roaster = beanMap["roaster"] as String,
                        name = beanMap["name"] as String,
                        origin = beanMap["origin"] as String,
                        estate = beanMap["estate"] as String,
                        variety = beanMap["variety"] as String,
                        process = beanMap["process"] as String,
                        roastLevel = beanMap["roastLevel"] as String,
                        roastDate = (beanMap["roastDate"] as? Double)?.toLong(),
                        notes = beanMap["notes"] as String,
                        imageUri = beanMap["imageUri"] as String,
                        isFavorite = (beanMap["isFavorite"] as? Boolean) ?: false,
                        dose = (beanMap["dose"] as? Double)?.toFloat(),
                        brewRatio = beanMap["brewRatio"] as? String,
                        waterAmount = (beanMap["waterAmount"] as? Double)?.toFloat(),
                        brewTime = (beanMap["brewTime"] as? Double)?.toInt(),
                        waterTemp = (beanMap["waterTemp"] as? Double)?.toInt(),
                        createdAt = (beanMap["createdAt"] as Double).toLong(),
                        updatedAt = (beanMap["updatedAt"] as Double).toLong()
                    )

                    val newId = repository.insertBean(newBean)
                    beanIdMap[oldId] = newId

                    // Import tags
                    val tagList = tags.map { it as String }
                    if (tagList.isNotEmpty()) {
                        repository.saveTagsForBean(newId, tagList)
                    }
                }

                // Import methods (new brew_methods table) — MUST come first to build methodIdMap
                val methods = data["methods"] as? List<*>
                var importedMethods = 0
                val methodIdMap = mutableMapOf<Long, Long>()
                methods?.forEach { methodEntry ->
                    val m = methodEntry as Map<*, *>
                    val oldId = (m["id"] as Double).toLong()
                    // steps is stored as JSON string (Room TypeConverter serializes List<BrewMethodStep>)
                    val stepsStr = m["steps"] as? String
                    val method = BrewMethod(
                        name = m["name"] as String,
                        isPreset = (m["isPreset"] as? Boolean) ?: false,
                        steps = stepsStr,
                        createdAt = (m["createdAt"] as Double).toLong(),
                        updatedAt = (m["updatedAt"] as Double).toLong()
                    )
                    val newId = repository.insertMethod(method)
                    methodIdMap[oldId] = newId
                    importedMethods++
                }

                // Import records (with backward compat: old recipeId → methodId, using methodIdMap)
                val records = data["records"] as List<*>
                for (recordMap in records) {
                    val r = recordMap as Map<*, *>
                    val oldBeanId = (r["beanId"] as Double).toLong()
                    val newBeanId = beanIdMap[oldBeanId] ?: continue

                    val waterWeight = (r["waterWeight"] as? Double) ?: 0.0
                    val coffeeWeightVal = (r["coffeeWeight"] as? Double) ?: 0.0
                    val coffeeWaterRatioVal = if (coffeeWeightVal > 0 && waterWeight > 0) {
                        (waterWeight / coffeeWeightVal)
                    } else {
                        (r["coffeeWaterRatio"] as? Double) ?: 0.0
                    }

                    // Backward compat: old backups use recipeId, new field is methodId
                    val oldMethodId = (r["methodId"] as? Double)?.toLong()
                        ?: (r["recipeId"] as? Double)?.toLong()
                    val newMethodId = oldMethodId?.let { methodIdMap[it] }

                    val record = BrewRecord(
                        beanId = newBeanId,
                        methodId = newMethodId,
                        dateTime = (r["dateTime"] as Double).toLong(),
                        equipment = r["equipment"] as? String ?: "",
                        coffeeWeight = coffeeWeightVal,
                        coffeeWaterRatio = coffeeWaterRatioVal,
                        waterAmount = r["waterAmount"] as? Double ?: 0.0,
                        waterTemp = r["waterTemp"] as? Double ?: 0.0,
                        grinder = r["grinder"] as? String ?: "",
                        grindSize = r["grindSize"] as? String ?: "",
                        extractionTime = (r["extractionTime"] as? Double)?.toInt() ?: 0,
                        acidity = (r["acidity"] as? Double)?.toInt() ?: 0,
                        sweetness = (r["sweetness"] as? Double)?.toInt() ?: 0,
                        bitterness = (r["bitterness"] as? Double)?.toInt() ?: 0,
                        mouthfeel = (r["mouthfeel"] as? Double)?.toInt() ?: 0,
                        aftertaste = (r["aftertaste"] as? Double)?.toInt() ?: 0,
                        overallRating = (r["overallRating"] as? Double)?.toInt() ?: 0,
                        flavorNotes = r["flavorNotes"] as? String ?: "",
                        imageUri = r["imageUri"] as? String ?: "",
                        createdAt = (r["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (r["updatedAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                    )
                    repository.insertRecord(record)
                }

                // Import equipment (dedup by name)
                val existingEquipment = repository.getAllEquipmentOnce().map { it.name }.toSet()
                val equipmentList = data["equipment"] as? List<*>
                var importedEquipment = 0
                equipmentList?.forEach { eq ->
                    val m = eq as Map<*, *>
                    val name = m["name"] as? String ?: return@forEach
                    if (name !in existingEquipment) {
                        repository.insertEquipment(Equipment(name = name, sortOrder = (m["sortOrder"] as? Double)?.toInt() ?: 0))
                        importedEquipment++
                    }
                }

                // Import grinders (dedup by name)
                val existingGrinderNames = repository.getAllGrindersOnce().map { it.name }.toSet()
                val grinderList = data["grinders"] as? List<*>
                var importedGrinders = 0
                grinderList?.forEach { gr ->
                    val m = gr as Map<*, *>
                    val name = m["name"] as? String ?: return@forEach
                    if (name !in existingGrinderNames) {
                        repository.insertGrinder(Grinder(name = name, sortOrder = (m["sortOrder"] as? Double)?.toInt() ?: 0))
                        importedGrinders++
                    }
                }

                _backupState.value = BackupState.Success(
                    "恢复成功！\n豆子: ${beans.size} 个\n冲煮记录: ${records.size} 条\n冲煮手法: +$importedMethods 个\n器具: +$importedEquipment 个\n磨豆机: +$importedGrinders 个"
                )
            }
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("恢复失败: ${e.message}")
        }
    }
}
