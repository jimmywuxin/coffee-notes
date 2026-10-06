package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.PeakFlavorConfig
import com.coffeelab.coffeenotes.data.entity.RestPeriodConfig
import com.coffeelab.coffeenotes.data.entity.RoastDegree
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RoastDegreeConfigItem(
    val degree: RoastDegree,
    val restDays: Int,
    val peakFlavorDays: Int
)

class RoastDegreeConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))

    init {
        // 一次性折叠历史重复行（修updateRestDays 把每次编辑都插成新行）。
        // 放在 ViewModel init 而不是 Room migration：只是清冗余行，不改 schema，不需要升版本。
        viewModelScope.launch {
            repository.collapseRestPeriodConfigs()
            repository.collapsePeakFlavorConfigs()
        }
    }

    val configs: StateFlow<List<RoastDegreeConfigItem>> = combine(
        repository.allRoastDegrees,
        repository.allRestPeriodConfigs,
        repository.allPeakFlavorConfigs
    ) { degrees, restConfigs, peakConfigs ->
        degrees.map { degree ->
            // lastOrNull 而非 find：折叠后每组唯一，等价；未折叠时取最后写入的那条（= 用户最后设置的值）
            val rest = restConfigs.lastOrNull { it.roastDegreeId == degree.id }
            val peak = peakConfigs.lastOrNull { it.roastDegreeId == degree.id }
            RoastDegreeConfigItem(
                degree = degree,
                restDays = rest?.restDays ?: 0,
                peakFlavorDays = peak?.peakFlavorDays ?: 14
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addDegree(name: String) {
        viewModelScope.launch {
            val maxOrder = repository.getMaxRoastDegreeSortOrder() ?: 0
            repository.insertRoastDegree(RoastDegree(name = name, sortOrder = maxOrder + 1))
        }
    }

    fun renameDegree(degree: RoastDegree, newName: String) {
        viewModelScope.launch {
            val oldName = degree.name
            repository.updateRoastDegree(degree.copy(name = newName))
            repository.updateRoastLevelOnBeans(oldName, newName)
        }
    }

    fun deleteDegree(degree: RoastDegree) {
        viewModelScope.launch {
            repository.deleteRoastDegree(degree)
        }
    }

    // RestPeriodConfig / PeakFlavorConfig 的 id 是 autoGenerate，roastDegreeId 上没有 unique 索引，
    // 直接 insert 会每次新增一行；而读取侧是 find{} 取 ORDER BY id ASC 的第一条（即最旧那行），
    // 于是「编辑」永远不生效。必须先查再决定 insert / update。
    fun updateRestDays(roastDegreeId: Long, restDays: Int) {
        viewModelScope.launch {
            val existing = repository.getRestPeriodConfigByRoastDegreeId(roastDegreeId)
            if (existing == null) {
                repository.insertRestPeriodConfig(RestPeriodConfig(roastDegreeId = roastDegreeId, restDays = restDays))
            } else {
                repository.updateRestPeriodConfig(existing.copy(restDays = restDays))
            }
        }
    }

    fun updatePeakFlavorDays(roastDegreeId: Long, peakFlavorDays: Int) {
        viewModelScope.launch {
            val existing = repository.getPeakFlavorConfigByRoastDegreeId(roastDegreeId)
            if (existing == null) {
                repository.insertPeakFlavorConfig(PeakFlavorConfig(roastDegreeId = roastDegreeId, peakFlavorDays = peakFlavorDays))
            } else {
                repository.updatePeakFlavorConfig(existing.copy(peakFlavorDays = peakFlavorDays))
            }
        }
    }

    fun saveOrder(items: List<RoastDegree>) {
        viewModelScope.launch {
            repository.saveRoastDegreeOrder(items)
        }
    }
}
