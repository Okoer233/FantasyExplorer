package me.aeterhilrin.fantasyExplorer

object SpawnerSearcher {
    fun findNearbySpawners(
        player: org.bukkit.entity.Player,
        radius: Int
    ): List<Pair<org.bukkit.Location, Double>> {
        val playerLoc = player.location
        val world = player.world
        val spawners = mutableListOf<Pair<org.bukkit.Location, Double>>()

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val block = world.getBlockAt(
                        playerLoc.blockX + x,
                        playerLoc.blockY + y,
                        playerLoc.blockZ + z
                    )

                    if (block.type == org.bukkit.Material.MOB_SPAWNER) {
                        val loc = block.location
                        val distance = loc.distance(playerLoc)
                        spawners.add(loc to distance)
                    }
                }
            }
        }

        return spawners.sortedBy { it.second }
    }
}
