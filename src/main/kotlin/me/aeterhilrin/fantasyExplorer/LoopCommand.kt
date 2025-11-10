package me.aeterhilrin.fantasyExplorer

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LoopCommand(private val plugin: FantasyExplorer) : CommandExecutor {

    private val loopUtils = LoopUtils(plugin)
    private var taskManager: LoopTaskManager? = null

    // 盘灵大陆预设坐标，约7w区块，用时大概是要1小时，用法/loop panlingcontinent
    private val panlingContinentCoords = mapOf(
        "x1" to 3582, "z1" to 1630, "x2" to -1250, "z2" to -2360
    )

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("fantasyexplorer.loop")) {
            sender.sendMessage("§c你没有权限使用此命令")
            return true
        }

        if (taskManager?.isRunning() == true) {
            if (args.isNotEmpty() && args[0].equals("stop", ignoreCase = true)) {
                stopCurrentTask()
                sender.sendMessage("§a已停止当前遍历任务")
                return true
            }
            sender.sendMessage("§c已有遍历任务正在运行，请等待完成或使用 /loop stop 停止当前任务")
            return true
        }

        when {
            args.isEmpty() -> {
                showUsage(sender)
                return true
            }

            args[0].equals("stop", ignoreCase = true) -> {
                sender.sendMessage("§c当前没有运行中的任务")
                return true
            }

            args[0].equals("panlingcontinent", ignoreCase = true) -> {
                val x1 = panlingContinentCoords["x1"]!!
                val z1 = panlingContinentCoords["z1"]!!
                val x2 = panlingContinentCoords["x2"]!!
                val z2 = panlingContinentCoords["z2"]!!
                startLoop(sender, x1, z1, x2, z2)
            }

            args.size == 4 -> {
                try {
                    val x1 = args[0].toInt()
                    val z1 = args[1].toInt()
                    val x2 = args[2].toInt()
                    val z2 = args[3].toInt()
                    startLoop(sender, x1, z1, x2, z2)
                } catch (e: NumberFormatException) {
                    sender.sendMessage("§c坐标参数必须是整数")
                    return true
                }
            }

            else -> {
                showUsage(sender)
                return true
            }
        }

        return true
    }

    private fun showUsage(sender: CommandSender) {
        sender.sendMessage("§c用法: /loop <x1> <z1> <x2> <z2>")
        sender.sendMessage("§c或: /loop panlingcontinent")
        sender.sendMessage("§c或: /loop stop (停止当前任务)")
    }

    private fun startLoop(sender: CommandSender, x1: Int, z1: Int, x2: Int, z2: Int) {
        val world = Bukkit.getWorlds().firstOrNull() ?: run {
            sender.sendMessage("§c无法获取世界")
            return
        }

        // 计算区块范围
        val chunkX1 = x1 shr 4
        val chunkZ1 = z1 shr 4
        val chunkX2 = x2 shr 4
        val chunkZ2 = z2 shr 4

        val minChunkX = minOf(chunkX1, chunkX2)
        val maxChunkX = maxOf(chunkX1, chunkX2)
        val minChunkZ = minOf(chunkZ1, chunkZ2)
        val maxChunkZ = maxOf(chunkZ1, chunkZ2)

        val totalChunks = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1)

        if (totalChunks <= 0) {
            sender.sendMessage("§c无效的坐标范围")
            return
        }

        plugin.logger.info("开始遍历区块范围: X[$minChunkX-$maxChunkX] Z[$minChunkZ-$maxChunkZ]")
        plugin.logger.info("总共需要处理 $totalChunks 个区块")

        taskManager = LoopTaskManager(plugin, loopUtils)
        taskManager!!.startLoopTask(world, minChunkX, maxChunkX, minChunkZ, maxChunkZ, totalChunks)

        if (sender is Player) {
            sender.sendMessage("§a已开始遍历区块，请查看控制台获取进度信息")
        }
    }

    private fun stopCurrentTask() {
        taskManager?.stopCurrentTask()
        taskManager = null
    }
}