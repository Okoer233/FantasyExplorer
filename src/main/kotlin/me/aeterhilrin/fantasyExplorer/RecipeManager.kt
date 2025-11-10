package me.aeterhilrin.fantasyExplorer

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import me.aeterhilrin.fantasyExplorer.PinyinUtils.cleanAndToPinyin
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.UUID
import java.util.logging.Level

class RecipeManager(private val plugin: FantasyExplorer) {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val activePlayers = mutableMapOf<UUID, PlayerRecipeState>()
    private val loopUtils = LoopUtils(plugin)
    private val validContainerTypes = setOf(
        Material.CHEST, Material.TRAPPED_CHEST, Material.HOPPER, Material.DROPPER, Material.DISPENSER,
        Material.FURNACE, Material.BURNING_FURNACE
    )

    /**
     * 玩家配方状态
     */
    data class PlayerRecipeState(
        val player: Player,
        val projectName: String,
        val recipeFile: File,
        var currentStep: RecipeStep = RecipeStep.WAITING_INPUT,
        var inputItems: MutableMap<String, Int> = mutableMapOf(),
        var outputItems: MutableMap<String, Int> = mutableMapOf()
    )

    enum class RecipeStep {
        WAITING_INPUT,      // 等待输入容器
        WAITING_OUTPUT,     // 等待输出容器
    }

