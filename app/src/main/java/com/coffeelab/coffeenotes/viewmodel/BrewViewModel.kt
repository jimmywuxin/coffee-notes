package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BrewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allRecords = repository.allRecords

    private val _recordsForBean = MutableStateFlow<List<BrewRecord>>(emptyList())
    val recordsForBean: StateFlow<List<BrewRecord>> = _recordsForBean.asStateFlow()

    // ===== Search =====
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isSearching: StateFlow<Boolean> = _searchQuery.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val searchResults: StateFlow<List<BrewRecord>> = _searchQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allRecords
            } else {
                repository.searchRecords(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // ===== Records =====

    private var recordsJob: Job? = null

    fun loadRecordsForBean(beanId: Long) {
        recordsJob?.cancel()
        recordsJob = viewModelScope.launch {
            repository.getRecordsForBean(beanId).collect { records ->
                _recordsForBean.value = records
            }
        }
    }

    suspend fun saveRecord(record: BrewRecord): Long = repository.insertRecord(record)
    suspend fun updateRecord(record: BrewRecord) = repository.updateRecord(record)

    fun deleteRecord(record: BrewRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun getBrewCountForBean(beanId: Long): Flow<Int> = repository.getBrewCountForBean(beanId)
    suspend fun getBestRecordForBean(beanId: Long) = repository.getBestRecordForBean(beanId)
    suspend fun getRecord(id: Long) = repository.getRecord(id)
}
