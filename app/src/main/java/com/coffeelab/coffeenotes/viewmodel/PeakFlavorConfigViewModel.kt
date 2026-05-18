package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.PeakFlavorConfig
import com.coffeelab.coffeenotes.data.entity.RoastDegree
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PeakFlavorConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allConfigs: StateFlow<List<PeakFlavorConfig>> = repository.allPeakFlavorConfigs
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allRoastDegrees: StateFlow<List<RoastDegree>> = repository.allRoastDegrees
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addOrUpdateConfig(roastDegreeId: Long, peakFlavorDays: Int) {
        viewModelScope.launch { repository.insertPeakFlavorConfig(PeakFlavorConfig(roastDegreeId = roastDegreeId, peakFlavorDays = peakFlavorDays)) }
    }

    fun updateConfig(config: PeakFlavorConfig) {
        viewModelScope.launch { repository.updatePeakFlavorConfig(config) }
    }

    fun deleteConfig(config: PeakFlavorConfig) {
        viewModelScope.launch { repository.deletePeakFlavorConfig(config) }
    }
}
