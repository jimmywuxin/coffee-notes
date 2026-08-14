package com.coffeelab.coffeenotes.data

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room 迁移全链路测试。
 *
 * 现状说明：仓库 schemas/ 仅收录 20.json / 21.json（早期 1~19 的 schema JSON 未入仓库），
 * MigrationTestHelper 依赖 schema JSON 建旧库，故 1→20 各段目前无法覆盖。
 * 本次聚焦最近一次迁移 20→21（2026-08-02 曾因 SQL 写错导致迁移崩溃丢数据的迁移），
 * 后续若补全早期 schema JSON，可扩展为 1→21 全链路。
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * v20 → v21：新建 stock_adjustments 表（库存调整/快捷扣减）。
     * 验证：迁移后旧数据逐列保留；新表可插入、可查询；beanId 索引生效。
     */
    @Test
    fun migrate20To21_preservesDataAndCreatesStockAdjustments() {
        // 1. 用 v20 schema JSON 建旧库，插入两条豆子模拟老用户数据。
        //    第一条覆盖全部可空列（extractionMethod/dose/brewRatio/.../stockResetAt），
        //    确保这些列在迁移过程中不被破坏。
        helper.createDatabase(testDbName, 20).use { db ->
            db.execSQL(
                """
                INSERT INTO coffee_beans
                    (roaster, name, origin, region, estate, variety, process, roastLevel, grindSize,
                     roastDate, notes, localPhotoPaths, imageUri, isFavorite, isArchived, sortOrder,
                     createdAt, updatedAt, extractionMethod, dose, brewRatio, waterAmount, brewTime,
                     waterTemp, pouringDurationSeconds, restDays, peakFlavorDays, stockResetAt)
                VALUES
                    ('白鲸咖啡', '埃塞日晒瑰夏', '埃塞俄比亚', '古吉', '罕贝拉', '瑰夏', '日晒', '浅烘', '中细',
                     1700000000000, '花香明显', '["/storage/a.jpg"]', 'content://a', 0, 0, 0,
                     1700000000000, 1700000000000, 'v60', 15.0, '1:15', 225.0, 150, 92,
                     45, 14, 35, 1700000000000)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO coffee_beans
                    (roaster, name, origin, region, estate, variety, process, roastLevel, grindSize,
                     notes, localPhotoPaths, imageUri, isFavorite, isArchived, sortOrder,
                     createdAt, updatedAt)
                VALUES
                    ('测试烘焙', '曼特宁', '印尼', '苏门答腊', '', '铁皮卡', '湿刨', '深烘', '中',
                     '', '[]', '', 1, 0, 1, 1700000001000, 1700000001000)
                """.trimIndent()
            )
        }

        // 2. 执行 20→21 迁移（MigrationTestHelper 内部会用 21.json 校验表结构一致性）
        val db: SupportSQLiteDatabase = helper.runMigrationsAndValidate(
            testDbName, 21, true, AppDatabaseMigrations.MIGRATION_20_21
        )

        try {
            // ---- 断言 1：数据全部保留 ----
            val countCursor: Cursor = db.query("SELECT COUNT(*) FROM coffee_beans", emptyArray())
            countCursor.use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("迁移后豆子数量应为 2", 2, c.getInt(0))
            }

            // 逐列对比第一条（覆盖 TEXT/INTEGER/REAL 各类型）
            val beanCursor: Cursor = db.query(
                """SELECT roaster, name, origin, region, estate, variety, process, roastLevel, grindSize,
                         roastDate, notes, localPhotoPaths, imageUri, isFavorite, isArchived, sortOrder,
                         createdAt, updatedAt, extractionMethod, dose, brewRatio, waterAmount, brewTime,
                         waterTemp, pouringDurationSeconds, restDays, peakFlavorDays, stockResetAt
                  FROM coffee_beans WHERE id = 1""",
                emptyArray()
            )
            beanCursor.use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("白鲸咖啡", c.getString(0))
                assertEquals("埃塞日晒瑰夏", c.getString(1))
                assertEquals("浅烘", c.getString(7))
                assertEquals(1700000000000L, c.getLong(9))   // roastDate
                assertEquals("花香明显", c.getString(10))      // notes
                assertEquals("""["/storage/a.jpg"]""", c.getString(11)) // localPhotoPaths
                assertEquals(0, c.getInt(13))                 // isFavorite
                assertEquals(0, c.getInt(14))                 // isArchived
                assertEquals("v60", c.getString(18))          // extractionMethod（MIGRATION_19_20 补的列）
                assertEquals(15.0, c.getDouble(19), 0.0)      // dose
                assertEquals("1:15", c.getString(20))         // brewRatio
                assertEquals(225.0, c.getDouble(21), 0.0)     // waterAmount
                assertEquals(150, c.getInt(22))               // brewTime
                assertEquals(92, c.getInt(23))                // waterTemp
                assertEquals(45, c.getInt(24))                // pouringDurationSeconds
                assertEquals(14, c.getInt(25))                // restDays
                assertEquals(35, c.getInt(26))                // peakFlavorDays
                assertEquals(1700000000000L, c.getLong(27))   // stockResetAt
            }

            // ---- 断言 2：新表 stock_adjustments 可插入、可查询 ----
            db.execSQL(
                "INSERT INTO stock_adjustments (beanId, changeGrams, note, createdAt) VALUES (1, -50.0, '分装送人', 1700000002000)"
            )
            db.execSQL(
                "INSERT INTO stock_adjustments (beanId, changeGrams, note, createdAt) VALUES (1, 200.0, '补录', 1700000003000)"
            )
            val adjCursor: Cursor = db.query(
                "SELECT beanId, changeGrams, note FROM stock_adjustments ORDER BY id", emptyArray()
            )
            adjCursor.use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1L, c.getLong(0))
                assertEquals(-50.0, c.getDouble(1), 0.0)
                assertEquals("分装送人", c.getString(2))
                assertTrue(c.moveToNext())
                assertEquals(200.0, c.getDouble(1), 0.0)
                assertEquals("补录", c.getString(2))
            }

            // beanId 索引可用（按 beanId 查询）
            val idxCursor: Cursor = db.query(
                "SELECT COUNT(*) FROM stock_adjustments WHERE beanId = 1", emptyArray()
            )
            idxCursor.use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(2, c.getInt(0))
            }
        } finally {
            db.close()
        }
    }
}
