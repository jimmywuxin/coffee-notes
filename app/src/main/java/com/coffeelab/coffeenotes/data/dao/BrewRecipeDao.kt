package com.coffeelab.coffeenotes.data.dao

import androidx.room.*
import com.coffeelab.coffeenotes.data.entity.BrewRecipe
import kotlinx.coroutines.flow.Flow

@Dao
interface BrewRecipeDao {
    @Query("SELECT * FROM brew_recipes ORDER BY updatedAt DESC")
    fun getAllRecipes(): Flow<List<BrewRecipe>>

    @Query("SELECT * FROM brew_recipes WHERE id = :id")
    suspend fun getRecipeById(id: Long): BrewRecipe?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: BrewRecipe): Long

    @Update
    suspend fun update(recipe: BrewRecipe)

    @Delete
    suspend fun delete(recipe: BrewRecipe)

    @Query("SELECT * FROM brew_recipes WHERE equipment = :equipment")
    fun getRecipesByEquipment(equipment: String): Flow<List<BrewRecipe>>

    @Query("DELETE FROM brew_recipes")
    suspend fun deleteAll()
}
