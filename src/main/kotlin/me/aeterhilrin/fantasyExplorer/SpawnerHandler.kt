package me.aeterhilrin.fantasyExplorer

import org.bukkit.block.Block

class SpawnerHandler(
    val loggingService: SpawnerLoggingService,
    private val jsonLoggingService: SpawnerJsonLoggingService // 新增JSON服务
) : org.bukkit.event.Listener {
    val activeHandlers = mutableSetOf<java.util.UUID>()


    fun activateHandler(player: org.bukkit.entity.Player) {
        activeHandlers.add(player.uniqueId)
    }

    fun deactivateHandler(player: org.bukkit.entity.Player) {
        activeHandlers.remove(player.uniqueId)
    }

    @org.bukkit.event.EventHandler
    fun onBlockBreak(event: org.bukkit.event.block.BlockBreakEvent) {
        val player = event.player
        if (player.uniqueId in activeHandlers && event.block.type == org.bukkit.Material.COMMAND) {
            event.isCancelled = true
            handleCommandBlock(event.block, player)
        }
    }

    private fun handleCommandBlock(block: org.bukkit.block.Block, player: org.bukkit.entity.Player) {
        val commandBlock = block.state as? org.bukkit.block.CommandBlock ?: return
        val command = commandBlock.command ?: return

        if (command.contains("setblock", ignoreCase = true) &&
            command.contains("mob_spawner", ignoreCase = true)) {

            val spawnerData = SpawnerDataParser.parseCommand(command,block)
            if (spawnerData != null) {
                player.sendMessage("${org.bukkit.ChatColor.GREEN}成功解析刷怪笼数据!")

                // 记录到JSON文件
                val success = jsonLoggingService.logSpawnerToJson(spawnerData, player.name)

                if (success) {
                    player.sendMessage("${org.bukkit.ChatColor.BLUE}刷怪笼数据已保存到 spawners.json")
                } else {
                    player.sendMessage("${org.bukkit.ChatColor.YELLOW}刷怪笼数据保存失败：已存在相同配置")
                }

                // 原有的日志记录
                loggingService.logSpawnerData(player, spawnerData)
            } else {
                player.sendMessage("${org.bukkit.ChatColor.RED}无法解析刷怪笼数据")
            }
        } else {
            player.sendMessage("${org.bukkit.ChatColor.YELLOW}命令方块不包含有效的刷怪笼设置指令")
        }
    }
}