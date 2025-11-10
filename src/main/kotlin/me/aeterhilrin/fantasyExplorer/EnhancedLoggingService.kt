package me.aeterhilrin.fantasyExplorer

import com.google.gson.GsonBuilder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.logging.Level

class EnhancedLoggingService(private val plugin: JavaPlugin) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val logFolder: File by lazy {
        val folder = File(plugin.dataFolder, "item_logs")
        if (!folder.exists()) folder.mkdirs()
        folder
    }

    fun logItemCheck(result: ItemParseResult) {
        logToConsole(result)
        logToDetailedFile(result)
    }

    private fun logToConsole(result: ItemParseResult) {
        plugin.logger.info("玩家 ${result.playerName} 检查了物品: ${result.internalId}")
    }

    private fun logToDetailedFile(result: ItemParseResult) {
        try {
            val timeStr = dateFormat.format(result.timestamp)
            val logFile = getDailyLogFile(result.timestamp)

            val logEntry = buildString {
                appendLine("[$timeStr] 玩家 ${result.playerName} 检查了物品")
                appendLine("内部ID: ${result.internalId}")
                appendLine("物品类型: ${result.itemType}")
                appendLine("自定义名称: ${result.customName ?: "无"}")

                appendLine("Lore:")
                if (!result.lore.isNullOrEmpty()) {
                    result.lore.forEachIndexed { index, line ->
                        appendLine("  $index: $line")
                    }
                } else {
                    appendLine("  无")
                }

                appendLine("附魔:")
                if (!result.enchantments.isNullOrEmpty()) {
                    result.enchantments.forEach { enchant ->
                        appendLine("  ${enchant.enchantmentName}: 等级 ${enchant.level}")
                    }
                } else {
                    appendLine("  无")
                }

                appendLine("属性修饰符:")
                if (!result.attributes.isNullOrEmpty()) {
                    result.attributes.forEach { attr ->
                        appendLine("  ${attr.name}: 数值 ${attr.amount}, 操作 ${attr.operation}, 属性名 ${attr.attributeName}")
                    }
                } else {
                    appendLine("  无")
                }

                // 药水效果日志
                appendLine("药水效果:")
                if (!result.potionEffects.isNullOrEmpty()) {
                    result.potionEffects.forEach { effect ->
                        val levelText = if (effect.amplifier > 0) "等级 ${effect.amplifier + 1}" else "等级 1"
                        val durationText = if (effect.duration > 0) "${effect.duration / 20}秒" else "瞬间"
                        appendLine("  ${effect.effectName}: $levelText, 持续时间: $durationText (ID: ${effect.id})")
                    }
                } else {
                    appendLine("  无")
                }

                appendLine("原始NBT: ${result.rawNbt}")
                appendLine("=".repeat(50))
            }

            logFile.appendText("$logEntry\n")
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "写入物品详细日志失败", e)
        }
    }

    private fun getDailyLogFile(timestamp: Long): File {
        val dateStr = SimpleDateFormat("yyyy-MM-dd").format(timestamp)
        return File(logFolder, "item_detail_$dateStr.log")
    }
}