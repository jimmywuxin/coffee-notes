package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.Grinder
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GrinderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allGrinders: StateFlow<List<Grinder>> = repository.allGrinders
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addGrinder(name: String) {
        viewModelScope.launch {
            val maxOrder = repository.getMaxGrinderSortOrder() ?: 0
            repository.insertGrinder(
                Grinder(name = name, sortOrder = maxOrder + 1)
            )
        }
    }

    fun updateGrinder(grinder: Grinder) {
        viewModelScope.launch {
            repository.updateGrinder(grinder)
        }
    }

    fun deleteGrinder(grinder: Grinder) {
        viewModelScope.launch {
            repository.deleteGrinder(grinder)
        }
    }
}
