package com.coffeelab.coffeenotes.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migrations for AppDatabase.
 * Extracted from AppDatabase companion object to keep the class focused.
 */
object AppDatabaseMigrations {
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

    // Migration from v15 → v16: convert brew_records.equipment/grinder (String) to equipmentId/grinderId (Long FK)
    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Add new nullable FK columns
            db.execSQL("ALTER TABLE brew_records ADD COLUMN equipmentId INTEGER REFERENCES equipment(id) ON DELETE SET NULL")
            db.execSQL("ALTER TABLE brew_records ADD COLUMN grinderId INTEGER REFERENCES grinders(id) ON DELETE SET NULL")

            // 2. Migrate data: map old equipment name string to equipment.id
            // Use a cursor to iterate and update row by row since SQLite doesn't support UPDATE with JOIN
            db.query("SELECT rowid, equipment, grinder FROM brew_records").use { recordsCursor ->
                while (recordsCursor.moveToNext()) {
                    val rowId = recordsCursor.getLong(0)
                    val oldEq = recordsCursor.getString(1) ?: ""
                    val oldGr = recordsCursor.getString(2) ?: ""

                    if (oldEq.isNotEmpty()) {
                        db.query("SELECT id FROM equipment WHERE name = ?", arrayOf(oldEq)).use { eqCursor ->
                            if (eqCursor.moveToFirst()) {
                                val eqId = eqCursor.getLong(0)
                                db.execSQL("UPDATE brew_records SET equipmentId = $eqId WHERE rowid = $rowId")
                            }
                        }
                    }

                    if (oldGr.isNotEmpty()) {
                        db.query("SELECT id FROM grinders WHERE name = ?", arrayOf(oldGr)).use { grCursor ->
                            if (grCursor.moveToFirst()) {
                                val grId = grCursor.getLong(0)
                                db.execSQL("UPDATE brew_records SET grinderId = $grId WHERE rowid = $rowId")
                            }
                        }
                    }
                }
            }

            // 3. Create indexes on new columns
            db.execSQL("CREATE INDEX index_brew_records_equipmentId ON brew_records(equipmentId)")
            db.execSQL("CREATE INDEX index_brew_records_grinderId ON brew_records(grinderId)")
        }
    }

    // Migration from v16 → v17: add impression_tags and bean_impression_tags tables
    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE impression_tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE bean_impression_tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    beanId INTEGER NOT NULL,
                    tagId INTEGER NOT NULL,
                    FOREIGN KEY(beanId) REFERENCES coffee_beans(id) ON DELETE CASCADE,
                    FOREIGN KEY(tagId) REFERENCES impression_tags(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX index_bean_impression_tags_beanId ON bean_impression_tags(beanId)")
            db.execSQL("CREATE INDEX index_bean_impression_tags_tagId ON bean_impression_tags(tagId)")
            val defaultTags = listOf("甜感突出", "回甘悠长", "清爽", "浓郁", "平衡", "复杂", "干净", "厚重")
            defaultTags.forEachIndexed { index, name ->
                db.execSQL("INSERT INTO impression_tags (name, sortOrder) VALUES (?, ?)", arrayOf(name, index))
            }
        }
    }

    // Migration from v17 → v18: add ocr_corrections table for OCR 纠错回流
    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE ocr_corrections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    field TEXT NOT NULL,
                    ocrRaw TEXT NOT NULL,
                    userValue TEXT NOT NULL,
                    beanId INTEGER,
                    hitCount INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX index_ocr_corrections_field_ocrRaw ON ocr_corrections(field, ocrRaw)")
        }
    }

    // Migration from v18 → v19: add stockResetAt to coffee_beans（库存重置时间戳）
    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE coffee_beans ADD COLUMN stockResetAt INTEGER")
        }
    }

    // Migration from v19 → v20: 补 extractionMethod 列（MIGRATION_5_6 当年漏加，老用户数据库缺此列）
    // 防御性检查：若数据库曾因 fallback 重建已含此列，则跳过，避免 duplicate column 报错
    private val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val cursor = db.query("SELECT COUNT(*) FROM pragma_table_info('coffee_beans') WHERE name='extractionMethod'")
            val exists = cursor.use { it.moveToFirst() && it.getInt(0) > 0 }
            if (!exists) {
                db.execSQL("ALTER TABLE coffee_beans ADD COLUMN extractionMethod TEXT")
            }
        }
    }

    val ALL = arrayOf(
        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
        MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12,
        MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
        MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20
    )
}
