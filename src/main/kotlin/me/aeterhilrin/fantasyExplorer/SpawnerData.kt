package me.aeterhilrin.fantasyExplorer

import me.aeterhilrin.fantasyExplorer.PinyinUtils.cleanAndToPinyin

/**
 * 专门用于存储刷怪笼解析结果的数据类
 */
data class SpawnerData(
    // 刷怪笼核心数据
    val entityId: String,
    val customName: String?,
    val customNameVisible: Boolean,
    val pos: List<Double>?,
    val spawnCount: Int,
    val spawnRange: Int,
    val delay: Int,
    val minSpawnDelay: Int,
    val maxSpawnDelay: Int,
    val requiredPlayerRange: Int,
    val maxNearbyEntities: Int,
    val attributes: List<SpawnerAttribute>,
    val equipment: List<SpawnerEquipment>,
    val dropChances: List<Float>, // 装备掉落概率
    val rawNbt: String
) {
    /**
     * 生成唯一键（生物类型 + 自定义名字的拼音）
     */
    fun generateUniqueKey(): String {
        // 清理自定义名字（去除颜色代码和特殊字符）再转拼音并首字母大写
        val pinyinName = if (customName != null) {
            customName.cleanAndToPinyin()
        } else {
            "Unnamed"
        }


        // 组合唯一键：entityId_PinyinName
        return "${entityId}_$pinyinName"
    }
}

data class SpawnerAttribute(
    val name: String,
    val base: Double
)

data class SpawnerEquipment(
    val id: String,
    val damage: Int,
    val count: Int,
    val name: String?,
    val lore: List<String>?,
    val dropChance: Float // 装备掉落概率
)