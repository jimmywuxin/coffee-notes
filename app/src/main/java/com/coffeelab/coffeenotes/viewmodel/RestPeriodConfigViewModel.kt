package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.RestPeriodConfig
import com.coffeelab.coffeenotes.data.entity.RoastDegree
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RestPeriodConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allConfigs: StateFlow<List<RestPeriodConfig>> = repository.allRestPeriodConfigs
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allRoastDegrees: StateFlow<List<RoastDegree>> = repository.allRoastDegrees
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addOrUpdateConfig(roastDegreeId: Long, restDays: Int) {
        viewModelScope.launch { repository.insertRestPeriodConfig(RestPeriodConfig(roastDegreeId = roastDegreeId, restDays = restDays)) }
    }

    fun updateConfig(config: RestPeriodConfig) {
        viewModelScope.launch { repository.updateRestPeriodConfig(config) }
    }

    fun deleteConfig(config: RestPeriodConfig) {
        viewModelScope.launch { repository.deleteRestPeriodConfig(config) }
    }
}
