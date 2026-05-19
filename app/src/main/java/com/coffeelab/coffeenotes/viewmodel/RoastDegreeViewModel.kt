package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.RoastDegree
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RoastDegreeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allRoastDegrees: StateFlow<List<RoastDegree>> = repository.allRoastDegrees
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addRoastDegree(name: String) {
        viewModelScope.launch {
            val maxOrder = repository.getMaxRoastDegreeSortOrder() ?: 0
            repository.insertRoastDegree(RoastDegree(name = name, sortOrder = maxOrder + 1))
        }
    }

    fun updateRoastDegree(roastDegree: RoastDegree) {
        viewModelScope.launch {
            repository.updateRoastDegree(roastDegree)
        }
    }

    fun renameRoastDegree(roastDegree: RoastDegree, newName: String) {
        viewModelScope.launch {
            val oldName = roastDegree.name
            repository.updateRoastDegree(roastDegree.copy(name = newName))
            repository.updateRoastLevelOnBeans(oldName, newName)
        }
    }

    fun deleteRoastDegree(roastDegree: RoastDegree) {
        viewModelScope.launch { repository.deleteRoastDegree(roastDegree) }
    }

    fun saveOrder(items: List<RoastDegree>) {
        viewModelScope.launch { repository.saveRoastDegreeOrder(items) }
    }
}
