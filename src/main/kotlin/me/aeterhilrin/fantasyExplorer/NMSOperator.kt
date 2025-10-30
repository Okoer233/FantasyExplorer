import me.aeterhilrin.fantasyExplorer.BookParser
import me.aeterhilrin.fantasyExplorer.ItemNbtParser
import me.aeterhilrin.fantasyExplorer.PinyinUtils.cleanAndToPinyin
import me.aeterhilrin.fantasyExplorer.VillagerTradeData
import org.bukkit.entity.Villager
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftVillager
import net.minecraft.server.v1_8_R3.EntityVillager
import net.minecraft.server.v1_8_R3.MerchantRecipe
import net.minecraft.server.v1_8_R3.MerchantRecipeList
import org.bukkit.Bukkit
import net.minecraft.server.v1_8_R3.ItemStack as NMSItemStack
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack

object NMSOperator {

    /**
     * 封装NMS后的工具类，逆天1.8,8版本的api,逆天ojang，e()和f()分别是获取uses和maxuses的
     * 获取村民交易数据（用于JSON输出）
     */
    fun getVillagerTradeData(villager: Villager): VillagerTradeData? {
        return try {
            val craftVillager = villager as CraftVillager
            val nmsVillager = craftVillager.handle
            val recipes = nmsVillager.getOffers(null) ?: return null

            val trades = mutableListOf<VillagerTradeData.TradeItem>()

            // 解析所有交易
            for ((index, recipe) in recipes.withIndex()) {
                val tradeItem = parseTradeRecipe(recipe, index + 1)
                if (tradeItem != null) {
                    trades.add(tradeItem)
                }
            }

            // 生成村民唯一键
            val villagerKey = generateVillagerKey(villager)

            VillagerTradeData(
                villagerKey = villagerKey,
                customName = villager.customName,
                profession = getVillagerProfession(villager),
                location = villager.location,
                trades = trades
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 生成村民唯一键（坐标_自定义名字）
     */
    private fun generateVillagerKey(villager: Villager): String {
        val location = villager.location
        val coordKey = "${location.blockX}_${location.blockY}_${location.blockZ}"
        val customName = villager.customName ?: "Unnamed"

        // 清理名字中的特殊字符
        val cleanName = customName.cleanAndToPinyin()
        return "${coordKey}_$cleanName"
    }

    /**
     * 解析单个交易配方
     */
    private fun parseTradeRecipe(recipe: MerchantRecipe, tradeIndex: Int): VillagerTradeData.TradeItem? {
        return try {
            val buyItem1 = recipe.buyItem1
            val buyItem2 = recipe.buyItem2
            val sellItem = recipe.buyItem3

            val input1 = buyItem1?.let { parseNMSItemStack(it) }
            val input2 = buyItem2?.let { parseNMSItemStack(it) }
            val output = parseNMSItemStack(sellItem) ?: return null

            VillagerTradeData.TradeItem(
                tradeIndex = tradeIndex,
                input1 = input1,
                input2 = input2,
                output = output,
                maxUses = recipe.f(),//f=maxuses
                currentUses = recipe.e()//e=uses
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析NMS物品堆栈为交易物品数据
     */
    private fun parseNMSItemStack(nmsItem: NMSItemStack): VillagerTradeData.TradeItemData? {
        return try {
            val bukkitItem = CraftItemStack.asBukkitCopy(nmsItem)

            // 获取物品类型
            val itemType = bukkitItem.type.toString()

            // 生成内部ID
            val internalId = if (isBook(bukkitItem)) {
                // 对于书本，使用BookParser获取内部ID
                val bookData = BookParser.parseBook(bukkitItem, "VillagerTradeParser")
                bookData?.title ?: itemType
            } else {
                // 对于普通物品，使用ItemNbtParser获取内部ID
                val itemData = ItemNbtParser.parseItemToItemData(bukkitItem, "VillagerTradeParser")
                itemData?.internalId ?: itemType
            }

            VillagerTradeData.TradeItemData(
                internalId = internalId,
                count = nmsItem.count,
                itemType = itemType
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查是否为书本
     */
    private fun isBook(item: ItemStack): Boolean {
        return item.type == org.bukkit.Material.WRITTEN_BOOK || item.type == org.bukkit.Material.BOOK_AND_QUILL
    }

    /**
     * 获取村民职业名称
     */
    fun getVillagerProfession(villager: Villager): String {
        return when (villager.profession) {
            Villager.Profession.FARMER -> "Farmer农夫"
            Villager.Profession.LIBRARIAN -> "LIBRARIAN图书管理员"
            Villager.Profession.PRIEST -> "PRIEST牧师"
            Villager.Profession.BLACKSMITH -> "BLACKSMITH铁匠"
            Villager.Profession.BUTCHER -> "BUTCHER屠夫"
            else -> "没有交易的普通村民普通村民"
        }
    }

    // MAN,THAT'S WHAT I GIVE U
    fun getVillagerTrades(villager: Villager): List<String> {
        val tradeInfo = mutableListOf<String>()

        try {
            val craftVillager = villager as CraftVillager
            val nmsVillager = craftVillager.handle
            val recipes = nmsVillager.getOffers(null) ?: return tradeInfo

            tradeInfo.add("${ChatColor.YELLOW}=== 村民交易数据 ===")
            tradeInfo.add("${ChatColor.YELLOW}职业: ${villager.profession}")
            tradeInfo.add("${ChatColor.YELLOW}等级: ${villager.profession}")
            tradeInfo.add("${ChatColor.YELLOW}交易数量: ${recipes.size}")
            tradeInfo.add("")

            for ((index, recipe) in recipes.withIndex()) {
                val tradeLine = getTradeDescription(recipe, index + 1)
                tradeInfo.add(tradeLine)
            }

        } catch (e: ClassCastException) {
            tradeInfo.add("${ChatColor.RED}错误: 村民实例类型转换失败")
            e.printStackTrace()
        } catch (e: Exception) {
            tradeInfo.add("${ChatColor.RED}错误: 获取交易数据时发生异常 - ${e.message}")
            e.printStackTrace()
        }

        return tradeInfo
    }

    private fun getTradeDescription(recipe: MerchantRecipe, tradeNumber: Int): String {
        val buyItem1 = recipe.buyItem1
        val buyItem2 = recipe.buyItem2
        val sellItem = recipe.buyItem3

        val builder = StringBuilder()
        builder.append("${ChatColor.GOLD}交易 $tradeNumber: ")

        if (buyItem1 != null) {
            val itemName = getItemDisplayName(buyItem1)
            builder.append("${ChatColor.WHITE}${buyItem1.count}x $itemName")
        }

        if (buyItem2 != null) {
            val itemName = getItemDisplayName(buyItem2)
            builder.append(" ${ChatColor.GRAY}+ ${buyItem2.count}x $itemName")
        }

        if (sellItem != null) {
            val itemName = getItemDisplayName(sellItem)
            builder.append(" ${ChatColor.GREEN}→ ${sellItem.count}x $itemName")
        }

        builder.append(" ${ChatColor.DARK_GRAY}(${recipe.e()}/${recipe.f()})")

        return builder.toString()
    }

    private fun getItemDisplayName(itemStack: NMSItemStack): String {
        return try {
            val bukkitItem = CraftItemStack.asBukkitCopy(itemStack)
            val meta = bukkitItem.itemMeta

            if (meta != null && meta.hasDisplayName()) {
                meta.displayName
            } else {
                bukkitItem.type.toString().lowercase().replace('_', ' ').split(' ').joinToString(" ") { it.capitalize() }
            }
        } catch (e: Exception) {
            "我也不知道这是啥"
        }
    }

    fun getNMSVillager(villager: Villager): EntityVillager? {
        return try {
            val craftVillager = villager as CraftVillager
            craftVillager.handle
        } catch (e: Exception) {
            null
        }
    }

    fun getMerchantRecipeList(villager: Villager): MerchantRecipeList? {
        return getNMSVillager(villager)?.getOffers(null)
    }
    /**
     * 从村民交易中直接提取所有涉及的物品实例（ItemStack）
     * 此方法专用于配合 LoopUtils.itemsToJson 进行序列化
     * @param villager 目标村民
     * @return 包含所有交易中输入和输出物品的ItemStack列表
     */
    fun getItemStacksFromVillagerTrades(villager: Villager): List<ItemStack> {
        val itemStacks = mutableListOf<ItemStack>()
        try {
            val craftVillager = villager as CraftVillager
            val nmsVillager = craftVillager.handle
            val recipes = nmsVillager.getOffers(null) ?: return emptyList()

            // 遍历所有交易配方
            for (recipe in recipes) {
                // 解析并添加输入物品1
                recipe.buyItem1?.let { buyItem1 ->
                    CraftItemStack.asBukkitCopy(buyItem1)?.let { bukkitItem ->
                        if (bukkitItem.type != Material.AIR) {
                            itemStacks.add(bukkitItem)
                        }
                    }
                }
                // 解析并添加输入物品2（如果存在）
                recipe.buyItem2?.let { buyItem2 ->
                    CraftItemStack.asBukkitCopy(buyItem2)?.let { bukkitItem ->
                        if (bukkitItem.type != Material.AIR) {
                            itemStacks.add(bukkitItem)
                        }
                    }
                }
                // 解析并添加输出物品
                recipe.buyItem3?.let { sellItem ->
                    CraftItemStack.asBukkitCopy(sellItem)?.let { bukkitItem ->
                        if (bukkitItem.type != Material.AIR) {
                            itemStacks.add(bukkitItem)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 记录错误，但返回已成功解析的部分物品
            Bukkit.getLogger().warning("从村民交易中提取物品时发生错误: ${e.message}")
        }
        return itemStacks
    }

}
