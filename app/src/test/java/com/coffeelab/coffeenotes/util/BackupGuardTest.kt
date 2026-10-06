package com.coffeelab.coffeenotes.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 恢复备份的三道安全闸。这是本项目唯一的数据保全路径，
 * 而 JVM 单测跑不了 Room/Context，所以把可判定的纯逻辑抽到这里来测。
 */
class BackupGuardTest {

    // ===== 闸 1：zip-slip =====

    @Test
    fun `正常条目名通过`() {
        assertTrue(BackupGuard.isSafeEntryName("manifest.json"))
        assertTrue(BackupGuard.isSafeEntryName("data.json"))
        assertTrue(BackupGuard.isSafeEntryName("images/bean_photos/bean_1.jpg"))
    }

    @Test
    fun `路径穿越被拒绝`() {
        // 回归测试：原先只removePrefix 就直接 File(photosDir, relativePath)，
        // "../" 能写出bean_photos 目录外的任意文件
        assertFalse(BackupGuard.isSafeEntryName("images/bean_photos/../../../etc/passwd"))
        assertFalse(BackupGuard.isSafeEntryName("../evil.jpg"))
        assertFalse(BackupGuard.isSafeEntryName("images/..\\evil.jpg"))
    }

    @Test
    fun `绝对路径被拒绝`() {
        assertFalse(BackupGuard.isSafeEntryName("/data/data/com.x/files/evil.jpg"))
    }

    // ===== 闸 2：data.json 结构校验 =====

    @Test
    fun `有豆子和记录时通过`() {
        val v = BackupGuard.validateData(mapOf("beans" to listOf(1, 2), "records" to listOf(1)))
        assertTrue(v is BackupGuard.Verdict.Ok)
    }

    @Test
    fun `只有豆子无记录也通过`() {
        // 合法场景：新库刚建、只录了豆子还没冲煮
        val v = BackupGuard.validateData(mapOf("beans" to listOf(1, 2, 3)))
        assertTrue(v is BackupGuard.Verdict.Ok)
    }

    @Test
    fun `结构异构时拒绝`() {
        // 回归测试：data.json 异构时 backup["data"] ?: backup 会把外层当 data，
        // beans/records 变空列表，继续走下去 = 全库清空后插0 条（用户数据归零）
        val v = BackupGuard.validateData(mapOf("foo" to "bar", "version" to "1.2.0"))
        assertTrue(v is BackupGuard.Verdict.Reject)
    }

    @Test
    fun `空beans 且空 records 时拒绝`() {
        val v = BackupGuard.validateData(mapOf("beans" to emptyList<Any>(), "records" to emptyList<Any>()))
        assertTrue(v is BackupGuard.Verdict.Reject)
    }

    @Test
    fun `缺键时拒绝`() {
        val v = BackupGuard.validateData(mapOf("methods" to listOf(1, 2)))
        assertTrue(v is BackupGuard.Verdict.Reject)
    }

    @Test
    fun `beans 类型错误时视为缺键并拒绝`() {
        val v = BackupGuard.validateData(mapOf("beans" to "not-a-list", "records" to "nope"))
        assertTrue(v is BackupGuard.Verdict.Reject)
    }

    // ===== 闸 3：实际写入量回报 =====

    @Test
    fun `正常情况只报实际量`() {
        val note = BackupGuard.buildResultNote(3, 10, 0, 3, 10)
        assertEquals("已恢复豆子 3 个 · 冲煮记录 10 条", note)
    }

    @Test
    fun `有跳过记录时明示`() {
        // 回归测试：原先 manifest 里找不到归属的记录静默continue，
        // 但界面数字取自 manifest 自称值，用户以为全恢复了
        val note = BackupGuard.buildResultNote(3, 8, 2, 3, 10)
        assertTrue(note.contains("2 条记录找不到归属豆子已跳过"))
        assertTrue(note.contains("已恢复豆子 3 个 · 冲煮记录 8 条"))
    }

    @Test
    fun `声称值与实际不符时给出对照`() {
        val note = BackupGuard.buildResultNote(3, 8, 0, 5, 10)
        assertTrue(note.contains("备份声称 5 个豆子"))
        assertTrue(note.contains("备份声称 10 条记录"))
    }

    @Test
    fun `无manifest 时不输出对照`() {
        val note = BackupGuard.buildResultNote(3, 10, 0, null, null)
        assertEquals("已恢复豆子 3 个 · 冲煮记录 10 条", note)
    }
}
