package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allBeans = repository.allBeans
    val allRecords = repository.allRecords

    // ===== Search =====
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isSearching: StateFlow<Boolean> = _searchQuery.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    /** Mixed results: beans first, then records, each sorted by recency */
    val mixedResults: StateFlow<List<Any>> = _searchQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else combine(
                repository.searchBeansFull(query),
                repository.searchRecords(query)
            ) { beans, records ->
                // Sort beans by updatedAt desc, records by dateTime desc
                val sortedBeans = beans.sortedByDescending { it.updatedAt }
                val sortedRecords = records.sortedByDescending { it.dateTime }
                sortedBeans + sortedRecords
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }
}
