package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.BrewRecipe
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allRecipes = repository.allRecipes

    suspend fun getRecipe(id: Long) = repository.getRecipe(id)

    fun getRecipesByEquipment(equipment: String) = repository.getRecipesByEquipment(equipment)

    fun saveRecipe(recipe: BrewRecipe) {
        viewModelScope.launch {
            repository.insertRecipe(recipe)
        }
    }

    suspend fun saveRecipeSync(recipe: BrewRecipe): Long = repository.insertRecipe(recipe)

    fun updateRecipe(recipe: BrewRecipe) {
        viewModelScope.launch {
            repository.updateRecipe(recipe)
        }
    }

    fun deleteRecipe(recipe: BrewRecipe) {
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
        }
    }

    // Equipment management
    private val _equipmentList = MutableStateFlow<List<String>>(emptyList())
    val equipmentList: StateFlow<List<String>> = _equipmentList.asStateFlow()

    fun loadEquipment() {
        viewModelScope.launch {
            repository.allEquipment.collect { equipment ->
                _equipmentList.value = equipment.map { it.name }
            }
        }
    }
}
