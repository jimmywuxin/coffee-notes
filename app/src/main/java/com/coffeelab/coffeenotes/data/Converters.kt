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
        return json?.let {
            val type = object : TypeToken<List<BrewMethodStep>>() {}.type
            gson.fromJson(it, type)
        }
    }

    companion object {
        private val gson = Gson()

        /** Parse JSON string to List<BrewMethodStep> (same logic as Room TypeConverter) */
        fun parseSteps(json: String?): List<BrewMethodStep> {
            return json?.let {
                val type = object : TypeToken<List<BrewMethodStep>>() {}.type
                gson.fromJson<List<BrewMethodStep>>(it, type) ?: emptyList()
            } ?: emptyList()
        }

        /** Serialize List<BrewMethodStep> to JSON string (same logic as Room TypeConverter) */
        fun serializeSteps(steps: List<BrewMethodStep>): String? {
            return if (steps.isEmpty()) null else gson.toJson(steps)
        }
    }
}
