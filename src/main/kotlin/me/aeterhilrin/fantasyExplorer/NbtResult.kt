package me.aeterhilrin.fantasyExplorer

data class NbtResult(
    val rawNbt: String,
    val itemType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val playerName: String = "",
    val hasCustomNbt: Boolean = false
)
