package me.aeterhilrin.fantasyExplorer

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class RecipeListener(private val recipeManager: RecipeManager) : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        // 检查玩家是否处于配方提取模式
        if (recipeManager.isPlayerActive(event.player.uniqueId)) {
            // 让RecipeManager处理容器破坏事件
            val handled = recipeManager.handleContainerBreak(event)
            if (handled) {
                // 事件已被处理，确保取消
                event.isCancelled = true
            }
        }
    }
}