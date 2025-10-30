package me.aeterhilrin.fantasyExplorer

import com.google.gson.GsonBuilder
import java.io.File

data class ItemParseResult(
    val rawNbt: String,
    val itemType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val playerName: String = "",
    val hasCustomNbt: Boolean = false,
    val customName: String? = null,
    val internalId: String,
    val lore: List<String>? = null,
    val attributes: List<ItemAttribute>? = null,
    val enchantments: List<ItemEnchantment>? = null,
    val potionEffects: List<PotionEffect>? = null, // 新增：药水效果
)

// 物品属性数据类
data class ItemAttribute(
    val name: String,
    val amount: Double,
    val operation: Int,
    val attributeName: String
)

// 物品附魔数据类
data class ItemEnchantment(
    val id: Int,
    val level: Int,
    val enchantmentName: String
)

// 药水效果数据类
data class PotionEffect(
    val id: Int,
    val amplifier: Int,
    val duration: Int,
    val effectName: String
)