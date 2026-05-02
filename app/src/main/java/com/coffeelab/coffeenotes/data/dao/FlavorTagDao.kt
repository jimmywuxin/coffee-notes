package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.FlavorTag
import kotlinx.coroutines.flow.Flow

@Dao
interface FlavorTagDao {
    @Query("SELECT * FROM flavor_tags WHERE beanId = :beanId")
    fun getTagsForBean(beanId: Long): Flow<List<FlavorTag>>

    @Query("SELECT * FROM flavor_tags WHERE beanId = :beanId")
    suspend fun getTagsForBeanOnce(beanId: Long): List<FlavorTag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: FlavorTag): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<FlavorTag>)

    @Delete
    suspend fun delete(tag: FlavorTag)

    @Query("DELETE FROM flavor_tags WHERE beanId = :beanId")
    suspend fun deleteAllForBean(beanId: Long)
}
