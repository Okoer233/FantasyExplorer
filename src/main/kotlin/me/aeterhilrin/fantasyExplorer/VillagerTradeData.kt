package me.aeterhilrin.fantasyExplorer

import org.bukkit.Location

/**
 * 村民交易数据类
 */
data class VillagerTradeData(
    val villagerKey: String,
    val customName: String?,
    val profession: String,
    val location: Location,
    val trades: List<TradeItem>
) {
    data class TradeItem(
        val tradeIndex: Int,
        val input1: TradeItemData?,
        val input2: TradeItemData?,
        val output: TradeItemData,
        val maxUses: Int,
        val currentUses: Int
    )

    data class TradeItemData(
        val internalId: String,
        val count: Int,
        val itemType: String
    )
}