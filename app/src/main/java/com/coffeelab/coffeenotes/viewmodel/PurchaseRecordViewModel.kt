package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.PurchaseRecord
import com.coffeelab.coffeenotes.data.entity.RoastDegree
import com.coffeelab.coffeenotes.data.entity.RestPeriodConfig
import com.coffeelab.coffeenotes.data.entity.PeakFlavorConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PurchaseRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).purchaseRecordDao()
    private val beanDao = AppDatabase.getInstance(application).coffeeBeanDao()
    private val roastDegreeDao = AppDatabase.getInstance(application).roastDegreeDao()
    private val restPeriodConfigDao = AppDatabase.getInstance(application).restPeriodConfigDao()
    private val peakFlavorConfigDao = AppDatabase.getInstance(application).peakFlavorConfigDao()

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
            // 当豆子没有烘焙日期，或有新日期，或缺少养豆期/赏味期时，自动同步并填充
            if (bean.roastDate == null || roastDate > 0 || bean.restDays == null || bean.peakFlavorDays == null) {
                // 根据烘焙度名称查找配置，自动填充养豆期/赏味期天数
                var restDays: Int? = bean.restDays
                var peakFlavorDays: Int? = bean.peakFlavorDays
                if (bean.roastLevel.isNotEmpty()) {
                    val roastDegree = roastDegreeDao.getAllOnce().find { it.name == bean.roastLevel }
                    if (roastDegree != null) {
                        if (restDays == null) {
                            restDays = restPeriodConfigDao.getByRoastDegreeId(roastDegree.id)?.restDays
                        }
                        if (peakFlavorDays == null) {
                            peakFlavorDays = peakFlavorConfigDao.getByRoastDegreeId(roastDegree.id)?.peakFlavorDays
                        }
                    }
                }
                beanDao.update(bean.copy(
                    roastDate = roastDate,
                    restDays = restDays,
                    peakFlavorDays = peakFlavorDays
                ))
            }
        }
    }
}
