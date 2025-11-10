package me.aeterhilrin.fantasyExplorer

import de.tr7zw.changeme.nbtapi.NBTItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class NbtService {

    companion object {
        const val MAX_NBT_LENGTH = 30000 // 限制NBT字符串长度，防止内存溢出
    }

    /**
     * 获取物品的NBT数据
     */
    fun getItemNbt(itemStack: ItemStack, player: Player): NbtResult {
        try {
            val nbtItem = NBTItem(itemStack)
            val nbtString = extractNbtToString(nbtItem)

            return NbtResult(
                rawNbt = nbtString,
                itemType = itemStack.type.name,
                playerName = player.name,
            )
        } catch (e: Exception) {
            throw NbtProcessingException("处理物品NBT时发生错误: ${e.message}", e)
        }
    }

    /**
     * 提取NBT数据并转换为字符串
     */
    private fun extractNbtToString(nbtItem: NBTItem): String {
        val compound = nbtItem.compound
        val nbtString = compound.toString()

        // 限制NBT字符串长度，防止过长的数据导致问题
        return if (nbtString.length > MAX_NBT_LENGTH) {
            nbtString.substring(0, MAX_NBT_LENGTH) + "... [截断]"
        } else {
            nbtString
        }
    }

}

/**
 * NBT处理异常类
 */
class NbtProcessingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
