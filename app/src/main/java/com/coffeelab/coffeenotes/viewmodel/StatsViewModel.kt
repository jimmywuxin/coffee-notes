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

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))

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
    val allBeans: Flow<List<CoffeeBean>> = repository.allBeans

    val beanCount: StateFlow<Int> = allBeans.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val roasterCount: StateFlow<Int> = allBeans
        .map { beans -> beans.map { it.roaster }.filter { it.isNotEmpty() }.distinct().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val originCount: StateFlow<Int> = allBeans
        .map { beans -> beans.map { it.origin }.filter { it.isNotEmpty() }.distinct().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ===== Brew Trend =====
    val monthlyBrewCounts: Flow<List<Int>> = repository.getMonthlyBrewCounts(twelveMonthsAgo)
    val thisWeekCount: Flow<Int> = repository.getBrewCountThisWeek(weekStart)
    val lastWeekCount: Flow<Int> = repository.getBrewCountLastWeek(lastWeekStart, weekStart)

    // ===== Brew Habits =====
    val equipmentCounts: Flow<List<EquipmentCount>> = repository.getBrewCountsByEquipment()
    val ratioCounts: Flow<List<RatioCount>> = repository.getBrewCountsByRatio()
    val tempCounts: Flow<List<TempBucket>> = repository.getBrewCountsByTemp()
    val timeSlotCounts: Flow<List<TimeSlotCount>> = repository.getBrewCountsByTimeSlot()

    // ===== Rating Analysis =====
    val ratingCounts: Flow<List<RatingCount>> = repository.getBrewCountsByRating()
    val avgRatingByEquipment: Flow<List<EquipmentRating>> = repository.getAvgRatingByEquipment()

    val overallAvgRating: Flow<Double> = repository.allRecords
        .map { records ->
            val rated = records.filter { it.overallRating > 0 }
            if (rated.isEmpty()) 0.0 else rated.map { it.overallRating }.average()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ===== Bean Distribution =====
    val topBrewedBeans: Flow<List<BeanBrewCount>> = repository.getTopBrewedBeans(10)
    val beanCountByOrigin: Flow<List<OriginCount>> = repository.getBeanCountByOrigin()
    val beanCountByRoastLevel: Flow<List<RoastLevelCount>> = repository.getBeanCountByRoastLevel()

    // ===== Flavor Tags =====
    fun getTopFlavorTags(limit: Int = 10) = repository.getTopFlavorTags(limit)

    // ===== All Records =====
    val allRecords = repository.allRecords

    val totalBrewCount: StateFlow<Int> = allRecords.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ===== Per-Bean Stats =====
    fun getRecordsForBean(beanId: Long) = repository.getRecordsForBean(beanId)
    fun getMonthlyBrewCountsForBean(beanId: Long) = repository.getMonthlyBrewCountsForBean(beanId, twelveMonthsAgo)
    fun getBrewCountForBean(beanId: Long) = repository.getBrewCountForBean(beanId)
    suspend fun getBestRecordForBean(beanId: Long) = repository.getBestRecordForBean(beanId)

    // ===== Per-Bean Brew Habits =====
    fun getEquipmentCountsForBean(beanId: Long) = repository.getBrewCountsByEquipmentForBean(beanId)
    fun getRatioCountsForBean(beanId: Long) = repository.getBrewCountsByRatioForBean(beanId)
    fun getTempCountsForBean(beanId: Long) = repository.getBrewCountsByTempForBean(beanId)
    fun getTimeSlotCountsForBean(beanId: Long) = repository.getBrewCountsByTimeSlotForBean(beanId)
    fun getRatingCountsForBean(beanId: Long) = repository.getBrewCountsByRatingForBean(beanId)
    fun getAvgRatingForBean(beanId: Long) = repository.getAvgRatingForBean(beanId)
}
