package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.Equipment
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EquipmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allEquipment: StateFlow<List<Equipment>> = repository.allEquipment
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addEquipment(name: String) {
        viewModelScope.launch {
            val maxOrder = repository.getMaxSortOrder() ?: 0
            repository.insertEquipment(
                Equipment(name = name, sortOrder = maxOrder + 1)
            )
        }
    }

    fun updateEquipment(equipment: Equipment) {
        viewModelScope.launch {
            repository.updateEquipment(equipment)
        }
    }

    fun deleteEquipment(equipment: Equipment) {
        viewModelScope.launch {
            repository.deleteEquipment(equipment)
        }
    }

    fun saveEquipmentOrder(items: List<Equipment>) {
        viewModelScope.launch {
            repository.saveEquipmentOrder(items)
        }
    }
}
