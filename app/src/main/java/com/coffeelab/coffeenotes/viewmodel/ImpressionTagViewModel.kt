package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.ImpressionTag
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ImpressionTagViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allImpressionTags: StateFlow<List<ImpressionTag>> = repository.allImpressionTags
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addTag(name: String) {
        viewModelScope.launch {
            val maxOrder = repository.getMaxImpressionTagSortOrder() ?: 0
            repository.insertImpressionTag(
                ImpressionTag(name = name, sortOrder = maxOrder + 1)
            )
        }
    }

    fun updateTag(tag: ImpressionTag) {
        viewModelScope.launch {
            repository.updateImpressionTag(tag)
        }
    }

    fun deleteTag(tag: ImpressionTag) {
        viewModelScope.launch {
            repository.deleteImpressionTag(tag)
        }
    }

    fun saveOrder(items: List<ImpressionTag>) {
        viewModelScope.launch {
            repository.saveImpressionTagOrder(items)
        }
    }
}
