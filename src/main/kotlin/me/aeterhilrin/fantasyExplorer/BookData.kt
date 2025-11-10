package me.aeterhilrin.fantasyExplorer

/**
 * 专门用于存储书本解析结果的数据类
 * 与原有物品解析逻辑完全独立,不用担心串台——独立的原因是，enc没法解析书本数据，但塞一起又太冗杂了
 */
data class BookData(
    // 书本核心数据
    val title: String,           // 书名（包含颜色代码）
    val author: String?,         // 作者
    val pages: List<String>,     // 页面内容
    val lore: List<String>,      // 描述文本

    // 解析元数据
    val playerName: String,      // 检查的玩家
    val itemType: String,        // 原始物品类型
    val timestamp: Long = System.currentTimeMillis(),

    // 原始数据备份
    val rawNbt: String
)