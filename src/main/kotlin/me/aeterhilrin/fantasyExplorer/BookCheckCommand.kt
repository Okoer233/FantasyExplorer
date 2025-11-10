package me.aeterhilrin.fantasyExplorer

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class BookCheckCommand(private val plugin: JavaPlugin) : CommandExecutor {

    private val bookParser = BookParser
    private val loggingService = BookJsonLoggingService(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§c只有玩家才能执行此命令")
            return true
        }

        val itemInHand: ItemStack = sender.inventory.itemInHand
        if (itemInHand.type.toString() == "AIR") {
            sender.sendMessage("§c你必须手持一本成书")
            return true
        }

        // 解析书本数据
        val bookData = bookParser.parseBook(itemInHand, sender.name)
        if (bookData == null) {
            sender.sendMessage("§c解析失败：请确保手持的是有效的成书")
            return true
        }

        // 写入JSON文件
        val success = loggingService.logBookToJson(bookData)

        // 发送反馈给玩家
        if (success) {
            sender.sendMessage("§6=== 书本解析完成 ===")
            sender.sendMessage("§a书名: §f${bookData.title}")
            sender.sendMessage("§a作者: §f${bookData.author ?: "未知"}")
            sender.sendMessage("§a页面数量: §f${bookData.pages.size}")
            sender.sendMessage("§a描述行数: §f${bookData.lore.size}")
            sender.sendMessage("§b书本内容已保存到 book.json")
        } else {
            sender.sendMessage("§c保存失败：已存在相同书名的书本")
        }

        return true
    }
    /**
     * 获取玩家手持物品
     */
    private fun getItemInHand(player: Player): ItemStack? {
        val item = player.inventory.itemInHand
        return if (item == null || item.type.toString() == "AIR") null else item
    }
}