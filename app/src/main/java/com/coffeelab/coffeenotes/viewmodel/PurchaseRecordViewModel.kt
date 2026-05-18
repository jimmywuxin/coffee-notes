package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.PurchaseRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PurchaseRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).purchaseRecordDao()

    private val _beanId = MutableStateFlow<Long?>(null)

    val records: StateFlow<List<PurchaseRecord>> = _beanId
        .filterNotNull()
        .flatMapLatest { dao.getByBeanId(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun loadForBean(beanId: Long) {
        _beanId.value = beanId
    }

    suspend fun insert(record: PurchaseRecord): Long = dao.insert(record)

    suspend fun update(record: PurchaseRecord) = dao.update(record)

    suspend fun delete(record: PurchaseRecord) = dao.delete(record)
}
