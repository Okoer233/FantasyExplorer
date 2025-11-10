package me.aeterhilrin.fantasyExplorer

import de.tr7zw.changeme.nbtapi.NBT
import de.tr7zw.changeme.nbtapi.NBTCompound
import me.aeterhilrin.fantasyExplorer.PinyinUtils.cleanAndToPinyin
import me.aeterhilrin.fantasyExplorer.PinyinUtils.getFirstTwoZWCharsInTheFirstLineOfLoreAndTheLastTwoZWCharsInTheLastLineOfLore
import me.aeterhilrin.fantasyExplorer.PinyinUtils.getItsTypeInHigherVersion
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.logging.Level

object ItemNbtParser {
    private val enchantmentMap = mapOf(
        // 保护类附魔 (0-8)
        0 to "保护",
        1 to "火焰保护",
        2 to "摔落保护",
        3 to "爆炸保护",
        4 to "弹射物保护",
        5 to "水下呼吸",
        6 to "水下速掘",
        7 to "荆棘",
        8 to "深海探索者",

        // 武器附魔 (16-21)
        16 to "锋利",
        17 to "亡灵杀手",
        18 to "节肢杀手",
        19 to "击退",
        20 to "火焰附加",
        21 to "抢夺",

        // 工具附魔 (32-35)
        32 to "效率",
        33 to "精准采集",
        34 to "耐久",
        35 to "时运",

        // 远程武器附魔 (48-51)
        48 to "力量",
        49 to "冲击",
        50 to "火矢",
        51 to "无限",

        // 钓鱼竿附魔 (61-62)
        61 to "海之眷顾",
        62 to "饵钓"
    )

    // 药水效果ID到名称的映射（来自于NMS）
    private val potionEffectMap = mapOf(
        1 to "速度",
        2 to "缓慢",
        3 to "急迫",
        4 to "挖掘疲劳",
        5 to "力量",
        6 to "瞬间治疗",
        7 to "瞬间伤害",
        8 to "跳跃提升",
        9 to "反胃",
        10 to "生命恢复",
        11 to "抗性提升",
        12 to "防火",
        13 to "水下呼吸",
        14 to "隐身",
        15 to "失明",
        16 to "夜视",
        17 to "饥饿",
        18 to "虚弱",
        19 to "中毒",
        20 to "凋零",
        21 to "生命提升",
        22 to "伤害吸收",
        23 to "饱和"
    )

    fun parseItem(item: ItemStack, playerName: String): ItemParseResult? {
        return try {
            val nbt = (NBT.itemStackToNBT(item) as? NBTCompound) ?: return null
            val itemType = item.type.name

            // 访问嵌套的display标签
            val tag = nbt.getCompound("tag") // 先获取tag复合标签
            val display = tag?.getCompound("display") // 再从tag中获取display

            val customName = display?.getString("Name")
            val lore = display?.getStringList("Lore")

            // 自定义名称的颜色代码处理
            val cleanName = customName?.cleanAndToPinyin()
            val internalId = if (cleanName != null && cleanName.isNotBlank()) {
                "{$itemType}_{$cleanName}"
            } else {
                itemType
            }

            // 解析附魔（从tag.ench获取）
            val enchantments = parseEnchantments(tag)

            // 解析属性修饰符（从tag.AttributeModifiers获取）
            val attributes = parseAttributes(tag)

            // 解析药水效果（从tag.CustomPotionEffects获取）
            val potionEffects = parsePotionEffects(tag)

            val legacyAttributes: List<ItemAttribute> = attributes.map { attr ->
                ItemAttribute(
                    name = attr.name,
                    amount = attr.amount,
                    operation = attr.operation,
                    attributeName = attr.attributeName
                )
            }

            val legacyEnchantments: List<ItemEnchantment> = enchantments.map { enchant ->
                ItemEnchantment(
                    id = enchant.id,
                    level = enchant.level,
                    enchantmentName = enchant.enchantmentName
                )
            }

            val legacyPotionEffects: List<PotionEffect> = potionEffects.map { effect ->
                PotionEffect(
                    id = effect.id,
                    amplifier = effect.amplifier,
                    duration = effect.duration,
                    effectName = effect.effectName
                )
            }

            ItemParseResult(
                rawNbt = nbt.toString(),
                itemType = itemType,
                playerName = playerName,
                hasCustomNbt = tag != null,
                customName = customName,
                internalId = internalId,
                lore = lore,
                attributes = legacyAttributes,
                enchantments = legacyEnchantments,
                potionEffects = legacyPotionEffects
            )
        } catch (e: Exception) {
            Bukkit.getLogger().log(Level.WARNING, "解析物品NBT失败", e)
            null
        }
    }

