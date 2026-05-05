package com.coffeelab.coffeenotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
        Equipment::class,
        Grinder::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeBeanDao(): CoffeeBeanDao
    abstract fun flavorTagDao(): FlavorTagDao
    abstract fun brewRecordDao(): BrewRecordDao
    abstract fun brewRecipeDao(): BrewRecipeDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun grinderDao(): GrinderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from v4 → v5: add isIced, iceAmount, bypassAmount columns
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE brew_records ADD COLUMN isIced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE brew_records ADD COLUMN iceAmount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE brew_records ADD COLUMN bypassAmount INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coffee_notes.db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateEquipment(database)
                                    populateGrinders(database)
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
            val existing = dao.getAllOnce()
            if (existing.isEmpty()) {
                val items = Equipment.DEFAULT_EQUIPMENT.mapIndexed { index, name ->
                    Equipment(name = name, sortOrder = index)
                }
                dao.insertAll(items)
            }
        }

        private suspend fun populateGrinders(database: AppDatabase) {
            val dao = database.grinderDao()
            val existing = dao.getAllOnce()
            if (existing.isEmpty()) {
                val items = Grinder.DEFAULT_GRINDERS.mapIndexed { index, name ->
                    Grinder(name = name, sortOrder = index)
                }
                dao.insertAll(items)
            }
        }
    }
}
