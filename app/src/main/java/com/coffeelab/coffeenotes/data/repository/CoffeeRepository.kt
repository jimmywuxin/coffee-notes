package com.coffeelab.coffeenotes.data.repository

import androidx.room.Transaction
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.*
import kotlinx.coroutines.flow.Flow
import com.coffeelab.coffeenotes.data.dao.BeanBrewCount
import com.coffeelab.coffeenotes.data.dao.EquipmentCount
import com.coffeelab.coffeenotes.data.dao.RatioCount
import com.coffeelab.coffeenotes.data.dao.TempBucket
import com.coffeelab.coffeenotes.data.dao.RatingCount
import com.coffeelab.coffeenotes.data.dao.EquipmentRating
import com.coffeelab.coffeenotes.data.dao.OriginCount
import com.coffeelab.coffeenotes.data.dao.RoastLevelCount
import com.coffeelab.coffeenotes.data.dao.TimeSlotCount
import com.coffeelab.coffeenotes.data.dao.FlavorTagCount

class CoffeeRepository(private val db: AppDatabase) {

    // ===== Coffee Beans =====
    val allBeans: Flow<List<CoffeeBean>> = db.coffeeBeanDao().getAllBeans()
    val activeBeans: Flow<List<CoffeeBean>> = db.coffeeBeanDao().getActiveBeans()
    val archivedBeans: Flow<List<CoffeeBean>> = db.coffeeBeanDao().getArchivedBeans()

    suspend fun getBean(id: Long) = db.coffeeBeanDao().getBeanById(id)
    fun getBeanFlow(id: Long) = db.coffeeBeanDao().getBeanFlow(id)
    fun searchBeans(query: String) = db.coffeeBeanDao().searchBeans(query)

