package com.coffeelab.coffeenotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        BrewMethod::class,
        Equipment::class,
        Grinder::class,
        RoastDegree::class,
        ProcessMethod::class,
        RestPeriodConfig::class,
        PeakFlavorConfig::class,
        PurchaseRecord::class
    ],
    version = 16,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeBeanDao(): CoffeeBeanDao
    abstract fun flavorTagDao(): FlavorTagDao
    abstract fun brewRecordDao(): BrewRecordDao
    abstract fun brewMethodDao(): BrewMethodDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun grinderDao(): GrinderDao
    abstract fun roastDegreeDao(): RoastDegreeDao
    abstract fun processMethodDao(): ProcessMethodDao
    abstract fun restPeriodConfigDao(): RestPeriodConfigDao
    abstract fun peakFlavorConfigDao(): PeakFlavorConfigDao
    abstract fun purchaseRecordDao(): PurchaseRecordDao

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
                    .addMigrations(*AppDatabaseMigrations.ALL)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Synchronous population: directly insert default data in onCreate
                            populateEquipmentSync(db)
                            populateGrindersSync(db)
                            populateBrewMethodsSync(db)
                            populateRoastDegreesSync(db)
                            populateProcessMethodsSync(db)
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun populateEquipmentSync(db: SupportSQLiteDatabase) {
            val cursor = db.query("SELECT name FROM equipment")
            val existingNames = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                existingNames.add(cursor.getString(0))
            }
            cursor.close()
            val now = System.currentTimeMillis()
            Equipment.DEFAULT_EQUIPMENT.forEachIndexed { index, name ->
                if (name !in existingNames) {
                    db.execSQL("INSERT INTO equipment (name, sortOrder) VALUES (?, ?)", arrayOf(name, index))
                }
            }
        }

        private fun populateGrindersSync(db: SupportSQLiteDatabase) {
            val cursor = db.query("SELECT name FROM grinders")
            val existingNames = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                existingNames.add(cursor.getString(0))
            }
            cursor.close()
            Grinder.DEFAULT_GRINDERS.forEachIndexed { index, name ->
                if (name !in existingNames) {
                    db.execSQL("INSERT INTO grinders (name, sortOrder) VALUES (?, ?)", arrayOf(name, index))
                }
            }
        }

        private fun populateBrewMethodsSync(db: SupportSQLiteDatabase) {
            val cursor = db.query("SELECT COUNT(*) FROM brew_methods")
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()
            if (count > 0) return
            val now = System.currentTimeMillis()
            db.execSQL("""
                INSERT INTO brew_methods (name, isPreset, steps, sortOrder, createdAt, updatedAt) VALUES
                ('三段式冲煮', 1, '[{\"waterAmount\":30.0,\"durationSeconds\":30,\"description\":\"小水流闷蒸\"},{\"waterAmount\":120.0,\"durationSeconds\":45,\"description\":\"中水流稳定注入\"},{\"waterAmount\":null,\"durationSeconds\":45,\"description\":\"大水流至总水量\"}]', 0, $now, $now)
            """.trimIndent())
            db.execSQL("""
                INSERT INTO brew_methods (name, isPreset, steps, sortOrder, createdAt, updatedAt) VALUES
                ('一刀流', 1, '[{\"waterAmount\":null,\"durationSeconds\":90,\"description\":\"全程不断流，稳定中水流\"}]', 1, $now, $now)
            """.trimIndent())
            db.execSQL("""
                INSERT INTO brew_methods (name, isPreset, steps, sortOrder, createdAt, updatedAt) VALUES
                ('四六冲', 1, '[{\"waterAmount\":60.0,\"durationSeconds\":45,\"description\":\"第一段：大水流\"},{\"waterAmount\":60.0,\"durationSeconds\":30,\"description\":\"第二段：中水流\"},{\"waterAmount\":60.0,\"durationSeconds\":30,\"description\":\"第三段：小水流\"},{\"waterAmount\":null,\"durationSeconds\":30,\"description\":\"第四段：至总水量\"}]', 2, $now, $now)
            """.trimIndent())
        }

        private fun populateRoastDegreesSync(db: SupportSQLiteDatabase) {
            val cursor = db.query("SELECT COUNT(*) FROM roast_degrees")
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()
            if (count > 0) return
            RoastDegree.DEFAULT_ROAST_DEGREES.forEachIndexed { index, name ->
                db.execSQL("INSERT INTO roast_degrees (name, sortOrder) VALUES (?, ?)", arrayOf(name, index))
            }
        }

        private fun populateProcessMethodsSync(db: SupportSQLiteDatabase) {
            val cursor = db.query("SELECT COUNT(*) FROM process_methods")
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()
            if (count > 0) return
            ProcessMethod.DEFAULT_PROCESS_METHODS.forEachIndexed { index, name ->
                db.execSQL("INSERT INTO process_methods (name, sortOrder) VALUES (?, ?)", arrayOf(name, index))
            }
        }

        suspend fun populateEquipment(database: AppDatabase) {
            val dao = database.equipmentDao()
            val existing = dao.getAllOnce()
            val existingNames = existing.map { it.name }.toSet()
            val toInsert = Equipment.DEFAULT_EQUIPMENT
                .filter { it !in existingNames }
                .mapIndexed { index, name -> Equipment(name = name, sortOrder = index) }
            if (toInsert.isNotEmpty()) {
                dao.insertAll(toInsert)
            }
        }

        suspend fun populateGrinders(database: AppDatabase) {
            val dao = database.grinderDao()
            val existing = dao.getAllOnce()
            val existingNames = existing.map { it.name }.toSet()
            val toInsert = Grinder.DEFAULT_GRINDERS
                .filter { it !in existingNames }
                .mapIndexed { index, name -> Grinder(name = name, sortOrder = index) }
            if (toInsert.isNotEmpty()) {
                dao.insertAll(toInsert)
            }
        }

        suspend fun populateBrewMethods(database: AppDatabase) {
            val dao = database.brewMethodDao()
            val existing = dao.getAllOnce()
            if (existing.isNotEmpty()) return

            val presets = listOf(
                BrewMethod(
                    name = "三段式冲煮",
                    isPreset = true,
                    steps = Converters.serializeSteps(listOf(
                        BrewMethodStep(waterAmount = 30f, durationSeconds = 30, description = "小水流闷蒸"),
                        BrewMethodStep(waterAmount = 120f, durationSeconds = 45, description = "中水流稳定注入"),
                        BrewMethodStep(waterAmount = null, durationSeconds = 45, description = "大水流至总水量")
                    ))
                ),
                BrewMethod(
                    name = "一刀流",
                    isPreset = true,
                    steps = Converters.serializeSteps(listOf(
                        BrewMethodStep(waterAmount = null, durationSeconds = 90, description = "全程不断流，稳定中水流")
                    ))
                ),
                BrewMethod(
                    name = "四六冲",
                    isPreset = true,
                    steps = Converters.serializeSteps(listOf(
                        BrewMethodStep(waterAmount = 60f, durationSeconds = 45, description = "第一段：大水流"),
                        BrewMethodStep(waterAmount = 60f, durationSeconds = 30, description = "第二段：中水流"),
                        BrewMethodStep(waterAmount = 60f, durationSeconds = 30, description = "第三段：小水流"),
                        BrewMethodStep(waterAmount = null, durationSeconds = 30, description = "第四段：至总水量")
                    ))
                )
            )
            presets.forEach { dao.insert(it) }
        }
    }
}
