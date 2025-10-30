package me.aeterhilrin.fantasyExplorer

import java.util.*

/**
 * 专门用于存储普通物品解析结果的数据类
 */
data class ItemData(
    // 物品基础信息
    val internalId: String,           // 内部ID（用于去重）
    val itemType: String,             // 物品类型
    val material: String,            // 材料类型
    //val amount: Int,                 // 数量

    // 显示属性
    val customName: String?,         // 自定义名称
    val lore: List<String>,          // 描述文本

    // 物品属性
    val enchantments: List<EnchantmentData>,  // 附魔
    val attributes: List<AttributeData>,      // 属性修饰符
    val potionEffects: List<PotionEffectData>, // 新增：药水效果
    val unbreakable: Boolean,        // 不可破坏
    val damage: Int,                 // 耐久度
    val maxDurability: Int,          // 最大耐久度

    // 解析元数据
    val playerName: String,          // 检查的玩家
    val timestamp: Long = System.currentTimeMillis(),

    // 原始数据备份
    val rawNbt: String
) {
    /**
     * 附魔数据类
     */
    data class EnchantmentData(
        val id: Int,
        val level: Int,
        val enchantmentName: String
    )

    /**
     * 属性修饰符数据类
     */
    data class AttributeData(
        val name: String,
        val amount: Double,
        val operation: Int,
        val attributeName: String,
        val slot: String?
    )

    /**
     * 新增：药水效果数据类
     */
    data class PotionEffectData(
        val id: Int,
        val amplifier: Int,          // 效果等级（0-based，实际显示需要+1）
        val duration: Int,          // 持续时间（tick）
        val effectName: String       // 效果名称
    )
}