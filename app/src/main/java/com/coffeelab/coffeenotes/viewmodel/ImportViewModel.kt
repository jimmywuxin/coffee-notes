package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.FlavorTag
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class ImportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    private val gson = Gson()

    sealed class ImportState {
        object Idle : ImportState()
        object Loading : ImportState()
        data class Success(val beanCount: Int, val recordCount: Int, val tagCount: Int, val skippedRecordCount: Int = 0) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun resetState() {
        _importState.value = ImportState.Idle
    }

    suspend fun importFromUri(context: Context, uri: Uri) {
        _importState.value = ImportState.Loading

        try {
            withContext(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val json = reader.readText()
                reader.close()

                val backup = gson.fromJson(json, JsonObject::class.java)
                val data = backup.getAsJsonObject("data")

                // Import beans
                val beans = data.getAsJsonArray("coffeeBeans")
                val beanIdMap = mutableMapOf<String, Long>()
                var beanCount = 0
                var tagCount = 0

                for (element in beans) {
                    val bean = element.asJsonObject
                    val oldId = bean.get("id")?.asString ?: ""

                    val roastDate = bean.get("roastDate")?.asString
                    val roastDateTs = if (!roastDate.isNullOrEmpty()) {
                        try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(roastDate)?.time
                        } catch (e: Exception) { null }
                    } else null

                    val blend = bean.getAsJsonArray("blendComponents")
                    val firstBlend = if (blend.size() > 0) blend[0].asJsonObject else null
                    val origin = firstBlend?.get("origin")?.asString ?: ""
                    val estate = firstBlend?.get("estate")?.asString ?: ""
                    val process = firstBlend?.get("process")?.asString ?: ""
                    val variety = firstBlend?.get("variety")?.asString ?: ""

                    val newBean = CoffeeBean(
                        roaster = bean.get("roaster")?.asString ?: "",
                        name = bean.get("name")?.asString ?: "",
                        origin = origin,
                        estate = estate,
                        variety = variety,
                        process = process,
                        roastLevel = bean.get("roastLevel")?.asString ?: "",
                        roastDate = roastDateTs,
                        notes = bean.get("notes")?.asString ?: "",
                        createdAt = bean.get("timestamp")?.asLong ?: System.currentTimeMillis(),
                        updatedAt = bean.get("timestamp")?.asLong ?: System.currentTimeMillis()
                    )

                    val newId = repository.insertBean(newBean)
                    beanIdMap[oldId] = newId
                    beanCount++

                    // Import flavors
                    val flavors = bean.getAsJsonArray("flavor")
                    val flavorList = mutableListOf<String>()
                    for (flavor in flavors) {
                        flavorList.add(flavor.asString)
                    }
                    if (flavorList.isNotEmpty()) {
                        repository.saveTagsForBean(newId, flavorList)
                        tagCount += flavorList.size
                    }
                }

                // Import brew records
                val notes = data.getAsJsonArray("brewingNotes")
                var recordCount = 0
                var skippedRecordCount = 0

                for (element in notes) {
                    val note = element.asJsonObject
                    val beanOldId = note.get("beanId")?.asString ?: ""
                    // Skip if bean not found in imported data (orphaned record)
                    val beanNewId = beanIdMap[beanOldId]
                    if (beanNewId == null) {
                        skippedRecordCount++
                        continue
                    }

                    val params = note.getAsJsonObject("params")
                    val coffeeStr = params?.get("coffee")?.asString ?: "0"
                    val waterStr = params?.get("water")?.asString ?: "0"
                    val tempStr = params?.get("temp")?.asString ?: "0"

                    val coffeeWeight = coffeeStr.replace("g", "").trim().toDoubleOrNull() ?: 0.0
                    val waterWeight = waterStr.replace("g", "").replace("ml", "").trim().toDoubleOrNull() ?: 0.0
                    val waterTemp = tempStr.replace("°C", "").trim().toDoubleOrNull() ?: 0.0
                    val coffeeWaterRatio = if (coffeeWeight > 0 && waterWeight > 0) waterWeight / coffeeWeight else 0.0

                    val taste = note.getAsJsonObject("taste")
                    val totalTime = note.get("totalTime")?.let {
                        if (it.isJsonPrimitive && it.asJsonPrimitive.isNumber) it.asInt
                        else it.asString.replace("s", "").trim().toIntOrNull() ?: 0
                    } ?: 0

                    val record = BrewRecord(
                        beanId = beanNewId,
                        dateTime = note.get("timestamp")?.asLong ?: System.currentTimeMillis(),
                        equipment = note.get("equipment")?.asString ?: "",
                        coffeeWeight = coffeeWeight,
                        coffeeWaterRatio = coffeeWaterRatio,
                        waterTemp = waterTemp,
                        grindSize = params?.get("grindSize")?.asString ?: "",
                        acidity = taste?.get("acidity")?.asInt ?: 0,
                        sweetness = taste?.get("sweetness")?.asInt ?: 0,
                        bitterness = taste?.get("bitterness")?.asInt ?: 0,
                        mouthfeel = taste?.get("body")?.asInt ?: 0,
                        overallRating = note.get("rating")?.asInt ?: 0,
                        flavorNotes = note.get("notes")?.asString ?: "",
                        createdAt = note.get("timestamp")?.asLong ?: System.currentTimeMillis(),
                        updatedAt = note.get("updatedAt")?.asLong ?: note.get("timestamp")?.asLong ?: System.currentTimeMillis()
                    )

                    repository.insertRecord(record)
                    recordCount++
                }

                _importState.value = ImportState.Success(beanCount, recordCount, tagCount, skippedRecordCount)
            }
        } catch (e: Exception) {
            _importState.value = ImportState.Error("导入失败: ${e.message}")
        }
    }
}
