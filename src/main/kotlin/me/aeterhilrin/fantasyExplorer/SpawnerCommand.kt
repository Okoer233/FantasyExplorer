package me.aeterhilrin.fantasyExplorer

class SpawnerCommand(private val spawnerHandler: SpawnerHandler) : org.bukkit.command.CommandExecutor {
    override fun onCommand(
        sender: org.bukkit.command.CommandSender,
        command: org.bukkit.command.Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is org.bukkit.entity.Player) {
            sender.sendMessage("只有玩家可以使用此命令")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("${org.bukkit.ChatColor.RED}用法: /spawnercommandhandle [searchspawner|handlespawner]")
            return true
        }

        when (args[0].lowercase()) {
            "searchspawner" -> handleSearchSpawner(sender, args)
            "handlespawner" -> handleSpawnerHandler(sender)
            else -> sender.sendMessage("${org.bukkit.ChatColor.RED}未知子命令: ${args[0]}")
        }

        return true
    }

    private fun handleSearchSpawner(player: org.bukkit.entity.Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("${org.bukkit.ChatColor.RED}用法: /spawnercommandhandle searchspawner [半径]")
            return
        }

        val radius = args[1].toIntOrNull() ?: run {
            player.sendMessage("${org.bukkit.ChatColor.RED}无效的半径值: ${args[1]}")
            return
        }

        if (radius > 100) {
            player.sendMessage("${org.bukkit.ChatColor.RED}半径不能超过100")
            return
        }

        val spawners = SpawnerSearcher.findNearbySpawners(player, radius)
        if (spawners.isEmpty()) {
            player.sendMessage("${org.bukkit.ChatColor.YELLOW}在半径${radius}范围内未找到刷怪笼")
            return
        }

        player.sendMessage("${org.bukkit.ChatColor.GREEN}找到${spawners.size}个刷怪笼:")
        spawners.forEach { (location, distance) ->
            player.sendMessage("${org.bukkit.ChatColor.GRAY}位置: ${location.x}, ${location.y}, ${location.z} 距离: ${"%.1f".format(distance)}")
        }

        spawnerHandler.loggingService.logSpawnerSearch(player, radius, spawners.size)
    }

    private fun handleSpawnerHandler(player: org.bukkit.entity.Player) {
        if (spawnerHandler.activeHandlers.contains(player.uniqueId)) {
            spawnerHandler.deactivateHandler(player)
            player.sendMessage("${org.bukkit.ChatColor.YELLOW}已关闭刷怪笼处理器")
        } else {
            spawnerHandler.activateHandler(player)
            player.sendMessage("${org.bukkit.ChatColor.GREEN}已启用刷怪笼处理器 - 现在可以检查命令方块")
        }
    }
}
