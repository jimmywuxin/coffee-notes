package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeBeanDao {
    @Query("SELECT * FROM coffee_beans ORDER BY sortOrder ASC, updatedAt DESC")
    fun getAllBeans(): Flow<List<CoffeeBean>>

    @Query("SELECT * FROM coffee_beans WHERE isArchived = 0 ORDER BY sortOrder ASC, updatedAt DESC")
    fun getActiveBeans(): Flow<List<CoffeeBean>>

    @Query("SELECT * FROM coffee_beans WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedBeans(): Flow<List<CoffeeBean>>

    @Query("SELECT * FROM coffee_beans WHERE id = :id")
    suspend fun getBeanById(id: Long): CoffeeBean?

    @Query("SELECT * FROM coffee_beans WHERE id = :id")
    fun getBeanFlow(id: Long): Flow<CoffeeBean?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bean: CoffeeBean): Long

    @Update
    suspend fun update(bean: CoffeeBean)

    @Delete
    suspend fun delete(bean: CoffeeBean)

    @Query("SELECT * FROM coffee_beans WHERE name LIKE '%' || :query || '%' OR roaster LIKE '%' || :query || '%' OR origin LIKE '%' || :query || '%'")
    fun searchBeans(query: String): Flow<List<CoffeeBean>>

    /** Full-text search across bean fields + impression tags */
    @Query("""
        SELECT DISTINCT cb.* FROM coffee_beans cb
        LEFT JOIN bean_impression_tags bit ON cb.id = bit.beanId
        LEFT JOIN impression_tags it ON bit.tagId = it.id
        WHERE cb.roaster LIKE '%' || :query || '%'
           OR cb.name LIKE '%' || :query || '%'
           OR cb.origin LIKE '%' || :query || '%'
           OR cb.region LIKE '%' || :query || '%'
           OR cb.estate LIKE '%' || :query || '%'
           OR cb.variety LIKE '%' || :query || '%'
           OR cb.process LIKE '%' || :query || '%'
           OR cb.roastLevel LIKE '%' || :query || '%'
           OR cb.notes LIKE '%' || :query || '%'
           OR it.name LIKE '%' || :query || '%'
        ORDER BY cb.updatedAt DESC
    """)
    fun searchBeansFull(query: String): Flow<List<CoffeeBean>>

    @Query("DELETE FROM coffee_beans")
    suspend fun deleteAll()

    /** Top brewed beans */
    @Query("""
        SELECT cb.id, cb.roaster, cb.name, cb.origin, COUNT(br.id) as brewCount
        FROM coffee_beans cb
        LEFT JOIN brew_records br ON cb.id = br.beanId
        GROUP BY cb.id
        ORDER BY brewCount DESC
        LIMIT :limit
    """)
    fun getTopBrewedBeans(limit: Int): Flow<List<BeanBrewCount>>

    @Query("UPDATE coffee_beans SET roastLevel = :newName WHERE roastLevel = :oldName")
    suspend fun updateRoastLevelByName(oldName: String, newName: String)

    @Query("UPDATE coffee_beans SET process = :newName WHERE process = :oldName")
    suspend fun updateProcessByName(oldName: String, newName: String)
}

data class BeanBrewCount(
    val id: Long,
    val roaster: String,
    val name: String,
    val origin: String,
    val brewCount: Int
)
