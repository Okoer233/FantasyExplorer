package me.aeterhilrin.fantasyExplorer

import de.tr7zw.changeme.nbtapi.NBT
import de.tr7zw.changeme.nbtapi.NBTCompound
import me.aeterhilrin.fantasyExplorer.PinyinUtils.cleanAndToPinyin
import me.aeterhilrin.fantasyExplorer.PinyinUtils.getFirstTwoZWCharsInTheFirstLineOfLoreAndTheLastTwoZWCharsInTheLastLineOfLore
import me.aeterhilrin.fantasyExplorer.PinyinUtils.getItsTypeInHigherVersion
import me.aeterhilrin.fantasyExplorer.PinyinUtils.getItsTypeInHigherVersionAnotherOne
import org.bukkit.Bukkit
import java.util.logging.Level
import kotlin.Double
import kotlin.collections.List
import org.bukkit.block.Block
import java.util.regex.Pattern

object SpawnerDataParser {
    fun parseCommand(command: String,block: Block): SpawnerData? {
        try {
            // 从命令中提取NBT数据
            val startIdx = command.indexOf('{')
            val endIdx = command.lastIndexOf('}')
            if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) return null

            val nbtString = command.substring(startIdx, endIdx + 1)
            val nbt = NBT.parseNBT(nbtString) ?: return null

            // 确保转换为NBTCompound类型
            return if (nbt is NBTCompound) {
                parseSpawnerNBT(nbt,block,command)
            } else {
                null
            }
        } catch (e: Exception) {
            Bukkit.getLogger().log(Level.WARNING, "解析刷怪笼数据失败", e)
            return null
        }
    }

    private fun parseSpawnerNBT(nbt: NBTCompound,block: Block,command: String): SpawnerData {
        val entityId = nbt.getString("EntityId")
        val spawnData = nbt.getCompound("SpawnData")

        // 处理可能为空的字段
        val spawnPosition = spawnData?.getDoubleList("Pos")?.toList() ?:parseSetBlockCoordinates(command,block)
        val customName = spawnData?.getString("CustomName")
        val customNameVisible = spawnData?.getBoolean("CustomNameVisible") ?: false

        // 解析掉落概率
        val dropChances = spawnData?.getFloatList("DropChances")?.toList() ?: emptyList()
        //修复刷怪笼内部名命名机制：以命令方块的坐标作为前缀
        return SpawnerData(
            entityId = entityId.uppercase(),
            customName = customName,
            //修复点：该处命名机制已经修复
            internalId = "X${block.x}Y${block.y}Z${block.z}${entityId.uppercase()}${customName?.cleanAndToPinyin() ?:""}",
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
            val itemIdFixedPrefix=if(item.getString("id")=="minecraft:dye") (item.getInteger("Damage").toInt().getItsTypeInHigherVersion()) else item.getString("id").replace("minecraft:","").uppercase().getItsTypeInHigherVersionAnotherOne()
            equipment.add(
                SpawnerEquipment(
                    id = if(item.getString("id")=="minecraft:dye") (item.getInteger("Damage").toInt().getItsTypeInHigherVersion()) else item.getString("id"),
                    //todo:让其当后者为空的时候不输出任何数值，同时修改spawnerdata的transtoyml让它当读取到占位掉落的时候不写入本次装备

                    itemId= "${itemIdFixedPrefix.replace("_","").uppercase()}${display?.getString("Name")?.cleanAndToPinyin() ?:""}${display?.getStringList("Lore")?.toList<String>()?.getFirstTwoZWCharsInTheFirstLineOfLoreAndTheLastTwoZWCharsInTheLastLineOfLore()?.cleanAndToPinyin() ?:""}",
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
    /**
     * 解析/setblock指令中的坐标部分，支持相对坐标(~)和绝对坐标的混合使用
     * @param command 完整的/setblock指令字符串
     * @param baseBlock 基准方块，相对坐标将基于此方块计算
     * @return 包含三个Double值的列表，格式为[x, y, z]，与SpawnData.Pos格式一致
     */
    fun parseSetBlockCoordinates(command: String, baseBlock: Block): List<Double> {
        // 使用正则表达式提取/setblock后的三个坐标参数
        val pattern = Pattern.compile("^/setblock\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+")
        val matcher = pattern.matcher(command)

        if (!matcher.find() || matcher.groupCount() < 3) {
            throw IllegalArgumentException("无法从指令中解析坐标参数: $command")
        }

        // 提取三个坐标分量
        val xCoord = matcher.group(1)
        val yCoord = matcher.group(2)
        val zCoord = matcher.group(3)

        // 获取基准方块的坐标（转换为Double以确保类型一致）
        val baseX = baseBlock.x.toDouble()
        val baseY = baseBlock.y.toDouble()
        val baseZ = baseBlock.z.toDouble()

        // 解析每个坐标分量
        return listOf(
            parseCoordinateComponent(xCoord, baseX),
            parseCoordinateComponent(yCoord, baseY),
            parseCoordinateComponent(zCoord, baseZ)
        )
    }

    /**
     * 解析单个坐标分量，支持相对坐标和绝对坐标
     * @param coord 坐标字符串（如 "~", "~1", "123", "-247.5"）
     * @param baseValue 基准坐标值
     * @return 计算后的实际坐标值
     */
    private fun parseCoordinateComponent(coord: String, baseValue: Double): Double {
        return when {
            // 纯相对坐标：~
            coord == "~" -> baseValue
            // 带偏移的相对坐标：~数字
            coord.startsWith("~") -> {
                val offsetStr = coord.substring(1)
                val offset = if (offsetStr.isNotEmpty()) offsetStr.toDouble() else 0.0
                baseValue + offset
            }
            // 绝对坐标
            else -> coord.toDouble()
        }
    }
}