    /**
     * 解析物品并返回ItemData对象
     */
    fun parseItemToItemData(item: ItemStack, playerName: String): ItemData? {
        return try {
            val nbt = (NBT.itemStackToNBT(item) as? NBTCompound) ?: return null
            val itemType = if (item.type == Material.POTION) {
                // 获取药水的 Damage 值（数据值）
                val damage = item.durability.toInt()

                // 根据 Damage 值判断药水类型
                if (damage >= 16384) {
                    "SPLASH_POTION" // 喷溅型药水
                } else {
                    "POTION" // 普通药水
                }
            } else{item.getItsTypeInHigherVersion()}
            //else{item.type.name}
//修复染料映射bug
            //val material = item.type.toString()
            val material = item.getItsTypeInHigherVersion().toString()

            // 解析标签数据
            val tag = nbt.getCompound("tag")
            val display = tag?.getCompound("display")

            val customName = display?.getString("Name")
            val lore = display?.getStringList("Lore") ?: emptyList()

            // 生成内部ID（用于去重）
            val cleanName = customName?.cleanAndToPinyin()
            val internalId = if (cleanName?.isNotBlank() == true) {
                "${material}_${cleanName}_${item.getFirstTwoZWCharsInTheFirstLineOfLoreAndTheLastTwoZWCharsInTheLastLineOfLore().cleanAndToPinyin()}"
            } else {
                material
            }

            // 解析各种属性
            val enchantments = parseEnchantments(tag)
            val attributes = parseAttributes(tag)
            val potionEffects = parsePotionEffects(tag) // 解析药水效果

            // 解析耐久度数据
            val unbreakable = tag?.getByte("Unbreakable")?.toInt() == 1
            val damage = tag?.getInteger("Damage") ?: 0
            val maxDurability = item.type.maxDurability.toInt()

            ItemData(
                internalId = internalId,
                itemType = itemType,
                material = material,
                customName = customName,
                lore = lore,
                enchantments = enchantments,
                attributes = attributes,
                potionEffects = potionEffects, // 新增药水效果
                unbreakable = unbreakable,
                damage = damage,
                maxDurability = maxDurability,
                playerName = playerName,
                rawNbt = nbt.toString()
            )
        } catch (e: Exception) {
            Bukkit.getLogger().log(Level.WARNING, "解析物品数据失败", e)
            null
        }
    }

    /**
     * 解析附魔数据
     */
    private fun parseEnchantments(tag: NBTCompound?): List<ItemData.EnchantmentData> {
        if (tag == null) return emptyList()

        val enchantments = mutableListOf<ItemData.EnchantmentData>()
        val enchList = tag.getCompoundList("ench") ?: return emptyList()

        for (i in 0 until enchList.size) {
            val ench = enchList[i]
            val id = ench.getInteger("id") ?: continue
            val lvl = ench.getInteger("lvl") ?: continue
            val enchantName = enchantmentMap[id] ?: "未知附魔($id)"

            enchantments.add(ItemData.EnchantmentData(id, lvl, enchantName))
        }
        return enchantments
    }

    /**
     * 解析属性修饰符
     */
    private fun parseAttributes(tag: NBTCompound?): List<ItemData.AttributeData> {
        if (tag == null) return emptyList()

        val attributes = mutableListOf<ItemData.AttributeData>()
        val modifiersList = tag.getCompoundList("AttributeModifiers") ?: return emptyList()

        for (i in 0 until modifiersList.size) {
            val modifier = modifiersList[i]
            val name = modifier.getString("Name") ?: "未知属性"
            val amount = modifier.getDouble("Amount") ?: 0.0
            val operation = modifier.getInteger("Operation") ?: 0
            val attributeName = modifier.getString("AttributeName") ?: "generic.unknown"
            val slot = modifier.getString("Slot")

            attributes.add(ItemData.AttributeData(name, amount, operation, attributeName, slot))
        }
        return attributes
    }

    /**
     * 解析药水效果
     * 药水效果存储在tag.CustomPotionEffects列表中
     */
    private fun parsePotionEffects(tag: NBTCompound?): List<ItemData.PotionEffectData> {
        if (tag == null) return emptyList()

        val potionEffects = mutableListOf<ItemData.PotionEffectData>()
        val effectsList = tag.getCompoundList("CustomPotionEffects") ?: return emptyList()

        for (i in 0 until effectsList.size) {
            val effect = effectsList[i]
            val id = effect.getInteger("Id") ?: continue
            val amplifier = effect.getInteger("Amplifier") ?: 0
            val duration = effect.getInteger("Duration") ?: 0
            val effectName = potionEffectMap[id] ?: "未知药水效果($id)"

            potionEffects.add(ItemData.PotionEffectData(id, amplifier, duration, effectName))
        }
        return potionEffects
    }
}