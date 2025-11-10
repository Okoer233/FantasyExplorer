package me.aeterhilrin.fantasyExplorer

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.logging.Level

class LoggingService4Nbt(private val plugin: JavaPlugin) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val logFolder: File by lazy {
        val folder = File(plugin.dataFolder, "nbt_logs")
        if (!folder.exists()) folder.mkdirs()
        folder
    }

    /**
     * 记录NBT检查日志
     */
    fun logNbtCheck(result: NbtResult) {
        logToConsole(result)
        logToFile(result)
    }

    /**
     * 记录到控制台
     */
    private fun logToConsole(result: NbtResult) {
        plugin.logger.info("玩家 ${result.playerName} 检查了物品NBT: ${result.itemType} 得到物品原始Nbt数据：${result.rawNbt}")
    }

    /**
     * 记录到文件（仅记录精简数据）
     */
    private fun logToFile(result: NbtResult) {
        try {
            val timeStr = dateFormat.format(result.timestamp)
            val logFile = getDailyLogFile()

            // 只记录时间戳和物品类型
            val logEntry = "玩家 ${result.playerName} 于[$timeStr] 检查了${result.itemType}类型的物品 得到物品原始Nbt数据：${result.rawNbt}\n"

            File(logFile.parent).mkdirs() // 确保目录存在
            logFile.appendText(logEntry)

        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "写入NBT日志文件失败", e)
        }
    }


    /**
     * 获取刷怪笼指令日志文件
     */
    private fun getSpawnerLogFile(): File {
        val dateStr = SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis())
        return File(logFolder, "spawner_command_$dateStr.log")
    }

    /**
     * 获取每日日志文件
     */
    private fun getDailyLogFile(): File {
        val dateStr = SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis())
        return File(logFolder, "nbt_check_$dateStr.log")
    }
}