package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeBeanDao {
    @Query("SELECT * FROM coffee_beans ORDER BY updatedAt DESC")
    fun getAllBeans(): Flow<List<CoffeeBean>>

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

    @Query("DELETE FROM coffee_beans")
    suspend fun deleteAll()

    /** 冲煮次数最多的豆子 Top N */
    @Query("""
        SELECT cb.id, cb.roaster, cb.name, cb.origin, COUNT(br.id) as brewCount
        FROM coffee_beans cb
        LEFT JOIN brew_records br ON cb.id = br.beanId
        GROUP BY cb.id
        ORDER BY brewCount DESC
        LIMIT :limit
    """)
    fun getTopBrewedBeans(limit: Int): Flow<List<BeanBrewCount>>
}

data class BeanBrewCount(
    val id: Long,
    val roaster: String,
    val name: String,
    val origin: String,
    val brewCount: Int
)
