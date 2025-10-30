package me.aeterhilrin.fantasyExplorer

import com.google.gson.GsonBuilder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.logging.Level

/**
 * 专门处理普通物品数据JSON输出的服务
 */

class ItemJsonLoggingService(private val plugin: JavaPlugin) {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val outputFolder: File by lazy {
        val folder = File(plugin.dataFolder, "item_output")
        if (!folder.exists()) folder.mkdirs()
        folder
    }

    fun logItemToJson(itemData: ItemData): Boolean {
        return try {
            val itemsFile = File(outputFolder, "items.json")
            val existingData = readExistingItemData(itemsFile)

            if (existingData.containsKey(itemData.internalId)) {
                plugin.logger.info("检测到重复物品，跳过写入: ${itemData.internalId}")
                return false
            }

            existingData[itemData.internalId] = createItemJsonObject(itemData)
            writeItemDataToFile(itemsFile, existingData)

            plugin.logger.info("物品数据已保存到 items.json: ${itemData.internalId}")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "写入items.json失败", e)
            false
        }
    }

    private fun readExistingItemData(itemsFile: File): MutableMap<String, Any> {
        return if (itemsFile.exists() && itemsFile.length() > 0) {
            try {
                val jsonString = itemsFile.readText()
                val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(jsonString, type).toMutableMap()
            } catch (e: Exception) {
                plugin.logger.warning("解析现有items.json失败，将创建新文件: ${e.message}")
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
    }

    private fun createItemJsonObject(itemData: ItemData): Map<String, Any> {
        return mapOf(
            "internal_id" to itemData.internalId,
            "item_type" to itemData.itemType,
            "material" to itemData.material,
            "display_name" to (itemData.customName ?: "无"),
            "lore" to itemData.lore,
            "lore_count" to itemData.lore.size,
            "enchantments" to itemData.enchantments.map { enchant ->
                mapOf(
                    "id" to enchant.id,
                    "name" to enchant.enchantmentName,
                    "level" to enchant.level
                )
            },
            "enchantment_count" to itemData.enchantments.size,
            "attributes" to itemData.attributes.map { attr ->
                mapOf(
                    "name" to attr.name,
                    "attribute" to attr.attributeName,
                    "amount" to attr.amount,
                    "operation" to attr.operation,
                    "slot" to (attr.slot ?: "全部")
                )
            },
            "attribute_count" to itemData.attributes.size,
            // 新增：药水效果JSON输出
            "potion_effects" to itemData.potionEffects.map { effect ->
                mapOf(
                    "id" to effect.id,
                    "name" to effect.effectName,
                    "amplifier" to effect.amplifier,
                    "display_level" to (effect.amplifier + 1),
                    "duration_ticks" to effect.duration,
                    "duration_seconds" to (effect.duration / 20),
                    "display_duration" to if (effect.duration > 0) "${effect.duration / 20}秒" else "瞬间"
                )
            },
            "potion_effect_count" to itemData.potionEffects.size,
            "durability" to mapOf(
                "damage" to itemData.damage,
                "max_durability" to itemData.maxDurability,
                "unbreakable" to itemData.unbreakable
            ),
            "player" to itemData.playerName,
            "timestamp" to itemData.timestamp,
            "timestamp_readable" to dateFormat.format(itemData.timestamp),
            "raw_nbt" to itemData.rawNbt
        )
    }

    private fun writeItemDataToFile(itemsFile: File, itemData: Map<String, Any>) {
        itemsFile.writeText(gson.toJson(itemData))
    }

    fun getOutputFolderPath(): String {
        return outputFolder.absolutePath
    }
}