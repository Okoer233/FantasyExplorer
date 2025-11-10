package me.aeterhilrin.fantasyExplorer

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Furnace
import org.bukkit.block.Hopper
import org.bukkit.block.Dispenser
import org.bukkit.block.Dropper
import org.bukkit.entity.EntityType
import org.bukkit.entity.Villager
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level

class LoopTaskManager(private val plugin: FantasyExplorer, private val loopUtils: LoopUtils) {

    private var currentTask: BukkitTask? = null
    private val chunkQueue = ConcurrentLinkedQueue<ChunkCoord>()
    private var processedChunks = 0
    private var totalChunks = 0
    private var startTime = 0L

    // 统计数据
    private var commandBlocksProcessed = 0
    private var containersProcessed = 0
    private var villagersProcessed = 0
    private var itemsProcessed = 0
    private var booksProcessed = 0

    fun startLoopTask(world: World, minChunkX: Int, maxChunkX: Int, minChunkZ: Int, maxChunkZ: Int, total: Int) {
        stopCurrentTask()

        chunkQueue.clear()
        for (x in minChunkX..maxChunkX) {
            for (z in minChunkZ..maxChunkZ) {
                chunkQueue.add(ChunkCoord(x, z))
            }
        }

        processedChunks = 0
        totalChunks = total
        startTime = System.currentTimeMillis()

        // 重置统计
        commandBlocksProcessed = 0
        containersProcessed = 0
        villagersProcessed = 0
        itemsProcessed = 0
        booksProcessed = 0

        plugin.logger.info("开始异步遍历任务，总共 $totalChunks 个区块")

        currentTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            Runnable { processNextChunk(world) },
            0L,
            1L
        )
    }

    private fun processNextChunk(world: World) {
        val chunkCoord = chunkQueue.poll() ?: run {
            finishLoopTask()
            return
        }

        try {
            Bukkit.getScheduler().runTask(plugin, Runnable {
                try {
                    val chunk = world.getChunkAt(chunkCoord.x, chunkCoord.z)
                    if (!chunk.isLoaded) {
                        chunk.load()
                    }

                    val chunkStats = processChunk(chunk)

                    commandBlocksProcessed += chunkStats.commandBlocks
                    containersProcessed += chunkStats.containers
                    villagersProcessed += chunkStats.villagers
                    itemsProcessed += chunkStats.items
                    booksProcessed += chunkStats.books

                    processedChunks++
                    updateProgress()
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "处理区块 (${chunkCoord.x}, ${chunkCoord.z}) 时发生错误", e)
                    processedChunks++
                    updateProgress()
                }
            })
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "调度区块处理任务时发生错误", e)
            processedChunks++
            updateProgress()
        }
    }

    private fun processChunk(chunk: Chunk): ChunkStats {
        val stats = ChunkStats()
        val playerName = "ConsoleGetter"

        try {
            // 处理方块
            for (x in 0..15) {
                for (z in 0..15) {
                    for (y in 0..255) {
                        val block = chunk.getBlock(x, y, z)

                        when {
                            isCommandBlock(block.type) -> {
                                loopUtils.spawnerToJson(playerName, block)
                                stats.commandBlocks++
                                // 检查是否为召唤村民的命令方块并执行
                                val command = getCommandBlockCommand(block)
                                if (isSummonVillagerCommand(command)) {
                                    plugin.logger.info("检测到召唤村民命令方块，正在执行: $command")

                                    // 使用Location扩展函数执行命令方块
                                    val success = block.location.executeCommandBlock(plugin)

                                    if (success) {
                                        plugin.logger.info("召唤村民命令方块执行成功")
                                    } else {
                                        plugin.logger.warning("召唤村民命令方块执行失败")
                                    }
                                }
                            }
                            isContainer(block.type) -> {
                                val containerStats = processContainer(block, playerName)
                                stats.containers++
                                stats.items += containerStats.items
                                stats.books += containerStats.books
                            }
                        }
                    }
                }
            }

            // 处理实体
            for (entity in chunk.entities) {
                if (entity.type == EntityType.VILLAGER) {
                    loopUtils.villagerTradesToJson(playerName, entity as Villager)
                    stats.villagers++
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "处理区块内容时发生错误", e)
        }

        return stats
    }

    private fun processContainer(block: Block, playerName: String): ContainerStats {
        val stats = ContainerStats()

        try {
            val blockState = block.state
            if (blockState is InventoryHolder) {
                val inventory = blockState.inventory
                val contents = inventory.contents

                for (i in contents.indices) {
                    val item = contents[i]
                    if (item != null && item.type != Material.AIR) {
                        try {
                            if (isBook(item.type)) {
                                loopUtils.bookToJson(playerName, item)
                                stats.books++
                            } else {
                                loopUtils.itemsToJson(playerName, item)
                                stats.items++
                            }
                        } catch (e: Exception) {
                            plugin.logger.log(Level.WARNING, "处理容器物品时发生错误: ${item.type}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "处理容器内容时发生错误", e)
        }

        return stats
    }

    private fun updateProgress() {
        val percentage = if (totalChunks > 0) (processedChunks * 100.0 / totalChunks).toInt() else 0
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        val chunksPerSecond = if (elapsed > 0) processedChunks.toDouble() / elapsed else 0.0
        val estimatedRemaining = if (chunksPerSecond > 0) (totalChunks - processedChunks) / chunksPerSecond else 0

        val minutesRemaining = estimatedRemaining.toInt() / 60
        val secondsRemaining = estimatedRemaining.toInt() % 60

        plugin.logger.info("进度: $processedChunks/$totalChunks ($percentage%) - " +
                "速度: ${"%.2f".format(chunksPerSecond)} 区块/秒 - " +
                "预计剩余: ${minutesRemaining}分${secondsRemaining}秒")

        if (processedChunks % 10 == 0 || processedChunks == totalChunks) {
            plugin.logger.info("统计 - 命令方块: $commandBlocksProcessed, 容器: $containersProcessed, " +
                    "村民: $villagersProcessed, 物品: $itemsProcessed, 书本: $booksProcessed")
        }
    }

    private fun finishLoopTask() {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        val minutes = elapsed / 60
        val seconds = elapsed % 60

        plugin.logger.info("=== 遍历任务完成 ===")
        plugin.logger.info("总耗时: ${minutes}分${seconds}秒")
        plugin.logger.info("处理统计:")
        plugin.logger.info("- 命令方块: $commandBlocksProcessed")
        plugin.logger.info("- 容器: $containersProcessed")
        plugin.logger.info("- 村民: $villagersProcessed")
        plugin.logger.info("- 物品: $itemsProcessed")
        plugin.logger.info("- 书本: $booksProcessed")
        plugin.logger.info("=== 任务结束 ===")

        stopCurrentTask()
    }

    fun stopCurrentTask() {
        currentTask?.cancel()
        currentTask = null
        chunkQueue.clear()
    }

    fun isRunning(): Boolean = currentTask != null && !chunkQueue.isEmpty()

    // 1.8.8兼容的判断方法
    private fun isCommandBlock(material: Material): Boolean {
        return material == Material.COMMAND ||
                material.toString().contains("COMMAND", ignoreCase = true)
    }

    private fun isContainer(material: Material): Boolean {
        return material == Material.CHEST ||
                material == Material.TRAPPED_CHEST ||
                material == Material.FURNACE ||
                material == Material.BURNING_FURNACE ||
                material == Material.HOPPER ||
                material == Material.DROPPER ||
                material == Material.DISPENSER ||
                material.toString().contains("CHEST", ignoreCase = true)
    }

    private fun isBook(material: Material): Boolean {
        return material == Material.WRITTEN_BOOK ||
                material == Material.BOOK_AND_QUILL ||
                material.toString().contains("BOOK", ignoreCase = true)
    }

    // 内部数据类
    private data class ChunkCoord(val x: Int, val z: Int)

    private class ChunkStats(
        var commandBlocks: Int = 0,
        var containers: Int = 0,
        var villagers: Int = 0,
        var items: Int = 0,
        var books: Int = 0
    )

    private class ContainerStats(
        var items: Int = 0,
        var books: Int = 0
    )
    /**
     * 从命令方块中获取命令内容
     */
    private fun getCommandBlockCommand(block: org.bukkit.block.Block): String? {
        return try {
            if (block.state is org.bukkit.block.CommandBlock) {
                val commandBlock = block.state as org.bukkit.block.CommandBlock
                commandBlock.command
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 判断命令是否为召唤村民的命令
     * 支持多种召唤村民的命令格式
     */
    private fun isSummonVillagerCommand(command: String?): Boolean {
        if (command.isNullOrBlank()) return false

        val lowerCommand = command.lowercase()

        // 匹配各种召唤村民的命令格式
        val villagerPatterns = listOf(
            "summon villager",
            "summon minecraft:villager",
            "execute.*summon villager",
            "execute.*summon minecraft:villager"
        )

        return villagerPatterns.any { pattern ->
            lowerCommand.contains(pattern) ||
                    Regex(pattern).containsMatchIn(lowerCommand)
        }

}
}