package me.aeterhilrin.fantasyExplorer

import org.bukkit.plugin.java.JavaPlugin
//todo:修复followRange的映射问题(不解决，小问题，只有留下bug你才知道跑的是代码
// todo:制作好trade转换器（已完成)
//todo:映射序号问题，已完整映射
//todo:tradde输出物品的时候物品的内部id不对劲（本次需要爆改trade,将其作为直接的交易配置读取文件）,村民的key已完成洁净修复，现已经没有_
//todo:修复potion的提取无法正确提取出喷溅型的问题（已解决，通过数据值转换Int，推测出物品类型是否为splash_potion）
//todo:修复遍历器无法获取到未加载的村民的数据的问题（已解决，nms直接调用命令方块底层处理命令）
//todo:修复activemobs,maxmobs的问题（待琢磨,还是没搞明白activemobs是干什么的，已注掉activemobs）
//todo:rabit的掉落就不修复了，自己手写Mythicmobs配置文件和交易系统的配置，真服了这byd的不命名兔子刷怪和Mythicmobs的可能误读为原版物品，不过想来我的抽象名字组合应该不至于让它误判——

//todo:bug修复PreventDrops (已修复)
//todo:light gray dye 的问题,战神族种族证明的问题和货物混了，妖族种族证明的问题,仙族证明的问题
//todo:已修复颜色映射，已跳过魔影水晶。已修复REEDS映射问题,低版本的endercrystal高版本加_,但该实体不受mythicmobs支持,低版本的reeds高版本叫sugar_cane,，低版本映射错误的物品之——煤炭和木炭，木炭才是火元素结晶
//
//todo:注意，提取出来的数据的问题，::的问题(已修复，是中文转拼音的问题)，闪烁西瓜（->GLISTERING_MELON_SLICE）的问题，西瓜片和西瓜(MELON_SLICE)的问题，fallingblock的问题（已修复，跳过fb）,command的问题(已修复)，黄金马凯（->GOLD_HORSE_ARMOR），物品属性Follow_Range(follow range不做修改)
//todo:dye的问题已于ItemDye类找到映射关系
//todo:修复怪物生成配置id的生成机制过于简单，修复了怪物的Pos为空的问题，如果为空则以该刷怪笼的命令方块坐标算出该刷怪笼的坐标作为基准点，并且其id的命名用该命令方块自己的坐标为主
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
