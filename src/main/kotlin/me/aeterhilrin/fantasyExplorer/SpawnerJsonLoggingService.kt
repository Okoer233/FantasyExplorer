package me.aeterhilrin.fantasyExplorer

import com.google.gson.GsonBuilder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.Level

/**
 * 专门处理刷怪笼数据JSON输出的服务
 */
class SpawnerJsonLoggingService(private val plugin: JavaPlugin) {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val outputFolder: File by lazy {
        val folder = File(plugin.dataFolder, "spawner_output")
        if (!folder.exists()) folder.mkdirs()
        folder
    }

    /**
     * 将刷怪笼数据写入spawners.json文件
     * 使用生物类型+自定义名字的拼音作为唯一标识避免重复
     */
    fun logSpawnerToJson(spawnerData: SpawnerData, playerName: String): Boolean {
        return try {
            val spawnersFile = File(outputFolder, "spawners.json")
            val existingData = readExistingSpawnerData(spawnersFile)

            // 生成唯一键（生物类型+自定义名字的拼音）
            val uniqueKey = spawnerData.generateUniqueKey()

            // 检查是否已存在相同键的刷怪笼
            if (existingData.containsKey(uniqueKey)) {
                plugin.logger.info("检测到重复刷怪笼，跳过写入: $uniqueKey")
                return false
            }
            if (spawnerData.entityId=="FALLINGSAND") {
                plugin.logger.info("检测到落沙，跳过写入: $uniqueKey")
                return false
            }

            // 添加新刷怪笼数据
            existingData[uniqueKey] = createSpawnerJsonObject(spawnerData, playerName)

            // 写回文件
            writeSpawnerDataToFile(spawnersFile, existingData)

            plugin.logger.info("刷怪笼数据已保存到 spawners.json: $uniqueKey")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "写入spawners.json失败", e)
            false
        }
    }

    /**
     * 读取现有的刷怪笼数据
     */
    private fun readExistingSpawnerData(spawnersFile: File): MutableMap<String, Any> {
        return if (spawnersFile.exists() && spawnersFile.length() > 0) {
            try {
                val jsonString = spawnersFile.readText()
                val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(jsonString, type).toMutableMap()
            } catch (e: Exception) {
                plugin.logger.warning("解析现有spawners.json失败，将创建新文件: ${e.message}")
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
    }

    /**
     * 创建刷怪笼JSON对象
     */
    private fun createSpawnerJsonObject(spawnerData: SpawnerData, playerName: String): Map<String, Any> {
        return mapOf(
            "entity_id" to spawnerData.entityId,
            "custom_name" to (spawnerData.customName ?: "无"),
            "internal_Id" to (spawnerData.internalId ?: "无"),
            "custom_name_visible" to spawnerData.customNameVisible,
            "position" to (spawnerData.pos ?: emptyList<Double>()),
            "spawn_count" to spawnerData.spawnCount,
            "spawn_range" to spawnerData.spawnRange,
            "delay" to spawnerData.delay,
            "min_spawn_delay" to spawnerData.minSpawnDelay,
            "max_spawn_delay" to spawnerData.maxSpawnDelay,
            "required_player_range" to spawnerData.requiredPlayerRange,
            "max_nearby_entities" to spawnerData.maxNearbyEntities,
            "attributes" to spawnerData.attributes.map { attr ->
                mapOf(
                    "name" to attr.name,
                    "base" to attr.base
                )
            },
            "equipment" to spawnerData.equipment.map { equip ->
                mapOf(
                    "id" to equip.id,
                    "itemInternalId" to equip.itemId,
                    "damage" to equip.damage,
                    "count" to equip.count,
                    "name" to (equip.name ?: "无"),
                    "lore" to (equip.lore ?: emptyList<String>()),
                    "drop_chance" to equip.dropChance
                )
            },
            "player" to playerName,
            "timestamp" to System.currentTimeMillis(),
            "timestamp_readable" to dateFormat.format(Date()),
            "raw_nbt" to spawnerData.rawNbt
        )
    }

    /**
     * 将刷怪笼数据写入文件
     */
    private fun writeSpawnerDataToFile(spawnersFile: File, spawnerData: Map<String, Any>) {
        spawnersFile.writeText(gson.toJson(spawnerData))
    }

    /**
     * 获取输出文件夹路径（用于反馈消息）
     */
    fun getOutputFolderPath(): String {
        return outputFolder.absolutePath
    }
}