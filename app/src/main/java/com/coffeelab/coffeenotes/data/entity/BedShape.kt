package com.coffeelab.coffeenotes.data.entity

/**
 * 粉坑形状六分类（口径参考前街咖啡「六种粉床状态」，社区通用说法）。
 * 存储用 key，展示用 label，hint 为选中后的辅助说明。
 */
enum class BedShape(
    val key: String,
    val label: String,
    val hint: String
) {
    DEEP_EVEN("deep_even", "深坑均匀", "理想：大水流抬高粉床，中心均匀注水"),
    DEEP_OFFSET("deep_offset", "深坑偏移", "注水不垂直，萃取可能不均"),
    FLAT_WITH_WALL("flat_wall", "平坦带粉墙", "没抬高粉床 / 分段注水太多"),
    FLAT_NO_WALL("flat", "平坦无粉墙", "粉密度高（浅烘豆），粉全沉底"),
    MUD("mud", "泥坑", "磨太细，冲煮拖长，易苦涩"),
    SALT("salt", "海盐坑", "磨太粗，萃取不足，缺香气");

    companion object {
        fun fromKey(key: String): BedShape? = entries.find { it.key == key }
    }
}
