package com.coffeelab.coffeenotes.util

/**
 * 恢复备份的三道安全闸（纯逻辑，无 Android 依赖，可 JVM 单测）。
 *
 * 背景：恢复是本app 唯一「先全表清空再插入」的入口，走错一步数据不可逆。
 * 这三道闸拦的是「结构异常/被污染的备份文件把用户全库变成空库」。
 */
object BackupGuard {

    sealed class Verdict {
        data class Ok(val beans: Int, val records: Int) : Verdict()
        data class Reject(val message: String) : Verdict()
    }

    /**
     * 闸1：ZIP 条目名必须是 zip 内相对路径。
     * 拒绝 `../`、绝对路径、含 .. 的任何位置——否则解包时可写出目标目录外的任意文件（zip-slip）。
     */
    fun isSafeEntryName(name: String): Boolean =
        !name.startsWith("/") && !name.contains("..") && !name.contains('\\')

    /**
     * 闸 2：data.json 结构校验。
     *
     * [data] 是解析后的 data.json 顶层 map。历史上这里写成
     * `backup["data"] as? Map ?: backup`（异构时把外层当 data），
     * 导致 beans/records 双双变成空列表却继续往下走 → 全库清空后插入 0 条。
     *
     * 规则：beans 与 records 至少有一个是非空数组，否则拒绝恢复。
     */
    fun validateData(data: Map<*, *>): Verdict {
        val beans = data["beans"] as? List<*>
        val records = data["records"] as? List<*>
        if (beans == null && records == null) {
            return Verdict.Reject("无效备份文件：data.json 结构无法识别（缺少 beans/records）")
        }
        if (beans.isNullOrEmpty() && records.isNullOrEmpty()) {
            return Verdict.Reject("备份文件里没有任何豆子或冲煮记录，已取消恢复（当前数据未改动）")
        }
        return Verdict.Ok(beans?.size ?: 0, records?.size ?: 0)
    }

    /**
     * 闸 3：回报实际写入量，并在与备份自称值不一致时追加说明。
     * 不再拿 manifest 的 counts 直接展示——那是备份文件自己的说法，与实际落库量无关。
     */
    fun buildResultNote(
        beansImported: Int,
        recordsImported: Int,
        recordsSkipped: Int,
        claimedBeans: Int?,
        claimedRecords: Int?
    ): String = buildString {
        append("已恢复豆子 $beansImported 个 · 冲煮记录 $recordsImported 条")
        if (recordsSkipped > 0) append(" · $recordsSkipped 条记录找不到归属豆子已跳过")
        if (claimedBeans != null && claimedBeans != beansImported) append(" · 备份声称 $claimedBeans 个豆子")
        if (claimedRecords != null && claimedRecords != recordsImported + recordsSkipped) {
            append(" · 备份声称 $claimedRecords 条记录")
        }
    }
}
