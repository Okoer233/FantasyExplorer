package me.aeterhilrin.fantasyExplorer

import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.logging.Level


/**
 * 遍历器专用
 * 工具类，用于批量解析物品、书本和刷怪笼数据
 * 所有消息提示直接输出到控制台和日志文件，不发送给玩家，别问我为什么，这就是我对后台的和日志文件的爱啊
 */
class LoopUtils(private val plugin: FantasyExplorer) {

    private val itemParser = ItemNbtParser
    private val bookParser = BookParser
    private val spawnerParser = SpawnerDataParser

    private val itemJsonService = ItemJsonLoggingService(plugin)
    private val bookJsonService = BookJsonLoggingService(plugin)
    private val spawnerJsonService = SpawnerJsonLoggingService(plugin)

    private val enhancedLoggingService = EnhancedLoggingService(plugin)
    private val villagerTradeJsonService = VillagerTradeJsonLoggingService(plugin)
    /**
     * 解析村民交易并保存到Trade.json
     * @param playerName 玩家名称
     * @param villager 要解析的村民
     * @return 是否成功解析和保存
     */
    fun villagerTradesToJson(playerName: String, villager: org.bukkit.entity.Villager): Boolean {
        return try {
            plugin.logger.info("开始解析村民交易数据 (解析代号: $playerName)")
            val tradeItemStacks = NMSOperator.getItemStacksFromVillagerTrades(villager)
            var itemsSavedCount = 0
            var booksSavedCount = 0

            for (itemStack in tradeItemStacks) {
                val success = if (isBook(itemStack)) {
                    bookToJson(playerName, itemStack).also { if (it) booksSavedCount++ }
                } else {
                    itemsToJson(playerName, itemStack).also { if (it) itemsSavedCount++ }
                }
            }
            plugin.logger.info("从村民交易中解析并保存了 $itemsSavedCount 个物品和 $booksSavedCount 本书本。")
            // 获取村民交易数据
            val tradeData = NMSOperator.getVillagerTradeData(villager)
            if (tradeData == null) {
                plugin.logger.warning("村民交易数据获取失败")
                return false
            }

            // 记录到JSON文件
            val success = villagerTradeJsonService.logVillagerTradeToJson(tradeData, playerName)

            // 记录详细日志到控制台
            logVillagerTradeDataToConsole(playerName, tradeData)

            if (success) {
                plugin.logger.info("成功解析村民交易数据!")
                plugin.logger.info("村民键: ${tradeData.villagerKey}")
                plugin.logger.info("自定义名称: ${tradeData.customName ?: "无"}")
                plugin.logger.info("职业: ${tradeData.profession}")
                plugin.logger.info("交易数量: ${tradeData.trades.size}")
                plugin.logger.info("村民交易数据已保存到 Trade.json")
            } else {
                plugin.logger.warning("村民交易数据保存失败：已存在相同配置")
            }
            success
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "村民交易处理过程中发生错误", e)
            false
        }
    }

    /**
     * 将村民交易数据记录到控制台
     */
    private fun logVillagerTradeDataToConsole(playerName: String, data: VillagerTradeData) {
        try {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            plugin.logger.info("[$time] 解析代号 $playerName 解析了村民交易数据")
            plugin.logger.info("村民键: ${data.villagerKey}")
            plugin.logger.info("自定义名称: ${data.customName ?: "无"}")
            plugin.logger.info("职业: ${data.profession}")
            plugin.logger.info("位置: ${data.location.world?.name}, ${data.location.blockX}, ${data.location.blockY}, ${data.location.blockZ}")
            plugin.logger.info("交易数量: ${data.trades.size}")

            data.trades.forEach { trade ->
                plugin.logger.info("交易 ${trade.tradeIndex}:")
                trade.input1?.let { input ->
                    plugin.logger.info("  输入1: ${input.internalId} x${input.count}")
                }
                trade.input2?.let { input ->
                    plugin.logger.info("  输入2: ${input.internalId} x${input.count}")
                }
                plugin.logger.info("  输出: ${trade.output.internalId} x${trade.output.count}")
                plugin.logger.info("  使用情况: ${trade.currentUses}/${trade.maxUses}")
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "记录村民交易数据到控制台失败", e)
        }
    }

    /**
     * 解析物品并保存到items.json
     * @param playerName 等效替代player.name的字符串
     * @param itemStack 要解析的物品
     * @return 是否成功解析和保存
     */
    fun itemsToJson(playerName: String, itemStack: ItemStack): Boolean {
        return try {
            plugin.logger.info("开始解析物品: ${itemStack.type} (解析代号: $playerName)")

            // 解析物品数据
            val itemData = itemParser.parseItemToItemData(itemStack, playerName)
            if (itemData == null) {
                plugin.logger.warning("物品解析失败: ${itemStack.type}")
                return false
            }

            // 记录到JSON文件
            val success = itemJsonService.logItemToJson(itemData)

            // 同时记录详细日志（模拟原有逻辑）
            val parseResult = itemParser.parseItem(itemStack, playerName)
            if (parseResult != null) {
                enhancedLoggingService.logItemCheck(parseResult)
            }

            if (success) {
                plugin.logger.info("=== 物品解析完成 ===")
                plugin.logger.info("内部ID: ${itemData.internalId}")
                plugin.logger.info("物品类型: ${itemData.itemType}")
                plugin.logger.info("自定义名称: ${itemData.customName ?: "无"}")
                plugin.logger.info("Lore行数: ${itemData.lore.size}")
                plugin.logger.info("物品数据已保存到 items.json")
            } else {
                plugin.logger.warning("保存失败：已存在相同物品")
            }

            success
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "物品处理过程中发生错误", e)
            false
        }
    }

    /**
     * 解析书本并保存到book.json
     * @param playerName 等效替代player.name的字符串
     * @param bookItem 要解析的书本物品
     * @return 是否成功解析和保存
     */
    fun bookToJson(playerName: String, bookItem: ItemStack): Boolean {
        return try {
            plugin.logger.info("开始解析书本 (解析代号: $playerName)")

            // 检查是否为书本类型
            if (bookItem.type.toString() == "AIR") {
                plugin.logger.warning("必须提供有效的书本物品")
                return false
            }

            // 解析书本数据
            val bookData = bookParser.parseBook(bookItem, playerName)
            if (bookData == null) {
                plugin.logger.warning("书本解析失败：请确保提供的是有效的成书")
                return false
            }

            // 写入JSON文件
            val success = bookJsonService.logBookToJson(bookData)

            // 发送反馈到控制台
            if (success) {
                plugin.logger.info("=== 书本解析完成 ===")
                plugin.logger.info("书名: ${bookData.title}")
                plugin.logger.info("作者: ${bookData.author ?: "未知"}")
                plugin.logger.info("页面数量: ${bookData.pages.size}")
                plugin.logger.info("描述行数（Lores）: ${bookData.lore.size}")
                plugin.logger.info("书本内容已保存到 book.json")
            } else {
                plugin.logger.warning("保存失败：已存在相同书名的书本")
            }

            success
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "书本处理过程中发生错误", e)
            false
        }
    }

    /**
     * 解析刷怪笼并保存到spawners.json
     * @param playerName 等效替代player.name的字符串
     * @param commandBlock 命令方块Block（包含刷怪笼设置命令）
     * @return 是否成功解析和保存
     */
    fun spawnerToJson(playerName: String, commandBlock: Block): Boolean {
        return try {
            plugin.logger.info("开始解析刷怪笼 (解析代号: $playerName)")

            // 检查是否为命令方块
            if (commandBlock.type != org.bukkit.Material.COMMAND) {
                plugin.logger.warning("提供的方块不是命令方块")
                return false
            }

            val commandBlockState = commandBlock.state as? org.bukkit.block.CommandBlock ?: run {
                plugin.logger.warning("无法获取命令方块状态")
                return false
            }

            val command = commandBlockState.command ?: run {
                plugin.logger.warning("命令方块中没有命令")
                return false
            }

            // 检查是否为有效的刷怪笼设置命令
            if (!command.contains("setblock", ignoreCase = true) ||
                !command.contains("mob_spawner", ignoreCase = true)) {
                plugin.logger.warning("命令方块不包含有效的刷怪笼设置指令")
                return false
            }

            // 解析刷怪笼数据
            val spawnerData = spawnerParser.parseCommand(command)
            if (spawnerData == null) {
                plugin.logger.warning("无法解析刷怪笼数据")
                return false
            }

            // 记录到JSON文件
            val success = spawnerJsonService.logSpawnerToJson(spawnerData, playerName)

            // 记录详细日志到控制台（替代原有的日志服务）
            logSpawnerDataToConsole(playerName, commandBlock.location, spawnerData)

            if (success) {
                plugin.logger.info("成功解析刷怪笼数据!")
                plugin.logger.info("实体ID: ${spawnerData.entityId}")
                plugin.logger.info("自定义名称: ${spawnerData.customName ?: "无"}")
                plugin.logger.info("生成数量: ${spawnerData.spawnCount}")
                plugin.logger.info("刷怪笼数据已保存到 spawners.json")
            } else {
                plugin.logger.warning("刷怪笼数据保存失败：已存在相同配置")
            }

            success
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "刷怪笼处理过程中发生错误", e)
            false
        }
    }

    /**
     * 将刷怪笼数据记录到控制台（替代原有的SpawnerLoggingService功能）
     */
    private fun logSpawnerDataToConsole(playerName: String, location: org.bukkit.Location, data: SpawnerData) {
        try {
            val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())

            plugin.logger.info("[$time] 解析代号 $playerName 中解析了刷怪笼数据")
            plugin.logger.info("实体ID: ${data.entityId}")
            plugin.logger.info("自定义名称: ${data.customName ?: "无"}")
            plugin.logger.info("显示名称: ${data.customNameVisible}")
            plugin.logger.info("生成坐标: ${data.pos}")
            plugin.logger.info("生成数量: ${data.spawnCount}")
            plugin.logger.info("生成范围: ${data.spawnRange}")
            plugin.logger.info("延迟: ${data.delay}")
            plugin.logger.info("最小生成延迟: ${data.minSpawnDelay}")
            plugin.logger.info("最大生成延迟: ${data.maxSpawnDelay}")
            plugin.logger.info("玩家范围要求: ${data.requiredPlayerRange}")
            plugin.logger.info("最大附近实体数: ${data.maxNearbyEntities}")

            if (data.attributes.isNotEmpty()) {
                plugin.logger.info("属性:")
                data.attributes.forEach { attr ->
                    plugin.logger.info("  - ${attr.name}: ${attr.base}")
                }
            }

            if (data.equipment.isNotEmpty()) {
                plugin.logger.info("装备:")
                data.equipment.forEachIndexed { index, eq ->
                    plugin.logger.info("  [$index] ID: ${eq.id}, 耐久: ${eq.damage}, 数量: ${eq.count}")
                    plugin.logger.info("    名称: ${eq.name ?: "无"}")
                    plugin.logger.info("    描述: ${eq.lore?.joinToString(", ") ?: "无"}")
                }
            }

            plugin.logger.info("原始NBT数据:")
            plugin.logger.info(data.rawNbt)

        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "记录刷怪笼数据到控制台失败", e)
        }
    }

    /**
     * 批量处理物品列表
     * @param playerName 玩家名称
     * @param items 物品列表
     * @return 成功处理的数量
     */
    fun batchItemsToJson(playerName: String, items: List<ItemStack>): Int {
        var successCount = 0
        items.forEach { item ->
            if (itemsToJson(playerName, item)) {
                successCount++
            }
        }
        plugin.logger.info("批量处理完成: 成功 $successCount/${items.size} 个物品")
        return successCount
    }

    /**
     * 批量处理书本列表
     * @param playerName 玩家名称
     * @param books 书本列表
     * @return 成功处理的数量
     */
    fun batchBooksToJson(playerName: String, books: List<ItemStack>): Int {
        var successCount = 0
        books.forEach { book ->
            if (bookToJson(playerName, book)) {
                successCount++
            }
        }
        plugin.logger.info("批量处理完成: 成功 $successCount/${books.size} 本书本")
        return successCount
    }

    /**
     * 批量处理刷怪笼命令方块列表
     * @param playerName 玩家名称
     * @param commandBlocks 命令方块列表
     * @return 成功处理的数量
     */
    fun batchSpawnersToJson(playerName: String, commandBlocks: List<Block>): Int {
        var successCount = 0
        commandBlocks.forEach { block ->
            if (spawnerToJson(playerName, block)) {
                successCount++
            }
        }
        plugin.logger.info("批量处理完成: 成功 $successCount/${commandBlocks.size} 个刷怪笼")
        return successCount
    }
    /**
     * 新增方法：处理交易中的单个物品，调用相应的LoopUtils方法
     */
    private fun parseItemInTrade(itemStack: ItemStack, playerName: String) {
        try {
            if (isBook(itemStack)) {
                bookToJson(playerName, itemStack)
            } else {
                itemsToJson(playerName, itemStack)
            }
        } catch (e: Exception) {
            plugin.logger.warning("处理交易物品时发生错误: ${itemStack.type} - ${e.message}")
        }
    }
    /**
     * 处理容器内的物品（某些奇妙的bug已经被修复了捏）
     */
    fun processContainerItems(playerName: String, inventory: org.bukkit.inventory.Inventory): Boolean {
        return try {
            var successCount = 0
            var totalCount = 0
            val contents = inventory.contents

            for (i in contents.indices) {
                val item = contents[i]
                if (item != null && item.type != org.bukkit.Material.AIR) {
                    totalCount++
                    val success = if (isBook(item)) {
                        bookToJson(playerName, item)
                    } else {
                        itemsToJson(playerName, item)
                    }
                    if (success) successCount++
                }
            }
            plugin.logger.info("处理容器物品完成: 成功 $successCount/$totalCount 个物品")
            successCount > 0
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "处理容器物品时发生错误", e)
            false
        }
    }

    /**
     * 判断物品是否为书本
     */
    private fun isBook(item: ItemStack): Boolean {
        return item.type == org.bukkit.Material.WRITTEN_BOOK || item.type == org.bukkit.Material.BOOK_AND_QUILL
    }
}