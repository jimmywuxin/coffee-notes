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
    private val beanDao = AppDatabase.getInstance(application).coffeeBeanDao()

    private val _beanId = MutableStateFlow<Long?>(null)

    val records: StateFlow<List<PurchaseRecord>> = _beanId
        .filterNotNull()
        .flatMapLatest { dao.getByBeanId(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun loadForBean(beanId: Long) {
        _beanId.value = beanId
    }

    suspend fun insert(record: PurchaseRecord): Long {
        val id = dao.insert(record)
        // 如果记录有烘焙日期，同步到豆子
        record.roastDate?.let { roastDate ->
            syncRoastDateToBean(record.beanId, roastDate)
        }
        return id
    }

    suspend fun update(record: PurchaseRecord) {
        dao.update(record)
        // 如果记录有烘焙日期，同步到豆子
        record.roastDate?.let { roastDate ->
            syncRoastDateToBean(record.beanId, roastDate)
        }
    }

    suspend fun delete(record: PurchaseRecord) = dao.delete(record)

    private suspend fun syncRoastDateToBean(beanId: Long, roastDate: Long) {
        beanDao.getBeanById(beanId)?.let { bean ->
            // 只在豆子当前没有烘焙日期时自动填入，或有新日期时更新
            if (bean.roastDate == null || roastDate > 0) {
                beanDao.update(bean.copy(roastDate = roastDate))
            }
        }
    }
}
