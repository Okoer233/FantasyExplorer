package me.aeterhilrin.fantasyExplorer

import org.bukkit.ChatColor
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import java.util.*

/**
 * 村民检查事件处理器
 */
class VillagerCheckHandler(private val plugin: FantasyExplorer) : Listener {

    private val activePlayers = mutableSetOf<UUID>()
    private val loopUtils = LoopUtils(plugin)

    fun isPlayerActive(playerId: UUID): Boolean {
        return activePlayers.contains(playerId)
    }

    fun activateHandler(player: org.bukkit.entity.Player) {
        activePlayers.add(player.uniqueId)
    }

    fun deactivateHandler(player: org.bukkit.entity.Player) {
        activePlayers.remove(player.uniqueId)
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (!isPlayerActive(player.uniqueId)) return

        val entity = event.rightClicked
        if (entity !is Villager) return

        // 取消事件避免正常交互
        event.isCancelled = true

        // 处理村民交易解析
        handleVillagerTrade(entity, player)
    }

    private fun handleVillagerTrade(villager: Villager, player: org.bukkit.entity.Player) {
        try {
            val success = loopUtils.villagerTradesToJson(player.name, villager)

            if (success) {
                player.sendMessage("${ChatColor.GREEN}成功解析村民交易数据!")
                player.sendMessage("${ChatColor.BLUE}村民交易数据已保存到 Trade.json")
            } else {
                player.sendMessage("${ChatColor.YELLOW}村民交易数据保存失败：已存在相同配置")
            }
        } catch (e: Exception) {
            player.sendMessage("${ChatColor.RED}解析村民交易时发生错误: ${e.message}")
            plugin.logger.severe("处理村民交易时发生错误: ${e.message}")
            e.printStackTrace()
        }
    }
}