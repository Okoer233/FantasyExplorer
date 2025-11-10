package me.aeterhilrin.fantasyExplorer
class SpawnerLoggingService(private val plugin: org.bukkit.plugin.java.JavaPlugin) {
    private val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val logFolder: java.io.File by lazy {
        val folder = java.io.File(plugin.dataFolder, "spawner_logs")
        if (!folder.exists()) folder.mkdirs()
        folder
    }

    fun logSpawnerSearch(player: org.bukkit.entity.Player, radius: Int, count: Int) {
        val time = dateFormat.format(java.util.Date())
        val logFile = getDailyLogFile()

        val entry = """
            |[$time] 玩家 ${player.name} 搜索了半径 $radius 内的刷怪笼
            |找到刷怪笼数量: $count
            |位置: ${player.location.world?.name}, ${player.location.x}, ${player.location.y}, ${player.location.z}
            |
        """.trimMargin()

        logFile.appendText("$entry\n\n")
    }

    fun logSpawnerData(player: org.bukkit.entity.Player, data: SpawnerData) {
        val time = dateFormat.format(java.util.Date())
        val logFile = getDailyLogFile()

        val entry = buildString {
            appendLine("[$time] 玩家 ${player.name} 解析了刷怪笼数据")
            appendLine("实体ID: ${data.entityId}")
            appendLine("自定义名称: ${data.customName ?: "无"}")
            appendLine("显示名称: ${data.customNameVisible}")
            appendLine("生成坐标: ${data.pos}")
            appendLine("生成数量: ${data.spawnCount}")
            appendLine("生成范围: ${data.spawnRange}")
            appendLine("延迟: ${data.delay}")
            appendLine("最小生成延迟: ${data.minSpawnDelay}")
            appendLine("最大生成延迟: ${data.maxSpawnDelay}")
            appendLine("玩家范围要求: ${data.requiredPlayerRange}")
            appendLine("最大附近实体数: ${data.maxNearbyEntities}")

            appendLine("属性:")
            data.attributes.forEach { attr ->
                appendLine("  - ${attr.name}: ${attr.base}")
            }

            appendLine("装备:")
            data.equipment.forEachIndexed { index, eq ->
                appendLine("  [$index] ID: ${eq.id}, 耐久: ${eq.damage}, 数量: ${eq.count}")
                appendLine("    名称: ${eq.name ?: "无"}")
                appendLine("    描述: ${eq.lore?.joinToString(", ") ?: "无"}")
            }

            appendLine("原始NBT数据:")
            appendLine(data.rawNbt)
            appendLine()
        }

        logFile.appendText(entry)
    }

    private fun getDailyLogFile(): java.io.File {
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())
        return java.io.File(logFolder, "spawner_log_$dateStr.log")
    }
}
