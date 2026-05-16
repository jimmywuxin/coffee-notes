package com.coffeelab.coffeenotes.data

import androidx.room.TypeConverter
import com.coffeelab.coffeenotes.data.entity.BrewMethodStep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromBrewMethodStepList(steps: List<BrewMethodStep>?): String? {
        return steps?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toBrewMethodStepList(json: String?): List<BrewMethodStep>? {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<BrewMethodStep>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            // Defensive: if JSON is malformed, return empty instead of crashing
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? = list?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private val gson = Gson()

        /** Parse JSON string to List<BrewMethodStep> (same logic as Room TypeConverter) */
        fun parseSteps(json: String?): List<BrewMethodStep> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val type = object : TypeToken<List<BrewMethodStep>>() {}.type
                gson.fromJson<List<BrewMethodStep>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                // Defensive: if JSON is malformed, return empty instead of crashing
                emptyList()
            }
        }

        fun serializeSteps(steps: List<BrewMethodStep>): String? {
            return if (steps.isEmpty()) null else gson.toJson(steps)
        }
    }
}
