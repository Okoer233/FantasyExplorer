package me.aeterhilrin.fantasyExplorer

import de.tr7zw.changeme.nbtapi.NBT
import de.tr7zw.changeme.nbtapi.NBTCompound
import org.bukkit.Bukkit
import java.util.logging.Level
import kotlin.Double
import kotlin.collections.List

object SpawnerDataParser {
    fun parseCommand(command: String): SpawnerData? {
        try {
            // 从命令中提取NBT数据
            val startIdx = command.indexOf('{')
            val endIdx = command.lastIndexOf('}')
            if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) return null

            val nbtString = command.substring(startIdx, endIdx + 1)
            val nbt = NBT.parseNBT(nbtString) ?: return null

            // 确保转换为NBTCompound类型
            return if (nbt is NBTCompound) {
                parseSpawnerNBT(nbt)
            } else {
                null
            }
        } catch (e: Exception) {
            Bukkit.getLogger().log(Level.WARNING, "解析刷怪笼数据失败", e)
            return null
        }
    }

    private fun parseSpawnerNBT(nbt: NBTCompound): SpawnerData {
        val entityId = nbt.getString("EntityId")
        val spawnData = nbt.getCompound("SpawnData")

        // 处理可能为空的字段
        val spawnPosition = spawnData?.getDoubleList("Pos")?.toList()
        val customName = spawnData?.getString("CustomName")
        val customNameVisible = spawnData?.getBoolean("CustomNameVisible") ?: false

        // 解析掉落概率
        val dropChances = spawnData?.getFloatList("DropChances")?.toList() ?: emptyList()

        return SpawnerData(
            entityId = entityId,
            customName = customName,
            customNameVisible = customNameVisible,
            pos = spawnPosition,
            spawnCount = nbt.getInteger("SpawnCount"),
            spawnRange = nbt.getInteger("SpawnRange"),
            delay = nbt.getInteger("Delay"),
            minSpawnDelay = nbt.getInteger("MinSpawnDelay"),
            maxSpawnDelay = nbt.getInteger("MaxSpawnDelay"),
            requiredPlayerRange = nbt.getInteger("RequiredPlayerRange"),
            maxNearbyEntities = nbt.getInteger("MaxNearbyEntities"),
            attributes = parseAttributes(spawnData),
            equipment = parseEquipment(spawnData, dropChances),
            dropChances = dropChances,
            rawNbt = nbt.toString()
        )
    }

    private fun parseAttributes(spawnData: NBTCompound?): List<SpawnerAttribute> {
        if (spawnData == null) return emptyList()

        val attributes = mutableListOf<SpawnerAttribute>()
        val attributesList = spawnData.getCompoundList("Attributes")

        for (i in 0 until attributesList.size) {
            val attr = attributesList[i]
            attributes.add(
                SpawnerAttribute(
                    name = attr.getString("Name"),
                    base = attr.getDouble("Base")
                )
            )
        }

        return attributes
    }

    private fun parseEquipment(spawnData: NBTCompound?, dropChances: List<Float>): List<SpawnerEquipment> {
        if (spawnData == null) return emptyList()

        val equipment = mutableListOf<SpawnerEquipment>()
        val equipmentList = spawnData.getCompoundList("Equipment")

        for (i in 0 until equipmentList.size) {
            val item = equipmentList[i]
            val tag = item.getCompound("tag")
            val display = tag?.getCompound("display")

            // 获取对应槽位的掉落概率（如果存在）
            val dropChance = if (i < dropChances.size) dropChances[i] else 0.0f

            equipment.add(
                SpawnerEquipment(
                    id = item.getString("id"),
                    damage = item.getInteger("Damage"),
                    count = item.getInteger("Count"),
                    name = display?.getString("Name"),
                    lore = display?.getStringList("Lore"),
                    dropChance = dropChance
                )
            )
        }

        return equipment
    }
}