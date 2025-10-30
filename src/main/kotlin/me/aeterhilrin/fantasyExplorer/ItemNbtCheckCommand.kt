package me.aeterhilrin.fantasyExplorer

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.CommandExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
class ItemNbtCheckCommand(
    private val nbtService: NbtService,
    private val loggingService: LoggingService4Nbt
) : CommandExecutor {

    companion object {
        const val PERMISSION = "itemsnbtcheck.command.use"
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§c你没有权限使用此命令")
            return true
        }

        // 玩家与否
        if (sender !is Player) {
            sender.sendMessage("§c只有玩家才能使用此命令")
            return true
        }

        val player = sender

        // 检查手持物品
        val itemInHand = getItemInHand(player)
            ?: return sendError(player, "请手持一个物品")

        try {
            // 处理NBT数据
            val nbtResult = nbtService.getItemNbt(itemInHand, player)

            // 记录日志
            loggingService.logNbtCheck(nbtResult)

            // 发送结果给玩家
            sendNbtToPlayer(player, nbtResult)

            player.sendMessage("§a物品NBT数据检查完成")

        } catch (e: NbtProcessingException) {
            return sendError(player, "处理NBT数据失败: ${e.message}")
        } catch (e: Exception) {
            return sendError(player, "发生未知错误: ${e.message}")
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

    /**
     * 发送NBT数据给玩家
     */
    private fun sendNbtToPlayer(player: Player, result: NbtResult) {
        player.sendMessage("§6=== 物品NBT数据 ===")
        player.sendMessage("§a物品类型: §7${result.itemType}")
        player.sendMessage("§a检查时间: §7${result.timestamp}")

        if (result.hasCustomNbt) {
            player.sendMessage("§e⚠ 检测到自定义NBT数据")
        }

        player.sendMessage("§6=== 原始NBT数据 ===")

        // 分块发送NBT数据，避免消息过长
        sendChunkedMessage(player, result.rawNbt)

        player.sendMessage("§6=== 结束 ===")
    }

    /**
     * 分块发送长消息
     */
    private fun sendChunkedMessage(player: Player, message: String) {
        val chunks = message.chunked(1000) // 每1000字符一块
        chunks.forEachIndexed { index, chunk ->
            player.sendMessage("§7[${index + 1}/${chunks.size}] §f$chunk")
        }
    }

    /**
     * 发送错误消息
     */
    private fun sendError(player: Player, message: String): Boolean {
        player.sendMessage("§c$message")
        return true
    }
}