package me.aeterhilrin.fantasyExplorer

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class RecipeGetterCommand(private val recipeManager: RecipeManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}只有玩家可以使用此命令")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}用法: /recipegetter <projectname>")
            return true
        }

        val projectName = args[0]

        // 切换玩家状态
        if (recipeManager.isPlayerActive(sender.uniqueId)) {
            // 如果玩家已经在监听中，则停止监听
            val hadInput = recipeManager.deactivatePlayer(sender.uniqueId)
            if (hadInput) {
                sender.sendMessage("${ChatColor.RED}配方创建失败！您已经提供了输入容器但未完成输出容器设置。")
            } else {
                sender.sendMessage("${ChatColor.YELLOW}已退出配方提取模式")
            }
        } else {
            // 激活玩家监听
            val success = recipeManager.activatePlayer(sender, projectName)
            if (success) {
                sender.sendMessage("${ChatColor.GREEN}已开启配方提取模式 - 项目: $projectName")
                sender.sendMessage("${ChatColor.GREEN}第一步: 左键破坏容器作为配方输入")
                sender.sendMessage("${ChatColor.GREEN}第二步: Shift+左键破坏容器作为配方输出")
                sender.sendMessage("${ChatColor.GREEN}再次输入命令退出模式")
            } else {
                sender.sendMessage("${ChatColor.RED}无法初始化配方文件")
            }
        }

        return true
    }
}