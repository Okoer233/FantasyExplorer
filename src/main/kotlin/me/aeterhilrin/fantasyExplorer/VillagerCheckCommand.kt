package me.aeterhilrin.fantasyExplorer

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 村民检查命令处理器
 */
class VillagerCheckCommand(private val villagerHandler: VillagerCheckHandler) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}只有玩家可以使用此命令")
            return true
        }

        if (villagerHandler.isPlayerActive(sender.uniqueId)) {
            villagerHandler.deactivateHandler(sender)
            sender.sendMessage("${ChatColor.YELLOW}已关闭村民检查模式")
        } else {
            villagerHandler.activateHandler(sender)
            sender.sendMessage("${ChatColor.GREEN}已启用村民检查模式 - 右键点击村民来解析其交易")
        }

        return true
    }
}
