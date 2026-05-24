package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.PeakFlavorConfig
import com.coffeelab.coffeenotes.data.entity.RestPeriodConfig
import com.coffeelab.coffeenotes.data.entity.RoastDegree
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RoastDegreeConfigItem(
    val degree: RoastDegree,
    val restDays: Int,
    val peakFlavorDays: Int
)

class RoastDegreeConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))

    val configs: StateFlow<List<RoastDegreeConfigItem>> = combine(
        repository.allRoastDegrees,
        repository.allRestPeriodConfigs,
        repository.allPeakFlavorConfigs
    ) { degrees, restConfigs, peakConfigs ->
        degrees.map { degree ->
            val rest = restConfigs.find { it.roastDegreeId == degree.id }
            val peak = peakConfigs.find { it.roastDegreeId == degree.id }
            RoastDegreeConfigItem(
                degree = degree,
                restDays = rest?.restDays ?: 0,
                peakFlavorDays = peak?.peakFlavorDays ?: 14
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addDegree(name: String) {
        viewModelScope.launch {
            val maxOrder = repository.getMaxRoastDegreeSortOrder() ?: 0
            repository.insertRoastDegree(RoastDegree(name = name, sortOrder = maxOrder + 1))
        }
    }

    fun renameDegree(degree: RoastDegree, newName: String) {
        viewModelScope.launch {
            val oldName = degree.name
            repository.updateRoastDegree(degree.copy(name = newName))
            repository.updateRoastLevelOnBeans(oldName, newName)
        }
    }

    fun deleteDegree(degree: RoastDegree) {
        viewModelScope.launch {
            repository.deleteRoastDegree(degree)
        }
    }

    fun updateRestDays(roastDegreeId: Long, restDays: Int) {
        viewModelScope.launch {
            repository.insertRestPeriodConfig(RestPeriodConfig(roastDegreeId = roastDegreeId, restDays = restDays))
        }
    }

    fun updatePeakFlavorDays(roastDegreeId: Long, peakFlavorDays: Int) {
        viewModelScope.launch {
            repository.insertPeakFlavorConfig(PeakFlavorConfig(roastDegreeId = roastDegreeId, peakFlavorDays = peakFlavorDays))
        }
    }

    fun saveOrder(items: List<RoastDegree>) {
        viewModelScope.launch {
            repository.saveRoastDegreeOrder(items)
        }
    }
}
