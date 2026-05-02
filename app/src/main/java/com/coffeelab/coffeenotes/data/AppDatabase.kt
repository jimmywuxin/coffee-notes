package com.coffeelab.coffeenotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.coffeelab.coffeenotes.data.dao.*
import com.coffeelab.coffeenotes.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CoffeeBean::class,
        FlavorTag::class,
        BrewRecord::class,
        BrewRecipe::class,
        Equipment::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeBeanDao(): CoffeeBeanDao
    abstract fun flavorTagDao(): FlavorTagDao
    abstract fun brewRecordDao(): BrewRecordDao
    abstract fun brewRecipeDao(): BrewRecipeDao
    abstract fun equipmentDao(): EquipmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coffee_notes.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateEquipment(database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateEquipment(database: AppDatabase) {
            val dao = database.equipmentDao()
            val items = Equipment.DEFAULT_EQUIPMENT.mapIndexed { index, name ->
                Equipment(name = name, sortOrder = index)
            }
            dao.insertAll(items)
        }
    }
}
