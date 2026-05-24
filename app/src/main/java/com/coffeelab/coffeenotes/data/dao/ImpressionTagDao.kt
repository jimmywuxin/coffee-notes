package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.ImpressionTag
import com.coffeelab.coffeenotes.data.entity.BeanImpressionTag
import kotlinx.coroutines.flow.Flow

data class ImpressionTagWithJoin(
    @Embedded val tag: ImpressionTag
)

@Dao
interface ImpressionTagDao {
    // ===== Impression Tags (标签库) =====

    @Query("SELECT * FROM impression_tags ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<ImpressionTag>>

    @Query("SELECT * FROM impression_tags ORDER BY sortOrder ASC")
    suspend fun getAllOnce(): List<ImpressionTag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: ImpressionTag): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<ImpressionTag>)

    @Update
    suspend fun update(tag: ImpressionTag)

    @Delete
    suspend fun delete(tag: ImpressionTag)

    @Query("DELETE FROM impression_tags")
    suspend fun deleteAll()

    @Query("SELECT MAX(sortOrder) FROM impression_tags")
    suspend fun getMaxSortOrder(): Int?

    // ===== Bean-Impression 关联 =====

    @Query("""
        SELECT it.*
        FROM bean_impression_tags bit
        JOIN impression_tags it ON bit.tagId = it.id
        WHERE bit.beanId = :beanId
        ORDER BY it.sortOrder ASC
    """)
    fun getTagsForBean(beanId: Long): Flow<List<ImpressionTag>>

    @Query("""
        SELECT it.*
        FROM bean_impression_tags bit
        JOIN impression_tags it ON bit.tagId = it.id
        WHERE bit.beanId = :beanId
        ORDER BY it.sortOrder ASC
    """)
    suspend fun getTagsForBeanOnce(beanId: Long): List<ImpressionTag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeanTag(tag: BeanImpressionTag): Long

    @Query("DELETE FROM bean_impression_tags WHERE beanId = :beanId")
    suspend fun deleteAllForBean(beanId: Long)

    @Transaction
    suspend fun saveTagsForBean(beanId: Long, tagIds: List<Long>) {
        deleteAllForBean(beanId)
        tagIds.forEach { tagId ->
            insertBeanTag(BeanImpressionTag(beanId = beanId, tagId = tagId))
        }
    }
}
