package me.aeterhilrin.fantasyExplorer

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld
import net.minecraft.server.v1_8_R3.BlockPosition
import net.minecraft.server.v1_8_R3.TileEntityCommand
import java.util.logging.Level

/**
 * 使用方法：CommandBlockExecutor.executeCommandBlock(plugin, location)
 */
object CommandBlockExecutor {

    /**
     * 执行指定位置的命令方块中的命令
     */
    fun executeCommandBlock(plugin: JavaPlugin, location: Location): Boolean {
        return try {
            val block = location.block

            // 检查是否为命令方块
            if (!isCommandBlock(block.type)) {
                plugin.logger.warning("位置 $location 不是命令方块，实际方块类型: ${block.type}")
                return false
            }

            val nmsWorld = (location.world as CraftWorld).handle
            val blockPos = BlockPosition(location.blockX, location.blockY, location.blockZ)

            // 获取 TileEntityCommand
            val tileEntity = nmsWorld.getTileEntity(blockPos) as? TileEntityCommand
            if (tileEntity == null) {
                plugin.logger.warning("无法获取位置 $location 的命令方块 TileEntity")
                return false
            }

            val commandBlock = tileEntity.commandBlock
            val command = commandBlock.command

            // 检查命令是否为空
            if (command.isNullOrEmpty()) {
                plugin.logger.warning("位置 $location 的命令方块中的命令为空")
                return false
            }

            plugin.logger.info("执行命令方块命令: $command")

            // 核心执行 - 调用 CommandBlockListenerAbstract.a(World)
            commandBlock.a(nmsWorld)

            val successCount = commandBlock.j()  // 获取执行成功计数
            val success = successCount > 0

            if (success) {
                plugin.logger.info("命令执行成功")
            } else {
                plugin.logger.warning("命令执行失败")
            }

            success

        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "执行位置 $location 的命令方块时发生错误: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * 安全执行命令方块（在主线程中执行）
     * @param plugin 插件实例（必需）
     * @param location 命令方块的位置
     * @param callback 执行完成后的回调函数，参数为是否成功
     */
    fun executeCommandBlockSafely(plugin: JavaPlugin, location: Location, callback: ((Boolean) -> Unit)? = null) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val success = executeCommandBlock(plugin, location)
            callback?.invoke(success)
        })
    }

    /**
     * 获取命令方块信息
     * @param plugin 插件实例（必需）
     * @param location 命令方块的位置
     * @return 命令方块信息，如果该位置不是命令方块则返回null
     */
    fun getCommandBlockInfo(plugin: JavaPlugin, location: Location): CommandBlockInfo? {
        return try {
            val block = location.block
            if (!isCommandBlock(block.type)) {
                return null
            }

            val nmsWorld = (location.world as CraftWorld).handle
            val blockPos = BlockPosition(location.blockX, location.blockY, location.blockZ)
            val tileEntity = nmsWorld.getTileEntity(blockPos) as? TileEntityCommand ?: return null

            val commandBlock = tileEntity.commandBlock
            CommandBlockInfo(
                command = commandBlock.command ?: "",
                successCount = commandBlock.j(),
                customName = commandBlock.name,
                lastOutput = commandBlock.k()?.c(),
                blockType = block.type.name,
                location = location
            )
        } catch (e: Exception) {
            plugin.logger.warning("获取命令方块信息时发生错误: ${e.message}")
            null
        }
    }

    /**
     * 设置命令方块的命令
     * @param plugin 插件实例（必需）
     * @param location 命令方块的位置
     * @param command 要设置的命令
     * @return 设置是否成功
     */
    fun setCommandBlockCommand(plugin: JavaPlugin, location: Location, command: String): Boolean {
        return try {
            val block = location.block
            if (!isCommandBlock(block.type)) {
                return false
            }

            val nmsWorld = (location.world as CraftWorld).handle
            val blockPos = BlockPosition(location.blockX, location.blockY, location.blockZ)
            val tileEntity = nmsWorld.getTileEntity(blockPos) as? TileEntityCommand ?: return false

            tileEntity.commandBlock.setCommand(command)
            tileEntity.update()  // 更新 TileEntity
            plugin.logger.info("命令方块命令设置成功: $command")
            true
        } catch (e: Exception) {
            plugin.logger.warning("设置命令方块命令时发生错误: ${e.message}")
            false
        }
    }

    /**
     * 批量执行多个命令方块
     * @param plugin 插件实例（必需）
     * @param locations 命令方块位置列表
     * @return 位置与执行结果的映射
     */
    fun executeMultipleCommandBlocks(plugin: JavaPlugin, locations: List<Location>): Map<Location, Boolean> {
        val results = mutableMapOf<Location, Boolean>()

        locations.forEach { location ->
            val success = executeCommandBlock(plugin, location)
            results[location] = success
        }

        return results
    }

    /**
     * 检查方块类型是否为命令方块
     */
    private fun isCommandBlock(material: Material): Boolean {
        return material == Material.COMMAND
    }

    /**
     * 命令方块信息数据类
     */
    data class CommandBlockInfo(
        val command: String,
        val successCount: Int,
        val customName: String,
        val lastOutput: String?,
        val blockType: String,
        val location: Location
    )
}

/**
 * 执行该位置的命令方块中的命令
 * @param plugin 插件实例（必需）
 * @return 执行是否成功（成功计数 > 0 表示成功）
 */
fun Location.executeCommandBlock(plugin: JavaPlugin): Boolean {
    return CommandBlockExecutor.executeCommandBlock(plugin, this)
}

/**
 * 安全执行该位置的命令方块（在主线程中执行）
 * @param plugin 插件实例（必需）
 * @param callback 执行完成后的回调函数，参数为是否成功
 */
fun Location.executeCommandBlockSafely(plugin: JavaPlugin, callback: ((Boolean) -> Unit)? = null) {
    CommandBlockExecutor.executeCommandBlockSafely(plugin, this, callback)
}

/**
 * 获取该位置命令方块的信息
 * @param plugin 插件实例（必需）
 * @return 命令方块信息，如果该位置不是命令方块则返回null
 */
fun Location.getCommandBlockInfo(plugin: JavaPlugin): CommandBlockExecutor.CommandBlockInfo? {
    return CommandBlockExecutor.getCommandBlockInfo(plugin, this)
}

/**
 * 设置该位置命令方块的命令
 * @param plugin 插件实例（必需）
 * @param command 要设置的命令
 * @return 设置是否成功
 */
fun Location.setCommandBlockCommand(plugin: JavaPlugin, command: String): Boolean {
    return CommandBlockExecutor.setCommandBlockCommand(plugin, this, command)
}

/**
 * 批量执行多个位置的命令方块
 * @param plugin 插件实例（必需）
 * @return 位置与执行结果的映射
 */
fun List<Location>.executeAllCommandBlocks(plugin: JavaPlugin): Map<Location, Boolean> {
    return CommandBlockExecutor.executeMultipleCommandBlocks(plugin, this)
}