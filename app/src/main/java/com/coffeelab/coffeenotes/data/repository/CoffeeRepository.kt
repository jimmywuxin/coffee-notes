package com.coffeelab.coffeenotes.data.repository

import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.*
import kotlinx.coroutines.flow.Flow

class CoffeeRepository(private val db: AppDatabase) {

    // ===== Coffee Beans =====
    val allBeans: Flow<List<CoffeeBean>> = db.coffeeBeanDao().getAllBeans()

    suspend fun getBean(id: Long) = db.coffeeBeanDao().getBeanById(id)
    fun getBeanFlow(id: Long) = db.coffeeBeanDao().getBeanFlow(id)
    fun searchBeans(query: String) = db.coffeeBeanDao().searchBeans(query)

    suspend fun insertBean(bean: CoffeeBean): Long = db.coffeeBeanDao().insert(bean)
    suspend fun updateBean(bean: CoffeeBean) = db.coffeeBeanDao().update(bean)
    suspend fun deleteBean(bean: CoffeeBean) = db.coffeeBeanDao().delete(bean)
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
    fun getRecordsByEquipment(equipment: String) = db.brewRecordDao().getRecordsByEquipment(equipment)
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

    suspend fun saveEquipmentOrder(items: List<Equipment>) {
        // Update sortOrder for each item without deleting
        items.forEachIndexed { index, equipment ->
            if (equipment.id > 0) {
                db.equipmentDao().update(equipment.copy(sortOrder = index))
            }
        }
    }

    suspend fun saveGrinderOrder(items: List<Grinder>) {
        items.forEachIndexed { index, grinder ->
            if (grinder.id > 0) {
                db.grinderDao().update(grinder.copy(sortOrder = index))
            }
        }
    }
}