    /**
     * 激活玩家监听
     */
    fun activatePlayer(player: Player, projectName: String): Boolean {
        try {
            // 创建配方目录和文件
            val recipeFolder = File(plugin.dataFolder, "recipe")
            if (!recipeFolder.exists()) recipeFolder.mkdirs()

            val recipeFile = File(recipeFolder, "$projectName.json")
            if (!recipeFile.exists()) {
                recipeFile.createNewFile()
                recipeFile.writeText("{}") // 初始化为空JSON对象
            }

            val playerState = PlayerRecipeState(player, projectName, recipeFile)
            activePlayers[player.uniqueId] = playerState

            return true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "激活玩家配方监听失败", e)
            return false
        }
    }

    /**
     * 停用玩家监听
     */
    fun deactivatePlayer(playerId: UUID): Boolean {
        val playerState = activePlayers[playerId] ?: return false
        val hadInput = playerState.currentStep == RecipeStep.WAITING_OUTPUT
        activePlayers.remove(playerId)
        return hadInput
    }

    /**
     * 检查玩家是否处于激活状态
     */
    fun isPlayerActive(playerId: UUID): Boolean {
        return activePlayers.containsKey(playerId)
    }

    /**
     * 处理容器破坏事件
     */
    fun handleContainerBreak(event: BlockBreakEvent): Boolean {
        val player = event.player
        val playerState = activePlayers[player.uniqueId] ?: return false

        val block = event.block
        if (!isValidContainer(block)) return false

        // 检查容器是否有物品
        if (!hasItems(block)) {
            player.sendMessage("${ChatColor.RED}容器必须是空的！")
            return false
        }

        event.isCancelled = true // 取消破坏事件

        when {
            // 输入阶段：左键破坏
            !player.isSneaking && playerState.currentStep == RecipeStep.WAITING_INPUT -> {
                return handleInputContainer(playerState, block)
            }
            // 输出阶段：Shift+左键破坏
            player.isSneaking && playerState.currentStep == RecipeStep.WAITING_OUTPUT -> {
                return handleOutputContainer(playerState, block)
            }
            else -> {
                player.sendMessage("${ChatColor.RED}当前阶段不允许此操作！")
                return true
            }
        }
    }

    /**
     * 处理输入容器
     */
    private fun handleInputContainer(playerState: PlayerRecipeState, block: Block): Boolean {
        try {
            val inventory = (block.state as InventoryHolder).inventory
            val items = getContainerItems(inventory)

            if (items.isEmpty()) {
                playerState.player.sendMessage("${ChatColor.RED}输入容器不能为空！")
                return true
            }

            // 处理物品到JSON
            loopUtils.processContainerItems(playerState.player.name, inventory)

            // 记录物品内部ID和数量
            playerState.inputItems.clear()
            items.forEach { (internalId, count) ->
                playerState.inputItems[internalId] = count
            }

            playerState.currentStep = RecipeStep.WAITING_OUTPUT
            playerState.player.sendMessage("${ChatColor.GREEN}已记录配方输入！现在请Shift+左键破坏输出容器")

            return true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "处理输入容器失败", e)
            playerState.player.sendMessage("${ChatColor.RED}处理输入容器时发生错误")
            return true
        }
    }

    /**
     * 处理输出容器
     */
    private fun handleOutputContainer(playerState: PlayerRecipeState, block: Block): Boolean {
        try {
            val inventory = (block.state as InventoryHolder).inventory
            val items = getContainerItems(inventory)

            if (items.isEmpty()) {
                playerState.player.sendMessage("${ChatColor.RED}输出容器不能为空！")
                return true
            }

            // 处理物品到JSON
            loopUtils.processContainerItems(playerState.player.name, inventory)

            // 记录物品内部ID和数量
            playerState.outputItems.clear()
            items.forEach { (internalId, count) ->
                playerState.outputItems[internalId] = count
            }

            // 生成配方名
            val recipeName = generateRecipeName(playerState.outputItems.keys.toList())

            // 保存配方
            val success = saveRecipeToFile(playerState, recipeName)

            if (success) {
                playerState.player.sendMessage("${ChatColor.GREEN}配方 '$recipeName' 创建成功！")
                // 重置状态，准备下一个配方
                playerState.inputItems.clear()
                playerState.outputItems.clear()
                playerState.currentStep = RecipeStep.WAITING_INPUT
                playerState.player.sendMessage("${ChatColor.GREEN}可以继续创建下一个配方，或再次输入命令退出")
            } else {
                playerState.player.sendMessage("${ChatColor.RED}配方保存失败！")
            }

            return true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "处理输出容器失败", e)
            playerState.player.sendMessage("${ChatColor.RED}处理输出容器时发生错误")
            return true
        }
    }

    /**
     * 生成配方名（处理重名情况）
     */
    private fun generateRecipeName(outputItems: List<String>): String {
        var baseName = outputItems.joinToString("_")
        var recipeName = baseName
        var counter = 1

        // 这里需要从文件中读取现有配方名来检查重复
        // 由于需要读取文件，这里先返回基础名称，在保存时处理重复
        return baseName
    }

    /**
     * 保存配方到文件
     */
    private fun saveRecipeToFile(playerState: PlayerRecipeState, baseRecipeName: String): Boolean {
        return try {
            val existingData = readRecipeFile(playerState.recipeFile)
            val recipeName = getUniqueRecipeName(existingData, baseRecipeName)

            // 构建配方数据
            val recipeData = mutableMapOf<String, Any>()
            recipeData["Input"] = playerState.inputItems.map { (internalId, count) ->
                mapOf("internal_id" to internalId, "count" to count)
            }
            recipeData["Output"] = playerState.outputItems.map { (internalId, count) ->
                mapOf("internal_id" to internalId, "count" to count)
            }

            // 添加到现有数据
            existingData[recipeName] = recipeData

            // 写回文件
            playerState.recipeFile.writeText(gson.toJson(existingData))

            plugin.logger.info("玩家 ${playerState.player.name} 创建了配方: $recipeName")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "保存配方失败", e)
            false
        }
    }

    /**
     * 获取唯一的配方名
     */
    private fun getUniqueRecipeName(existingData: MutableMap<String, Any>, baseName: String): String {
        var recipeName = baseName
        var counter = 1

        while (existingData.containsKey(recipeName)) {
            recipeName = "${baseName}_$counter"
            counter++
        }

        return recipeName
    }

    /**
     * 读取配方文件
     */
    private fun readRecipeFile(file: File): MutableMap<String, Any> {
        return if (file.exists() && file.length() > 0) {
            try {
                val jsonString = file.readText()
                val type = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(jsonString, type).toMutableMap()
            } catch (e: Exception) {
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
    }

    /**
     * 获取容器内物品的内部ID和数量
     */
    private fun getContainerItems(inventory: Inventory): Map<String, Int> {
        val items = mutableMapOf<String, Int>()

        inventory.contents.forEach { itemStack ->
            if (itemStack != null && itemStack.type != Material.AIR) {
                val internalId = getItemInternalId(itemStack)
                items[internalId] = items.getOrDefault(internalId, 0) + itemStack.amount
            }
        }

        return items
    }

    /**
     * 获取物品内部ID
     */
    private fun getItemInternalId(itemStack: ItemStack): String {
        // 获取物品显示名称
        val itemMeta = itemStack.itemMeta
        val displayName = itemMeta?.displayName

        return if (displayName != null) {
            val itemData = ItemNbtParser.parseItemToItemData(itemStack, "RecipeGetter4it")
            itemData?.internalId?.replace("_","") ?: itemStack.type.name
        } else {
            itemStack.type.name
        }
    }

    /**
     * 检查是否为有效容器
     */
    private fun isValidContainer(block: Block): Boolean {
        return validContainerTypes.contains(block.type)
    }

    /**
     * 检查容器是否有物品
     */
    private fun hasItems(block: Block): Boolean {
        if (block.state !is InventoryHolder) return false

        val inventory = (block.state as InventoryHolder).inventory
        return inventory.contents.any { it != null && it.type != Material.AIR }
    }
}