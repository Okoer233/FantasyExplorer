package me.aeterhilrin.fantasyExplorer

import com.google.gson.GsonBuilder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level

/**
 * 处理村民交易数据JSON输出的服务
 */
class VillagerTradeJsonLoggingService(private val plugin: JavaPlugin) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val outputFolder: File by lazy {
        val folder = File(plugin.dataFolder, "trade_output")
        if (!folder.exists()) folder.mkdirs()
        folder
    }

    /**
     * 将村民交易数据写入Trade.json文件(为PhantomLandCore提供额外的支持，输出干净一点的json)
     */
    fun logVillagerTradeToJson(tradeData: VillagerTradeData, playerName: String): Boolean {
        return try {
            val tradeFile = File(outputFolder, "Trade.json")
            val PureTradeFile = File(outputFolder, "PureTrade.json")
            val existingData = readExistingTradeData(tradeFile)
            val existingPureData = readExistingTradeData(PureTradeFile)

            // 检查是否已存在相同村民键的交易数据
            if (existingData.containsKey(tradeData.villagerKey)) {
                plugin.logger.info("检测到重复村民交易数据，跳过写入: ${tradeData.villagerKey}")
                return false
            }

            // 添加新交易数据
            existingData[tradeData.villagerKey] = createTradeJsonObject(tradeData, playerName)
            existingPureData[tradeData.villagerKey] = createTradeJsonObjectPurely(tradeData, playerName)

            // 写回文件
            writeTradeDataToFile(tradeFile, existingData)
            writeTradeDataToFile(PureTradeFile, existingPureData)
            plugin.logger.info("村民交易数据已保存到 Trade.json(也输出了纯净版PureTrade.json): ${tradeData.villagerKey}")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "写入Trade.json失败", e)
            false
        }
    }

    /**
     * 读取现有的交易数据
     */
    private fun readExistingTradeData(tradeFile: File): MutableMap<String, Any> {
        return if (tradeFile.exists() && tradeFile.length() > 0) {
            try {
                val jsonString = tradeFile.readText()
                val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(jsonString, type).toMutableMap()
            } catch (e: Exception) {
                plugin.logger.warning("解析现有Trade.json失败，将创建新文件: ${e.message}")
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
    }

    /**
     * 创建交易JSON对象
     */
    private fun createTradeJsonObject(tradeData: VillagerTradeData, playerName: String): Map<String, Any> {
        return mapOf(
            "villager_key" to tradeData.villagerKey,
            "custom_name" to (tradeData.customName ?: "Unnamed"),
            "profession" to tradeData.profession,
            "location" to mapOf(
                "world" to (tradeData.location.world?.name ?: "unknown").toString(),
                "x" to tradeData.location.blockX,
                "y" to tradeData.location.blockY,
                "z" to tradeData.location.blockZ
            ),
            "trades" to tradeData.trades.map { trade ->
                mapOf(
                    "trade_index" to trade.tradeIndex,
                    "input1" to trade.input1?.let { createTradeItemObject(it) },
                    "input2" to trade.input2?.let { createTradeItemObject(it) },
                    "output" to createTradeItemObject(trade.output),
                    "max_uses" to trade.maxUses,
                    "current_uses" to trade.currentUses
                )
            },
            "player" to playerName,
            "timestamp" to System.currentTimeMillis(),
            "timestamp_readable" to dateFormat.format(Date())
        )
    }
    /**
     * 创建交易JSON对象（纯洁版，特供给PhantomLandEngine）
     */
    private fun createTradeJsonObjectPurely(tradeData: VillagerTradeData, playerName: String): Map<String, Any> {
        return mapOf(
            "trade_display_name" to (tradeData.customName ?: "不知名的神秘商人"),
            //"custom_name" to (tradeData.customName ?: "Unnamed"),
            //"profession" to tradeData.profession,
//            "location" to mapOf(
//                "world" to (tradeData.location.world?.name ?: "unknown").toString(),
//                "x" to tradeData.location.blockX,
//                "y" to tradeData.location.blockY,
//                "z" to tradeData.location.blockZ
//            ),
            "trades" to tradeData.trades.map { trade ->
                mapOf(
                    //"trade_index" to trade.tradeIndex,
                    "input1" to trade.input1?.let { createTradeItemObjectPurely(it) },
                    "input2" to trade.input2?.let { createTradeItemObjectPurely(it) },
                    "output" to createTradeItemObjectPurely(trade.output),
                    //"max_uses" to trade.maxUses,
                    //"current_uses" to trade.currentUses
                )
            },
//            "player" to playerName,
//            "timestamp" to System.currentTimeMillis(),
//            "timestamp_readable" to dateFormat.format(Date())
        )
    }

    /**
     * 创建交易物品JSON对象
     */
    private fun createTradeItemObject(itemData: VillagerTradeData.TradeItemData): Map<String, Any> {
        return mapOf(
            "internal_id" to itemData.internalId,
            "count" to itemData.count,
            "item_type" to itemData.itemType
        )
    }
    /**
     * 创建交易物品JSON对象(特供版，专门提供给PhantomLandCore使用)
     */
    private fun createTradeItemObjectPurely(itemData: VillagerTradeData.TradeItemData): Map<String, Any> {
        return mapOf(
            "internal_id" to itemData.internalId,
            "count" to itemData.count,
            //"item_type" to itemData.itemType
        )
    }

    /**
     * 将交易数据写入文件
     */
    private fun writeTradeDataToFile(tradeFile: File, tradeData: Map<String, Any>) {
        tradeFile.writeText(gson.toJson(tradeData))
    }

    /**
     * 获取输出文件夹路径
     */
    fun getOutputFolderPath(): String {
        return outputFolder.absolutePath
    }
}