    suspend fun insertBean(bean: CoffeeBean): Long = db.coffeeBeanDao().insert(bean)
    suspend fun updateBean(bean: CoffeeBean) = db.coffeeBeanDao().update(bean)
    suspend fun deleteBean(bean: CoffeeBean) = db.coffeeBeanDao().delete(bean)
    suspend fun archiveBean(bean: CoffeeBean) = db.coffeeBeanDao().update(bean.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
    suspend fun unarchiveBean(bean: CoffeeBean) = db.coffeeBeanDao().update(bean.copy(isArchived = false, updatedAt = System.currentTimeMillis()))
    @Transaction
    suspend fun saveBeanOrder(items: List<CoffeeBean>) {
        items.forEachIndexed { index, bean ->
            if (bean.id > 0) {
                db.coffeeBeanDao().update(bean.copy(sortOrder = index))
            }
        }
    }

    // ===== Flavor Tags =====
    fun getTagsForBean(beanId: Long) = db.flavorTagDao().getTagsForBean(beanId)
    suspend fun getTagsForBeanOnce(beanId: Long) = db.flavorTagDao().getTagsForBeanOnce(beanId)

    suspend fun saveTagsForBean(beanId: Long, tags: List<String>) {
        db.flavorTagDao().deleteAllForBean(beanId)
        val entities = tags.map { FlavorTag(beanId = beanId, name = it) }
        if (entities.isNotEmpty()) {
            db.flavorTagDao().insertAll(entities)
        }
    }

    suspend fun addTag(tag: FlavorTag) = db.flavorTagDao().insert(tag)
    suspend fun deleteTag(tag: FlavorTag) = db.flavorTagDao().delete(tag)

    // ===== Brew Records =====
    val allRecords: Flow<List<BrewRecord>> = db.brewRecordDao().getAllRecords()

    suspend fun getRecord(id: Long) = db.brewRecordDao().getRecordById(id)
    fun getRecordsForBean(beanId: Long) = db.brewRecordDao().getRecordsForBean(beanId)
    fun getRecordsByEquipmentId(equipmentId: Long) = db.brewRecordDao().getRecordsByEquipmentId(equipmentId)
    fun getBrewCountForBean(beanId: Long) = db.brewRecordDao().getBrewCountForBean(beanId)
    suspend fun getBestRecordForBean(beanId: Long) = db.brewRecordDao().getBestRecordForBean(beanId)

    suspend fun insertRecord(record: BrewRecord): Long = db.brewRecordDao().insert(record)
    suspend fun updateRecord(record: BrewRecord) = db.brewRecordDao().update(record)
    suspend fun deleteRecord(record: BrewRecord) = db.brewRecordDao().delete(record)

    // ===== Brew Methods =====
    val allMethods: Flow<List<BrewMethod>> = db.brewMethodDao().getAllMethods()

    suspend fun getMethod(id: Long) = db.brewMethodDao().getMethodById(id)

    suspend fun insertMethod(method: BrewMethod): Long = db.brewMethodDao().insert(method)
    suspend fun updateMethod(method: BrewMethod) = db.brewMethodDao().update(method)
    suspend fun deleteMethod(method: BrewMethod) = db.brewMethodDao().delete(method)
    @Transaction
    suspend fun saveMethodOrder(items: List<BrewMethod>) {
        items.forEachIndexed { index, method ->
            if (method.id > 0) {
                db.brewMethodDao().updateSortOrder(method.id, index)
            }
        }
    }

    // ===== Equipment =====
    val allEquipment: Flow<List<Equipment>> = db.equipmentDao().getAll()
    suspend fun getAllEquipmentOnce() = db.equipmentDao().getAllOnce()

    suspend fun insertEquipment(equipment: Equipment): Long = db.equipmentDao().insert(equipment)
    suspend fun updateEquipment(equipment: Equipment) = db.equipmentDao().update(equipment)
    suspend fun deleteEquipment(equipment: Equipment) = db.equipmentDao().delete(equipment)
    suspend fun getMaxSortOrder() = db.equipmentDao().getMaxSortOrder()

    // ===== Grinder =====
    val allGrinders: Flow<List<Grinder>> = db.grinderDao().getAll()
    suspend fun getAllGrindersOnce() = db.grinderDao().getAllOnce()

    suspend fun insertGrinder(grinder: Grinder): Long = db.grinderDao().insert(grinder)
    suspend fun updateGrinder(grinder: Grinder) = db.grinderDao().update(grinder)
    suspend fun deleteGrinder(grinder: Grinder) = db.grinderDao().delete(grinder)
    suspend fun getMaxGrinderSortOrder() = db.grinderDao().getMaxSortOrder()

    @Transaction
    suspend fun saveEquipmentOrder(items: List<Equipment>) {
        // Update sortOrder for each item without deleting
        items.forEachIndexed { index, equipment ->
            if (equipment.id > 0) {
                db.equipmentDao().update(equipment.copy(sortOrder = index))
            }
        }
    }

    @Transaction
    suspend fun saveGrinderOrder(items: List<Grinder>) {
        items.forEachIndexed { index, grinder ->
            if (grinder.id > 0) {
                db.grinderDao().update(grinder.copy(sortOrder = index))
            }
        }
    }

    // ===== Roast Degrees =====
    val allRoastDegrees: Flow<List<RoastDegree>> = db.roastDegreeDao().getAll()
    suspend fun getAllRoastDegreesOnce() = db.roastDegreeDao().getAllOnce()
    suspend fun getRoastDegreeById(id: Long) = db.roastDegreeDao().getById(id)
    suspend fun insertRoastDegree(roastDegree: RoastDegree): Long = db.roastDegreeDao().insert(roastDegree)
    suspend fun updateRoastDegree(roastDegree: RoastDegree) = db.roastDegreeDao().update(roastDegree)
    suspend fun deleteRoastDegree(roastDegree: RoastDegree) = db.roastDegreeDao().delete(roastDegree)
    suspend fun getMaxRoastDegreeSortOrder() = db.roastDegreeDao().getMaxSortOrder()

    @Transaction
    suspend fun saveRoastDegreeOrder(items: List<RoastDegree>) {
        items.forEachIndexed { index, roastDegree ->
            if (roastDegree.id > 0) {
                db.roastDegreeDao().update(roastDegree.copy(sortOrder = index))
            }
        }
    }

    // ===== Process Methods =====
    val allProcessMethods: Flow<List<ProcessMethod>> = db.processMethodDao().getAll()
    suspend fun getAllProcessMethodsOnce() = db.processMethodDao().getAllOnce()
    suspend fun insertProcessMethod(processMethod: ProcessMethod): Long = db.processMethodDao().insert(processMethod)
    suspend fun updateProcessMethod(processMethod: ProcessMethod) = db.processMethodDao().update(processMethod)
    suspend fun deleteProcessMethod(processMethod: ProcessMethod) = db.processMethodDao().delete(processMethod)
    suspend fun getMaxProcessMethodSortOrder() = db.processMethodDao().getMaxSortOrder()

    @Transaction
    suspend fun saveProcessMethodOrder(items: List<ProcessMethod>) {
        items.forEachIndexed { index, processMethod ->
            if (processMethod.id > 0) {
                db.processMethodDao().update(processMethod.copy(sortOrder = index))
            }
        }
    }

    // ===== Rest Period Configs =====
    val allRestPeriodConfigs: Flow<List<RestPeriodConfig>> = db.restPeriodConfigDao().getAll()
    suspend fun getAllRestPeriodConfigsOnce() = db.restPeriodConfigDao().getAllOnce()
    suspend fun getRestPeriodConfigByRoastDegreeId(roastDegreeId: Long): RestPeriodConfig? = db.restPeriodConfigDao().getByRoastDegreeId(roastDegreeId)
    suspend fun insertRestPeriodConfig(config: RestPeriodConfig): Long = db.restPeriodConfigDao().insert(config)
    suspend fun updateRestPeriodConfig(config: RestPeriodConfig) = db.restPeriodConfigDao().update(config)
    suspend fun deleteRestPeriodConfig(config: RestPeriodConfig) = db.restPeriodConfigDao().delete(config)

    // ===== Peak Flavor Configs =====
    val allPeakFlavorConfigs: Flow<List<PeakFlavorConfig>> = db.peakFlavorConfigDao().getAll()
    suspend fun getAllPeakFlavorConfigsOnce() = db.peakFlavorConfigDao().getAllOnce()
    suspend fun getPeakFlavorConfigByRoastDegreeId(roastDegreeId: Long): PeakFlavorConfig? = db.peakFlavorConfigDao().getByRoastDegreeId(roastDegreeId)
    suspend fun insertPeakFlavorConfig(config: PeakFlavorConfig): Long = db.peakFlavorConfigDao().insert(config)
    suspend fun updatePeakFlavorConfig(config: PeakFlavorConfig) = db.peakFlavorConfigDao().update(config)
    suspend fun deletePeakFlavorConfig(config: PeakFlavorConfig) = db.peakFlavorConfigDao().delete(config)

    // ===== Purchase Records =====
    suspend fun getAllPurchaseRecordsOnce() = db.purchaseRecordDao().getAllOnce()
    suspend fun insertPurchaseRecord(record: PurchaseRecord): Long = db.purchaseRecordDao().insert(record)
    suspend fun deleteAllPurchaseRecords() = db.purchaseRecordDao().deleteAll()

    // ===== Stats queries (previously directly on DAOs in StatsViewModel) =====
    fun getTopBrewedBeans(limit: Int) = db.coffeeBeanDao().getTopBrewedBeans(limit)
    fun getMonthlyBrewCounts(startTime: Long) = db.brewRecordDao().getMonthlyBrewCounts(startTime)
    fun getBrewCountThisWeek(weekStart: Long) = db.brewRecordDao().getBrewCountThisWeek(weekStart)
    fun getBrewCountLastWeek(lastWeekStart: Long, weekStart: Long) = db.brewRecordDao().getBrewCountLastWeek(lastWeekStart, weekStart)
    fun getBrewCountsByEquipment() = db.brewRecordDao().getBrewCountsByEquipment()
    fun getBrewCountsByRatio() = db.brewRecordDao().getBrewCountsByRatio()
    fun getBrewCountsByTemp() = db.brewRecordDao().getBrewCountsByTemp()
    fun getBrewCountsByTimeSlot() = db.brewRecordDao().getBrewCountsByTimeSlot()
    fun getBrewCountsByRating() = db.brewRecordDao().getBrewCountsByRating()
    fun getAvgRatingByEquipment() = db.brewRecordDao().getAvgRatingByEquipment()
    fun getBeanCountByOrigin() = db.brewRecordDao().getBeanCountByOrigin()
    fun getBeanCountByRoastLevel() = db.brewRecordDao().getBeanCountByRoastLevel()
    fun getTopFlavorTags(limit: Int) = db.flavorTagDao().getTopFlavorTags(limit)
    fun getMonthlyBrewCountsForBean(beanId: Long, startTime: Long) = db.brewRecordDao().getMonthlyBrewCountsForBean(beanId, startTime)
    fun getBrewCountsByEquipmentForBean(beanId: Long) = db.brewRecordDao().getBrewCountsByEquipmentForBean(beanId)
    fun getBrewCountsByRatioForBean(beanId: Long) = db.brewRecordDao().getBrewCountsByRatioForBean(beanId)
    fun getBrewCountsByTempForBean(beanId: Long) = db.brewRecordDao().getBrewCountsByTempForBean(beanId)
    fun getBrewCountsByTimeSlotForBean(beanId: Long) = db.brewRecordDao().getBrewCountsByTimeSlotForBean(beanId)
    fun getBrewCountsByRatingForBean(beanId: Long) = db.brewRecordDao().getBrewCountsByRatingForBean(beanId)
    fun getAvgRatingForBean(beanId: Long) = db.brewRecordDao().getAvgRatingForBean(beanId)

    suspend fun updateRoastLevelOnBeans(oldName: String, newName: String) = db.coffeeBeanDao().updateRoastLevelByName(oldName, newName)
    suspend fun updateProcessOnBeans(oldName: String, newName: String) = db.coffeeBeanDao().updateProcessByName(oldName, newName)
}
