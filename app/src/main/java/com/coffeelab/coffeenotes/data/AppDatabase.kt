package com.coffeelab.coffeenotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        BrewMethod::class,
        Equipment::class,
        Grinder::class,
        RoastDegree::class,
        ProcessMethod::class,
        RestPeriodConfig::class,
        PeakFlavorConfig::class,
        PurchaseRecord::class
    ],
    version = 15,
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

        // Migration from v4 → v5: add isIced, iceAmount, bypassAmount columns
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE brew_records ADD COLUMN isIced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE brew_records ADD COLUMN iceAmount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE brew_records ADD COLUMN bypassAmount INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration from v5 → v6: add extraction suggestion columns to coffee_beans
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN dose REAL")
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN brewRatio TEXT")
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN waterAmount REAL")
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN brewTime INTEGER")
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN waterTemp INTEGER")
            }
        }

        // Migration from v6 → v7: replace brew_recipes with brew_methods + rename recipeId → methodId
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rename recipeId to methodId in brew_records
                db.execSQL("ALTER TABLE brew_records RENAME COLUMN recipeId TO methodId")
                // Create new brew_methods table
                db.execSQL("""
                    CREATE TABLE brew_methods (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        isPreset INTEGER NOT NULL DEFAULT 0,
                        steps TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Insert preset brew methods during migration (so upgraded users also get them)
                val now = System.currentTimeMillis()
                db.execSQL("""
                    INSERT INTO brew_methods (name, isPreset, steps, createdAt, updatedAt) VALUES
                    ('三段式冲煮', 1, '[{\"waterAmount\":30.0,\"durationSeconds\":30,\"description\":\"小水流闷蒸\"},{\"waterAmount\":120.0,\"durationSeconds\":45,\"description\":\"中水流稳定注入\"},{\"waterAmount\":null,\"durationSeconds\":45,\"description\":\"大水流至总水量\"}]', $now, $now),
                    ('一刀流', 1, '[{\"waterAmount\":null,\"durationSeconds\":90,\"description\":\"全程不断流，稳定中水流\"}]', $now, $now),
                    ('四六冲', 1, '[{\"waterAmount\":60.0,\"durationSeconds\":45,\"description\":\"第一段：大水流\"},{\"waterAmount\":60.0,\"durationSeconds\":30,\"description\":\"第二段：中水流\"},{\"waterAmount\":60.0,\"durationSeconds\":30,\"description\":\"第三段：小水流\"},{\"waterAmount\":null,\"durationSeconds\":30,\"description\":\"第四段：至总水量\"}]', $now, $now)
                """.trimIndent())
            }
        }

        // Migration from v8 → v9: add isArchived to coffee_beans
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration from v9 → v10: add coffeeWeight, coffeeWaterRatio, waterTemp to brew_methods
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE brew_methods ADD COLUMN coffeeWeight REAL")
                db.execSQL("ALTER TABLE brew_methods ADD COLUMN coffeeWaterRatio REAL")
                db.execSQL("ALTER TABLE brew_methods ADD COLUMN waterTemp INTEGER")
            }
        }

        // Migration from v10 → v11: add localPhotoPaths to coffee_beans
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN localPhotoPaths TEXT")
            }
        }

        // Migration from v11 → v12: add pouringDurationSeconds to brew_records
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE brew_records ADD COLUMN pouringDurationSeconds INTEGER")
            }
        }

        // Migration from v12 → v13: add pouringDurationSeconds to coffee_beans
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN pouringDurationSeconds INTEGER")
            }
        }

        // Migration from v13 → v14: add roast/system tables + restDays/peakFlavorDays to coffee_beans
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. roast_degrees
                db.execSQL("""
                    CREATE TABLE roast_degrees (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                // 2. process_methods
                db.execSQL("""
                    CREATE TABLE process_methods (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                // 3. rest_period_configs
                db.execSQL("""
                    CREATE TABLE rest_period_configs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        roastDegreeId INTEGER NOT NULL,
                        restDays INTEGER NOT NULL,
                        FOREIGN KEY(roastDegreeId) REFERENCES roast_degrees(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                // 4. peak_flavor_configs
                db.execSQL("""
                    CREATE TABLE peak_flavor_configs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        roastDegreeId INTEGER NOT NULL,
                        peakFlavorDays INTEGER NOT NULL,
                        FOREIGN KEY(roastDegreeId) REFERENCES roast_degrees(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                // 5. purchase_records
                db.execSQL("""
                    CREATE TABLE purchase_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        beanId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        weightGrams INTEGER NOT NULL,
                        price REAL NOT NULL,
                        unitPrice REAL NOT NULL,
                        FOREIGN KEY(beanId) REFERENCES coffee_beans(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                // 6. Add restDays & peakFlavorDays to coffee_beans
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN restDays INTEGER")
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN peakFlavorDays INTEGER")
                // 7. Indexes
                db.execSQL("CREATE INDEX index_rest_period_configs_roastDegreeId ON rest_period_configs(roastDegreeId)")
                db.execSQL("CREATE INDEX index_peak_flavor_configs_roastDegreeId ON peak_flavor_configs(roastDegreeId)")
                db.execSQL("CREATE INDEX index_purchase_records_beanId ON purchase_records(beanId)")

                // 8. 预置烘焙度
                val roastPreset = listOf("极浅烘", "浅烘", "中浅", "中烘", "中深", "深烘")
                roastPreset.forEachIndexed { index, name ->
                    db.execSQL("INSERT INTO roast_degrees (name, sortOrder) VALUES (?, ?)", arrayOf(name, index))
                }
                // 预置处理法
                val processPreset = listOf("水洗", "日晒", "蜜处理", "厌氧", "其他")
                processPreset.forEachIndexed { index, name ->
                    db.execSQL("INSERT INTO process_methods (name, sortOrder) VALUES (?, ?)", arrayOf(name, index))
                }
            }
        }

        // Migration from v14 → v15: add roastDate to purchase_records
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE purchase_records ADD COLUMN roastDate INTEGER")
            }
        }

        // Migration from v7 → v8: add sortOrder to coffee_beans and brew_methods
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE brew_methods ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                // Set sortOrder = updatedAt for existing beans (preserve relative order)
                db.execSQL("UPDATE coffee_beans SET sortOrder = updatedAt WHERE sortOrder = 0")
                // Set sortOrder = updatedAt for existing methods
                db.execSQL("UPDATE brew_methods SET sortOrder = updatedAt WHERE sortOrder = 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coffee_notes.db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
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
