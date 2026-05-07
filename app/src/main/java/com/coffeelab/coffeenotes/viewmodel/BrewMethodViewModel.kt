package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.BrewMethod
import com.coffeelab.coffeenotes.data.entity.BrewMethodStep
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BrewMethodViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allMethods = repository.allMethods

    suspend fun getMethod(id: Long) = repository.getMethod(id)

    fun saveMethod(method: BrewMethod) {
        viewModelScope.launch {
            repository.insertMethod(method)
        }
    }

    suspend fun saveMethodSync(method: BrewMethod): Long = repository.insertMethod(method)

    fun updateMethod(method: BrewMethod) {
        viewModelScope.launch {
            repository.updateMethod(method)
        }
    }

    fun deleteMethod(method: BrewMethod) {
        viewModelScope.launch {
            repository.deleteMethod(method)
        }
    }
}
