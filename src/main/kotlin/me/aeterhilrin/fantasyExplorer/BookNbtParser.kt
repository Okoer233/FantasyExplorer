package me.aeterhilrin.fantasyExplorer

import de.tr7zw.changeme.nbtapi.NBT
import de.tr7zw.changeme.nbtapi.NBTCompound
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.logging.Level

/**
 * 专门用于解析成书的解析器
 */
object BookParser {

    /**
     * 解析成书数据
     * @return 如果解析成功返回BookData，否则返回null
     */
    fun parseBook(item: ItemStack, playerName: String): BookData? {
        return try {
            // 检查是否为成书类型
            if (!isBookItem(item)) {
                return null
            }

            val nbt = (NBT.itemStackToNBT(item) as? NBTCompound) ?: return null
            val itemType = item.type.name

            // 解析书本特定字段
            val tag = nbt.getCompound("tag") ?: return null

            val title = tag.getString("title") ?: return null
            val author = tag.getString("author")
            val pages = parseBookPages(tag)
            val lore = parseBookLore(tag)

            BookData(
                title = title,
                author = author,
                pages = pages,
                lore = lore,
                playerName = playerName,
                itemType = itemType,
                rawNbt = nbt.toString()
            )
        } catch (e: Exception) {
            org.bukkit.Bukkit.getLogger().log(Level.WARNING, "解析成书数据失败", e)
            null
        }
    }

    /**
     * 判断物品是否为成书
     */
    private fun isBookItem(item: ItemStack): Boolean {
        return item.type == Material.WRITTEN_BOOK || item.type == Material.BOOK_AND_QUILL
    }

    /**
     * 解析书本页面内容
     */
    private fun parseBookPages(tag: NBTCompound): List<String> {
        return try {
            val pagesList = tag.getStringList("pages") ?: emptyList()
            pagesList.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 解析书本描述（lore）
     */
    private fun parseBookLore(tag: NBTCompound): List<String> {
        return try {
            val display = tag.getCompound("display")
            val loreList = display?.getStringList("Lore") ?: emptyList()
            loreList.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}