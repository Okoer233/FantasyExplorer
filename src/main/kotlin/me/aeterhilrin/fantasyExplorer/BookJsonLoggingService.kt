package me.aeterhilrin.fantasyExplorer

import com.google.gson.GsonBuilder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.logging.Level

/**
 * 专门处理书本数据JSON输出的服务
 */
class BookJsonLoggingService(private val plugin: JavaPlugin) {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val logFolder: File by lazy {
        val folder = File(plugin.dataFolder, "book_output") // 与其他日志同一文件夹
        if (!folder.exists()) folder.mkdirs()
        folder
    }

    /**
     * 将书本数据写入book.json文件
     * 会自动检测并避免重复书名
     */
    fun logBookToJson(bookData: BookData): Boolean {
        return try {
            val bookFile = File(logFolder, "book.json")
            val existingData = readExistingBookData(bookFile)

            // 检查是否已存在相同书名的书本（包含颜色代码比较）
            if (existingData.containsKey(bookData.title)) {
                plugin.logger.info("检测到重复书本，跳过写入: ${bookData.title}")
                return false
            }

            // 添加新书本数据
            existingData[bookData.title] = createBookJsonObject(bookData)

            // 写回文件
            writeBookDataToFile(bookFile, existingData)

            plugin.logger.info("书本数据已保存到 book.json: ${bookData.title}")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "写入book.json失败", e)
            false
        }
    }

    /**
     * 读取现有的书本数据
     */
    private fun readExistingBookData(bookFile: File): MutableMap<String, Any> {
        return if (bookFile.exists() && bookFile.length() > 0) {
            try {
                val jsonString = bookFile.readText()
                val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(jsonString, type).toMutableMap()
            } catch (e: Exception) {
                plugin.logger.warning("解析现有book.json失败，将创建新文件: ${e.message}")
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
    }

    /**
     * 创建书本JSON对象
     */
    private fun createBookJsonObject(bookData: BookData): Map<String, Any> {
        return mapOf(
            "title" to bookData.title,
            "author" to (bookData.author ?: "未知作者"),
            "pages" to bookData.pages,
            "pages_count" to bookData.pages.size,
            "lore" to bookData.lore,
            "lore_count" to bookData.lore.size,
            "player" to bookData.playerName,
            "item_type" to bookData.itemType,
            "timestamp" to bookData.timestamp,
            "timestamp_readable" to dateFormat.format(bookData.timestamp),
            "raw_nbt" to bookData.rawNbt
        )
    }

    /**
     * 将书本数据写入文件
     */
    private fun writeBookDataToFile(bookFile: File, bookData: Map<String, Any>) {
        bookFile.writeText(gson.toJson(bookData))
    }
}