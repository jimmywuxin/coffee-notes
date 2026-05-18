package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.ProcessMethod
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProcessMethodViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allProcessMethods: StateFlow<List<ProcessMethod>> = repository.allProcessMethods
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addProcessMethod(name: String) {
        viewModelScope.launch {
            val maxOrder = repository.getMaxProcessMethodSortOrder() ?: 0
            repository.insertProcessMethod(processMethod = ProcessMethod(name = name, sortOrder = maxOrder + 1))
        }
    }

    fun updateProcessMethod(processMethod: ProcessMethod) {
        viewModelScope.launch { repository.updateProcessMethod(processMethod) }
    }

    fun deleteProcessMethod(processMethod: ProcessMethod) {
        viewModelScope.launch { repository.deleteProcessMethod(processMethod) }
    }

    fun saveOrder(items: List<ProcessMethod>) {
        viewModelScope.launch { repository.saveProcessMethodOrder(items) }
    }
}
