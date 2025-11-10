package me.aeterhilrin.fantasyExplorer

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class EnhancedItemNbtCheckCommand(private var plugin: FantasyExplorer) : CommandExecutor {
    private val parser = ItemNbtParser
    private val loggingService = EnhancedLoggingService(plugin)
    private val itemJsonService = ItemJsonLoggingService(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("只有玩家才能执行此命令")
            return true
        }

        val itemInHand: ItemStack? = sender.inventory.itemInHand
        if (itemInHand == null || itemInHand.type == Material.AIR) {
            sender.sendMessage("§c你必须手持一个物品")
            return true
        }

        // 解析物品为ItemData
        val itemData = parser.parseItemToItemData(itemInHand, sender.name) ?: run {
            sender.sendMessage("§c解析物品失败")
            return true
        }

        // 记录到JSON文件
        val jsonSuccess = itemJsonService.logItemToJson(itemData)
        val result = parser.parseItem(itemInHand, sender.name) ?: run {
            sender.sendMessage("§c解析物品失败")
            return true
        }

        // 记录日志
        loggingService.logItemCheck(result)

        // 发送反馈给玩家
        sender.sendMessage("§6=== 物品解析结果 ===")
        sender.sendMessage("§a内部ID: §f${result.internalId}")
        sender.sendMessage("§a物品类型: §f${result.itemType}")
        sender.sendMessage("§a自定义名称: §f${result.customName ?: "无"}")

        sender.sendMessage("§aLore:")
        result.lore?.forEachIndexed { index, line ->
            sender.sendMessage("  §7$index: §f$line")
        } ?: sender.sendMessage("  §7无")

        sender.sendMessage("§a附魔:")
        result.enchantments?.forEach { enchant ->
            sender.sendMessage("  §7${enchant.enchantmentName}: §f等级 ${enchant.level}")
        } ?: sender.sendMessage("  §7无")

        sender.sendMessage("§a属性修饰符:")
        result.attributes?.forEach { attr ->
            sender.sendMessage("  §7${attr.name}: §f数值 ${attr.amount}, 操作 ${attr.operation}")
        } ?: sender.sendMessage("  §7无")

        // 药水效果显示
        sender.sendMessage("§a药水效果:")
        result.potionEffects?.forEach { effect ->
            val levelText = if (effect.amplifier > 0) "等级 ${effect.amplifier + 1}" else "等级 1"
            val durationText = if (effect.duration > 0) "${effect.duration / 20}秒" else "瞬间"
            sender.sendMessage("  §7${effect.effectName}: §f$levelText, 持续时间: $durationText")
        } ?: sender.sendMessage("  §7无")

        // 添加JSON保存结果反馈
        if (jsonSuccess) {
            sender.sendMessage("§b物品数据已保存到 items.json")
        } else {
            sender.sendMessage("§c物品数据保存失败：已存在相同物品")
        }

        return true
    }
}