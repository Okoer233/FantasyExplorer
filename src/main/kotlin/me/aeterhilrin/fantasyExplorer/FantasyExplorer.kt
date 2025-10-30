package me.aeterhilrin.fantasyExplorer

import org.bukkit.plugin.java.JavaPlugin
//todo:完成提取器的善后工作
class FantasyExplorer : JavaPlugin() {
    private lateinit var nbtService: NbtService
    private lateinit var loggingService: LoggingService4Nbt
    private lateinit var nbtCheckCommand: ItemNbtCheckCommand
    private lateinit var spawnerCommand: SpawnerCommand
    private lateinit var enhancedItemCommand: EnhancedItemNbtCheckCommand
    private lateinit var bookCheckCommand: BookCheckCommand
    private lateinit var spawnerHandler: SpawnerHandler
    private lateinit var spawnerJsonLoggingService: SpawnerJsonLoggingService
    private lateinit var spawnerLoggingService: SpawnerLoggingService
    private lateinit var villagerCheckHandler: VillagerCheckHandler
    private lateinit var villagerCheckCommand: VillagerCheckCommand
    private lateinit var villagerTradeJsonLoggingService: VillagerTradeJsonLoggingService
    private lateinit var loopUtils: LoopUtils
    private lateinit var loopCommand: LoopCommand
    private lateinit var recipeManager: RecipeManager
    private lateinit var recipeListener: RecipeListener
    companion object {
        lateinit var instance: FantasyExplorer
            private set
    }
    override fun onEnable() {
        initializeServices()
        registerCommands()
        registerEvents()
        ensureDataFolder()
        try {
            // 检查 NBT-API 主类是否存在
            Class.forName("de.tr7zw.changeme.nbtapi.NBT")
            logger.info { "nbt类没问题" }
        } catch (e: ClassNotFoundException) {
            logger.info{"what can i say"}
        }
        logger.info("已完成加载Nbt结构获取器加载，已完成byd的刷怪笼检测器和遍历小工具")
    }


    override fun onDisable() {
        logger.info("已完成关闭")
    }
    private fun initializeServices() {
        nbtService = NbtService()
        loggingService = LoggingService4Nbt(this)
        nbtCheckCommand = ItemNbtCheckCommand(nbtService, loggingService)
        enhancedItemCommand = EnhancedItemNbtCheckCommand(this)
        bookCheckCommand = BookCheckCommand(this)
        // 初始化刷怪笼相关服务
        spawnerJsonLoggingService = SpawnerJsonLoggingService(this)
        spawnerLoggingService = SpawnerLoggingService(this)
        spawnerHandler = SpawnerHandler(spawnerLoggingService, spawnerJsonLoggingService)
        spawnerCommand = SpawnerCommand(spawnerHandler)
        // 初始化村民检查相关服务
        villagerCheckHandler = VillagerCheckHandler(this)
        villagerCheckCommand = VillagerCheckCommand(villagerCheckHandler)
        villagerTradeJsonLoggingService = VillagerTradeJsonLoggingService(this)
        loopUtils = LoopUtils(this)
        loopCommand = LoopCommand(this)
        recipeManager = RecipeManager(this)
        recipeListener = RecipeListener(recipeManager)
    }

    /**
     * 注册命令
     */
    private fun registerCommands() {
        getCommand("itemsnbtcheck")?.setExecutor(nbtCheckCommand)
        getCommand("spawnercommandhandle")?.setExecutor(spawnerCommand)
        getCommand("enhanceditemsnbtcheck")?.setExecutor(enhancedItemCommand)
        getCommand("bookcheck")?.setExecutor(bookCheckCommand)
        getCommand("villagercheck")?.setExecutor(villagerCheckCommand)
        getCommand("loop")?.setExecutor(loopCommand)
        getCommand("recipegetter")?.setExecutor(RecipeGetterCommand(recipeManager))
    }

    /**
     * 注册事件监听器
     */
    private fun registerEvents() {
        server.pluginManager.registerEvents(spawnerHandler, this)
        server.pluginManager.registerEvents(villagerCheckHandler, this)
        server.pluginManager.registerEvents(recipeListener, this)
    }

    /**
     * 确保数据目录存在
     */
    private fun ensureDataFolder() {
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }
    }}
