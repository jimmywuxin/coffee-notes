package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.dao.*
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = CoffeeRepository(db)

    // ===== Time Ranges =====
    private val twelveMonthsAgo: Long
        get() {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -12)
            return cal.timeInMillis
        }

    private val weekStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private val lastWeekStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.timeInMillis = weekStart
            cal.add(Calendar.WEEK_OF_YEAR, -1)
            return cal.timeInMillis
        }

    // ===== Bean Stats =====
    val allBeans: Flow<List<CoffeeBean>> = db.coffeeBeanDao().getAllBeans()

    val beanCount: StateFlow<Int> = allBeans.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val roasterCount: StateFlow<Int> = allBeans
        .map { beans -> beans.map { it.roaster }.filter { it.isNotEmpty() }.distinct().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val originCount: StateFlow<Int> = allBeans
        .map { beans -> beans.map { it.origin }.filter { it.isNotEmpty() }.distinct().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ===== Brew Trend =====
    val monthlyBrewCounts: Flow<List<Int>> = db.brewRecordDao().getMonthlyBrewCounts(twelveMonthsAgo)

    val thisWeekCount: Flow<Int> = db.brewRecordDao().getBrewCountThisWeek(weekStart)

    val lastWeekCount: Flow<Int> = db.brewRecordDao().getBrewCountLastWeek(lastWeekStart, weekStart)

    // ===== Brew Habits =====
    val equipmentCounts: Flow<List<EquipmentCount>> = db.brewRecordDao().getBrewCountsByEquipment()

    val ratioCounts: Flow<List<RatioCount>> = db.brewRecordDao().getBrewCountsByRatio()

    val tempCounts: Flow<List<TempBucket>> = db.brewRecordDao().getBrewCountsByTemp()

    // ===== Rating Analysis =====
    val ratingCounts: Flow<List<RatingCount>> = db.brewRecordDao().getBrewCountsByRating()

    val avgRatingByEquipment: Flow<List<EquipmentRating>> = db.brewRecordDao().getAvgRatingByEquipment()

    val overallAvgRating: Flow<Double> = db.brewRecordDao().getAllRecords()
        .map { records ->
            val rated = records.filter { it.overallRating > 0 }
            if (rated.isEmpty()) 0.0 else rated.map { it.overallRating }.average()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ===== Bean Stats =====
    val topBrewedBeans: Flow<List<BeanBrewCount>> = db.coffeeBeanDao().getTopBrewedBeans(10)

    val beanCountByOrigin: Flow<List<OriginCount>> = db.brewRecordDao().getBeanCountByOrigin()

    val beanCountByRoastLevel: Flow<List<RoastLevelCount>> = db.brewRecordDao().getBeanCountByRoastLevel()

    // ===== Flavor Tags =====
    fun getTopFlavorTags(limit: Int = 10) = db.flavorTagDao().getTopFlavorTags(limit)

    // ===== All Records =====
    val allRecords = repository.allRecords

    val totalBrewCount: StateFlow<Int> = allRecords.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ===== Per-Bean Stats =====
    fun getRecordsForBean(beanId: Long) = repository.getRecordsForBean(beanId)

    fun getMonthlyBrewCountsForBean(beanId: Long) =
        db.brewRecordDao().getMonthlyBrewCountsForBean(beanId, twelveMonthsAgo)

    fun getBrewCountForBean(beanId: Long) = repository.getBrewCountForBean(beanId)

    suspend fun getBestRecordForBean(beanId: Long) = repository.getBestRecordForBean(beanId)
}
