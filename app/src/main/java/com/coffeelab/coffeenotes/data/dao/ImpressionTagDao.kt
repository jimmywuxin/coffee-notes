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

    /**
     * 一次取回「豆子 → 印象标签名」的全量映射，替代逐豆查询的 N+1
     * （原 BeanListScreen 里 `for (bean in beans) getTagsForBeanOnce(bean.id)` 是 N 次查询）。
     */
    @Query("""
        SELECT bit.beanId AS beanId, it.name AS name
        FROM bean_impression_tags bit
        JOIN impression_tags it ON bit.tagId = it.id
        ORDER BY bit.beanId ASC, it.sortOrder ASC
    """)
    suspend fun getAllBeanTagNamesOnce(): List<BeanTagName>

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

/** 批量查询结果行：某豆子的一个印象标签名 */
data class BeanTagName(val beanId: Long, val name: String)
