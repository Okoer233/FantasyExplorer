package me.aeterhilrin.fantasyExplorer

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.Level

object PinyinUtils {
    private val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.UPPERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
    }

    /**
     * 将中文转换为拼音（首字母大写）
     */
    fun toPinyin(chinese: String): String {
        return try {
            val sb = StringBuilder()
            for (c in chinese.toCharArray()) {
                if (c.toString().matches("[\u4e00-\u9fa5]".toRegex())) {
                    val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format)
                    if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                        // 取第一个拼音，并转换为首字母大写形式
                        val pinyin = pinyinArray[0]
                        sb.append(pinyin.substring(0, 1).uppercase())
                        sb.append(pinyin.substring(1).lowercase())
                    }
                } else {
                    sb.append(c)
                }
            }
            sb.toString()
        } catch (e: BadHanyuPinyinOutputFormatCombination) {
            chinese // 转换失败时返回原字符串
        }
    }

    /**
     * 清理字符串（去除颜色代码和特殊字符）
     */
    fun cleanString(input: String): String {
        // 移除Minecraft颜色代码（§后跟任意字符）
        var cleaned = input.replace("§.", "")
        // 移除其他特殊字符，临时修改：确保非汉字都会被祛除
        //cleaned = cleaned.replace("[^a-zA-Z0-9\\u4e00-\\u9fa5]".toRegex(), "")
        cleaned = cleaned.replace("[^\u4e00-\u9fa5]".toRegex(), "")
        return cleaned
    }

    /**
     * String类型扩展函数 - 清理字符串
     * 效果等同于PinyinUtils.cleanString()
     */
    fun String.toBeCleaned(): String {
        // 移除Minecraft颜色代码（§后跟0-9a-fk-or字符）
        //var cleaned = this.replace(Regex("§[0-9a-fk-or]"), "")
        // 移除其他特殊字符，只保留字母、数字、中文和空格
        //临时修改：确保非汉字都会祛除
        //cleaned = cleaned.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]"), "")
        var cleaned = this.replace("[^\u4e00-\u9fa5]".toRegex(), "")
        cleaned = cleaned.replace(Regex("[:;,]"), "")
        return cleaned.trim()
    }

    /**
     * String类型扩展函数 - 快速转换为拼音
     * 效果等同于PinyinUtils.toPinyin()，但使用更简洁的调用方式 额外修复：我就说为什么织女这本书分配内部id的时候怎么会一直报错结构体异常，是我误会了clean!
     */
    fun String.quickToPinYin(): String {
        return try {
            val sb = StringBuilder()
            for (c in this.toCharArray()) {
                if (c.toString().matches(Regex("[\\u4e00-\\u9fa5]"))) {
                    val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, PinyinUtils.format)
                    if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                        val pinyin = pinyinArray[0]
                        // 首字母大写，其余小写
                        sb.append(pinyin.substring(0, 1).uppercase())
                        sb.append(pinyin.substring(1).lowercase())
                    }
                } else {
                    // 对于非中文字符，如果是字母则保持原样，其他字符忽略或特殊处理
                    when {
                        c.isLetter() -> sb.append(c)
                        c.isDigit() -> sb.append(c)
                        c == ' ' -> sb.append(c)
                        // 其他字符可以忽略或根据需求处理
                    }
                }
            }
            sb.toString().replace(Regex("[:;,]"), "")//内部id多 ： 的修复点在此
        } catch (e: BadHanyuPinyinOutputFormatCombination) {
            this // 转换失败时返回原字符串
        }
    }

    /**
     * 扩展函数 - 清理并转换为拼音
     */
    fun String.cleanAndToPinyin(): String {
        return this.toBeCleaned().quickToPinYin()
    }

    /**
     * 扩展函数 - 检查字符串是否包含中文
     */
    fun String.containsChinese(): Boolean {
        return this.any { it.toString().matches(Regex("[\\u4e00-\\u9fa5]")) }
    }

    /**
     * 扩展函数 - 获取字符串的首字母拼音（用于缩写）
     */
    fun String.toPinyinAbbreviation(): String {
        return try {
            val sb = StringBuilder()
            for (c in this.toCharArray()) {
                if (c.toString().matches(Regex("[\\u4e00-\\u9fa5]"))) {
                    val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, PinyinUtils.format)
                    if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                        // 只取首字母并大写
                        sb.append(pinyinArray[0].substring(0, 1).uppercase())
                    }
                } else if (c.isLetter()) {
                    // 英文字母直接取首字母并大写
                    if (sb.isEmpty() || !sb.last().isLetter()) {
                        sb.append(c.uppercaseChar())
                    }
                }
            }
            sb.toString()
        } catch (e: Exception) {
            // 如果转换失败，返回空字符串或处理后的英文字母缩写
            this.filter { it.isLetter() }.takeIf { it.isNotEmpty() }?.let {
                it.substring(0, 1).uppercase()
            } ?: ""
        }
    }

    /**
     * 扩展函数 - 获取清理后的字符串长度
     */
    val String.cleanedLength: Int
        get() = this.toBeCleaned().length

    /**
     * 扩展函数 - 检查字符串是否包含颜色代码
     */
    val String.hasColorCodes: Boolean
        get() = this.contains(Regex("§[0-9a-fk-or]"))
    //史诗级大更新
    /**
     * BookData的扩展函数，将书本数据转换为MiniMessage严格模式的YAML格式
     * 严格遵循MiniMessage规范，正确处理颜色代码冲突和标签关闭
     */
    fun BookData.transToMMYml(plugin: JavaPlugin): Boolean {
        return try {
            plugin.logger.info("开始转换书本到MiniMessage YAML格式: ${this.title}")

            val id = this.title.cleanAndToPinyin()
            if (id.isEmpty()) {
                plugin.logger.warning("书本标题转换后为空，跳过写入")
                return false
            }

            // 使用修复版的转换函数
            val mmTitle = this.title.colorToStrictMiniMessage()
            val mmAuthor = this.author?.colorToStrictMiniMessage() ?: "未知"
            val mmPages = this.pages.map { page ->
                val converted = page.colorToStrictMiniMessage()
                // 验证转换结果
                if (!isValidMiniMessage(converted)) {
                    plugin.logger.warning("检测到无效MiniMessage语法，进行清理: ${converted.take(50)}...")
                    converted.cleanMiniMessageArtifacts()
                } else {
                    converted
                }
            }

            // 构建YAML内容
            val yamlContent = buildString {
                append("$id:\n")
                append("  Id: \"${this@transToMMYml.itemType}\"\n")
                append("  Title: \"${escapeYamlString(mmTitle)}\"\n")
                append("  Display: \"${escapeYamlString(mmTitle)}\"\n")
                append("  Author: \"${escapeYamlString(mmAuthor)}\"\n")
                append("  Pages:\n")
                mmPages.forEach { page ->
                    val escapedPage = escapeYamlString(page)
                    append("    - \"$escapedPage\"\n")
                }
                //append("  Player: \"${this@transToMMYml.playerName}\"\n")
                //append("  Timestamp: ${this@transToMMYml.timestamp}\n")
            }

            writeToMMYmlFile(plugin, id, yamlContent)
            plugin.logger.info("书本数据已成功转换为MiniMessage YAML格式: $id")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "转换书本到MiniMessage YAML失败", e)
            false
        }
    }

    /**
     * 获取可读的时间戳格式
     */
    private fun getReadableTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(timestamp))
    }

    /**
     * 将Minecraft颜色代码转换为MiniMessage严格模式格式
     * 严格处理颜色代码冲突和标签关闭逻辑
     */
    fun String.colorToStrictMiniMessage(): String {
        if (this.isEmpty()) return this

        val result = StringBuilder()
        val tagStack = mutableListOf<TagInfo>()

        // 预处理：处理Unicode转义和特殊序列
        val preprocessed = preprocessSpecialSequences(this)

        var i = 0
        while (i < preprocessed.length) {
            when {
                // 处理Minecraft颜色代码 (§) - 增强检测
                i < preprocessed.length - 1 && preprocessed[i] == '§' -> {
                    val formatCode = preprocessed[i + 1]
                    handleFormatCode(formatCode, tagStack, result)
                    i += 2
                }
                // 处理XML实体转义 - 修复&apos;问题
                i < preprocessed.length - 1 && preprocessed[i] == '&' -> {
                    i += handleXmlEntity(preprocessed, i, result, tagStack)
                }
                // 处理换行符
                preprocessed[i] == '\n' -> {
                    result.append("<newline>")
                    i++
                }
                // 处理MiniMessage特殊字符转义
                preprocessed[i] == '<' -> {
                    result.append("&lt;")
                    i++
                }

                preprocessed[i] == '>' -> {
                    result.append("&gt;")
                    i++
                }
                // 处理单引号（直接输出，不转义为&apos;）
                preprocessed[i] == '\'' -> {
                    result.append("'")
                    i++
                }

                preprocessed[i] == '"' -> {
                    result.append("&quot;")
                    i++
                }
                // 处理普通字符
                else -> {
                    result.append(preprocessed[i])
                    i++
                }
            }
        }

        // 关闭所有剩余的标签
        closeAllTags(tagStack, result)

        val finalResult = result.toString()

        // 最终清理：确保没有遗留的§字符
        return finalResult.replace("§", "")
    }

    /**
     * 预处理特殊序列（Unicode转义等）
     */
    private fun preprocessSpecialSequences(input: String): String {
        var processed = input
        // 处理Unicode转义序列
        processed = processed.replace("\\u0027", "'")  // 单引号
        processed = processed.replace("\\u0022", "\"") // 双引号
        // 处理其他常见转义
        processed = processed.replace("\\\\n", "\n")   // 转义的换行符
        processed = processed.replace("\\\\t", "\t")   // 转义的制表符
        return processed
    }

    /**
     * 处理XML实体转义（修复版）
     */
    private fun handleXmlEntity(input: String, index: Int, result: StringBuilder, tagStack: MutableList<TagInfo>): Int {
        return when {
            input.startsWith("&apos;", index) -> {
                // 直接输出单引号，避免&apos;被错误解析
                result.append("'")
                6
            }

            input.startsWith("&quot;", index) -> {
                result.append("\"")
                6
            }

            input.startsWith("&amp;", index) -> {
                result.append("&")
                5
            }

            input.startsWith("&lt;", index) -> {
                result.append("<")
                4
            }

            input.startsWith("&gt;", index) -> {
                result.append(">")
                4
            }

            input.startsWith("&nbsp;", index) -> {
                result.append(" ")
                6
            }

            else -> {
                // 如果不是已知的XML实体，直接输出&字符
                result.append("&")
                1
            }
        }
    }

    /**
     * 处理格式代码转换（修复标签嵌套逻辑）
     */
    private fun handleFormatCode(formatCode: Char, tagStack: MutableList<TagInfo>, result: StringBuilder) {
        val mmTag = when (formatCode) {
            // 颜色代码
            '0' -> "black"
            '1' -> "dark_blue"
            '2' -> "dark_green"
            '3' -> "dark_aqua"
            '4' -> "dark_red"
            '5' -> "dark_purple"
            '6' -> "gold"
            '7' -> "gray"
            '8' -> "dark_gray"
            '9' -> "blue"
            'a' -> "green"
            'b' -> "aqua"
            'c' -> "red"
            'd' -> "light_purple"
            'e' -> "yellow"
            'f' -> "white"
            // 装饰代码
            'k' -> "obfuscated"
            'l' -> "bold"
            'm' -> "strikethrough"
            'n' -> "underlined"
            'o' -> "italic"
            // 重置代码（严格模式禁止使用）
            'r' -> {
                closeAllTags(tagStack, result)
                null
            }

            else -> null
        }

        if (mmTag != null) {
            when {
                // 颜色标签处理（互斥）
                mmTag in listOf(
                    "black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
                    "dark_purple", "gold", "gray", "dark_gray", "blue", "green",
                    "aqua", "red", "light_purple", "yellow", "white"
                ) -> {
                    closePreviousColorTags(tagStack, result)
                    result.append("<$mmTag>")
                    tagStack.add(TagInfo(mmTag, TagType.COLOR))
                }
                // 装饰标签处理（可以共存）
                mmTag in listOf("obfuscated", "bold", "strikethrough", "underlined", "italic") -> {
                    handleDecorationTag(mmTag, tagStack, result)
                }
            }
        }
    }

    /**
     * 处理装饰标签（修复共存逻辑）
     */
    private fun handleDecorationTag(tagName: String, tagStack: MutableList<TagInfo>, result: StringBuilder) {
        val existingIndex = tagStack.indexOfLast { it.name == tagName }

        if (existingIndex != -1) {
            // 关闭该装饰标签及其后的所有标签
            closeTagsFromIndex(tagStack, existingIndex, result)
        } else {
            // 打开新装饰标签
            result.append("<$tagName>")
            tagStack.add(TagInfo(tagName, TagType.DECORATION))
        }
    }

    /**
     * 关闭之前的所有颜色标签（修复互斥逻辑）
     */
    private fun closePreviousColorTags(tagStack: MutableList<TagInfo>, result: StringBuilder) {
        val lastColorIndex = tagStack.indexOfLast { it.type == TagType.COLOR }
        if (lastColorIndex != -1) {
            // 重新打开颜色标签之前的所有装饰标签
            val decorationsToKeep = tagStack.subList(0, lastColorIndex)
                .filter { it.type == TagType.DECORATION }

            // 关闭从最后一个颜色标签开始的所有标签
            closeTagsFromIndex(tagStack, lastColorIndex, result)

            // 重新打开需要保留的装饰标签
            decorationsToKeep.forEach { tag ->
                result.append("<${tag.name}>")
                tagStack.add(tag)
            }
        }
    }

    /**
     * 验证MiniMessage字符串是否合法
     */
    fun isValidMiniMessage(text: String): Boolean {
        // 检查是否包含遗留的§代码
        if (text.contains(Regex("§[0-9a-fk-or]"))) {
            return false
        }

        // 检查标签是否正确嵌套和关闭
        if (!validateTagNesting(text)) {
            return false
        }

        return true
    }

    /**
     * 验证标签嵌套是否正确
     */
    private fun validateTagNesting(text: String): Boolean {
        val stack = mutableListOf<String>()
        val tagPattern = Regex("</?(\\w+)>")

        val matches = tagPattern.findAll(text)
        for (match in matches) {
            val tag = match.groups[1]?.value ?: continue
            if (match.value.startsWith("</")) {
                // 关闭标签
                if (stack.isEmpty() || stack.last() != tag) {
                    return false
                }
                stack.removeAt(stack.size - 1)
            } else {
                // 打开标签（自闭合标签如<newline/>不需要关闭）
                if (!match.value.endsWith("/>")) {
                    stack.add(tag)
                }
            }
        }

        return stack.isEmpty()
    }

    /**
     * 清理MiniMessage字符串中的遗留代码
     */
    fun String.cleanMiniMessageArtifacts(): String {
        var cleaned = this
        // 移除所有§代码
        cleaned = cleaned.replace(Regex("§[0-9a-fk-or]"), "")
        // 移除单独的§字符
        cleaned = cleaned.replace("§", "")
        // 确保XML实体正确转义
        cleaned = cleaned.replace("&apos;", "'")
        return cleaned
    }

    /**
     * 标签信息数据类
     */
    private data class TagInfo(
        val name: String,
        val type: TagType
    )

    /**
     * 标签类型枚举
     */
    private enum class TagType {
        COLOR,      // 颜色标签（互斥）
        DECORATION, // 装饰标签（可以共存）
        RESET       // 重置标签
    }

    /**
     * 将Minecraft格式代码转换为标签信息
     */
    private fun formatCodeToTagInfo(formatCode: Char): TagInfo? {
        return when (formatCode) {
            // 颜色代码（互斥）
            '0' -> TagInfo("black", TagType.COLOR)
            '1' -> TagInfo("dark_blue", TagType.COLOR)
            '2' -> TagInfo("dark_green", TagType.COLOR)
            '3' -> TagInfo("dark_aqua", TagType.COLOR)
            '4' -> TagInfo("dark_red", TagType.COLOR)
            '5' -> TagInfo("dark_purple", TagType.COLOR)
            '6' -> TagInfo("gold", TagType.COLOR)
            '7' -> TagInfo("gray", TagType.COLOR)
            '8' -> TagInfo("dark_gray", TagType.COLOR)
            '9' -> TagInfo("blue", TagType.COLOR)
            'a' -> TagInfo("green", TagType.COLOR)
            'b' -> TagInfo("aqua", TagType.COLOR)
            'c' -> TagInfo("red", TagType.COLOR)
            'd' -> TagInfo("light_purple", TagType.COLOR)
            'e' -> TagInfo("yellow", TagType.COLOR)
            'f' -> TagInfo("white", TagType.COLOR)

            // 装饰代码（可以共存）
            'k' -> TagInfo("obfuscated", TagType.DECORATION)
            'l' -> TagInfo("bold", TagType.DECORATION)
            'm' -> TagInfo("strikethrough", TagType.DECORATION)
            'n' -> TagInfo("underlined", TagType.DECORATION)
            'o' -> TagInfo("italic", TagType.DECORATION)

            // 重置代码
            'r' -> TagInfo("reset", TagType.RESET)

            else -> null
        }
    }


    /**
     * 从指定索引开始关闭标签
     */
    private fun closeTagsFromIndex(tagStack: MutableList<TagInfo>, startIndex: Int, result: StringBuilder) {
        if (startIndex < 0 || startIndex >= tagStack.size) return

        for (i in tagStack.size - 1 downTo startIndex) {
            result.append("</${tagStack[i].name}>")
        }
        tagStack.subList(startIndex, tagStack.size).clear()
    }

    /**
     * 关闭所有活动的标签
     */
    private fun closeAllTags(tagStack: MutableList<TagInfo>, result: StringBuilder) {
        for (i in tagStack.size - 1 downTo 0) {
            result.append("</${tagStack[i].name}>")
        }
        tagStack.clear()
    }

    /**
     * YAML字符串转义处理
     */
    private fun escapeYamlString(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
            .replace("\r", "\\r")
    }

    /**
     * 写入MiniMessage YAML文件（增量写入，不破坏原有数据）
     */
    private fun writeToMMYmlFile(plugin: JavaPlugin, id: String, yamlContent: String) {
        val folder = File(plugin.dataFolder, "book_output")
        if (!folder.exists()) folder.mkdirs()

        val yamlFile = File(folder, "MMBookYml.yml")

        try {
            if (yamlFile.exists()) {
                // 读取现有内容
                val existingContent = yamlFile.readText()

                // 检查是否已存在相同ID的内容（避免重复）
                if (existingContent.contains("$id:")) {
                    plugin.logger.warning("检测到重复书本ID，跳过写入: $id")
                    return
                }

                // 增量写入：在文件末尾追加新内容
                val newContent = if (existingContent.trim().isNotEmpty()) {
                    if (existingContent.endsWith("\n")) {
                        "$existingContent$yamlContent"
                    } else {
                        "$existingContent\n$yamlContent"
                    }
                } else {
                    yamlContent
                }

                yamlFile.writeText(newContent)
            } else {
                // 文件不存在，直接写入
                yamlFile.writeText(yamlContent)
            }

            plugin.logger.info("MiniMessage YAML数据已保存到: ${yamlFile.absolutePath}")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "写入MiniMessage YAML文件失败", e)
            throw e
        }
    }

    /**
     * ItemData的扩展函数，将物品数据转换为MythicMobs格式的YAML配置
     * 严格遵循MiniMessage规范，符合MythicMobs物品配置标准
     */
    fun ItemData.transToMMYml(plugin: JavaPlugin): Boolean {
        return try {
            plugin.logger.info("开始转换物品到MythicMobs YAML格式: ${this.customName}")

            val cleanInternalId = this.internalId.replace("_", "")
            if (cleanInternalId.isEmpty()) {
                plugin.logger.warning("物品内部ID为空，跳过写入")
                return false
            }

            val itemTypeLower = this.itemType.lowercase()
            val mmDisplay = this.customName?.colorToStrictMiniMessage() ?: ""
            val mmLore = this.lore.map { it.colorToStrictMiniMessage() }

            val yamlContent = buildString {
                append("$cleanInternalId:\n")
                append("  Id: \"$itemTypeLower\"\n")

                if (mmDisplay.isNotEmpty()) {
                    append("  Display: \"${escapeYamlString(mmDisplay)}\"\n")
                }

                if (mmLore.isNotEmpty()) {
                    append("  Lore:\n")
                    mmLore.forEach { loreLine ->
                        append("    - \"${escapeYamlString(loreLine)}\"\n")
                    }
                }


                // 修复后的附魔处理
                if (this@transToMMYml.enchantments.isNotEmpty()) {
                    append("  Enchantments:\n")
                    this@transToMMYml.enchantments.forEach { enchantment ->
                        val enchantName = getEnchantmentMythicName(enchantment.enchantmentName)
                        append("    - $enchantName:${enchantment.level}\n")
                    }
                }

                // === 修复的属性处理部分 ===
                if (this@transToMMYml.attributes.isNotEmpty()) {
                    append("  Attributes:\n")

                    // 修复：显式指定 lambda 参数类型并重命名避免冲突
                    val groupedBySlot = this@transToMMYml.attributes
                        .filter { attr ->  // 重命名为 attr 避免潜在冲突
                            // 关键修复：使用 attr.attribute 而非 attribute.attribute
                            isSupportedAttribute(attr.attributeName)
                        }
                        .groupBy { attr ->  // 保持一致的重命名
                            // 处理空槽位，默认为 mainhand
                            val slot = attr.slot?.takeIf { it.isNotBlank() } ?: "mainhand"
                            slot.uppercase()
                        }

                    // 为每个槽位输出属性组
                    groupedBySlot.forEach { (slot, attributes) ->
                        append("    $slot:\n")
                        attributes.forEach { attr ->  // 这里也重命名
                            val mythicAttributeName = getAttributeMythicName(attr.attributeName)
                            // 处理乘法操作（operation = 1）
                            val amount = if (attr.operation == 1) {
                                // MythicMobs 中乘法操作需要百分比表示
                                "${attr.amount * 100}%"
                            } else {
                                attr.amount.toString()
                            }
                            append("      $mythicAttributeName: $amount\n")
                        }
                    }
                }
                if (this@transToMMYml.potionEffects.isNotEmpty()) {
                    append("  PotionEffects:\n")
                    this@transToMMYml.potionEffects.forEach { effect ->
                        val mythicEffectName = getPotionEffectMythicName(effect.effectName)
                        // MythicMobs格式：效果名{持续时间;等级;粒子效果}
                        append("    - $mythicEffectName{d=${effect.duration};l=${effect.amplifier + 1};p=true}\n")
                    }
                }

                // 其他处理逻辑保持不变...
                if (this@transToMMYml.unbreakable) {
                    append("  Options:\n")
                    append("    Unbreakable: true\n")
                }
            }

            writeItemToMMYmlFile(plugin, cleanInternalId, yamlContent)
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "转换物品到MythicMobs YAML失败", e)
            false
        }
    }

    /**
     * 写入MiniMessage YAML文件到item_output文件夹（增量写入）
     */
    private fun writeItemToMMYmlFile(plugin: JavaPlugin, id: String, yamlContent: String) {
        val folder = File(plugin.dataFolder, "item_output")
        if (!folder.exists()) folder.mkdirs()

        val yamlFile = File(folder, "MMItemYml.yml")

        try {
            if (yamlFile.exists()) {
                val existingContent = yamlFile.readText()

                // 检查是否已存在相同ID的内容（避免重复）
                if (existingContent.contains("$id:")) {
                    plugin.logger.warning("检测到重复物品ID，跳过写入: $id")
                    return
                }

                // 增量写入：在文件末尾追加新内容
                val newContent = if (existingContent.trim().isNotEmpty()) {
                    if (existingContent.endsWith("\n")) {
                        "$existingContent$yamlContent"
                    } else {
                        "$existingContent\n$yamlContent"
                    }
                } else {
                    yamlContent
                }

                yamlFile.writeText(newContent)
            } else {
                // 文件不存在，直接写入
                yamlFile.writeText(yamlContent)
            }

            plugin.logger.info("MythicMobs物品YAML数据已保存到: ${yamlFile.absolutePath}")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "写入MythicMobs物品YAML文件失败", e)
            throw e
        }
    }

    /**
     * 将ItemData属性名转换为MythicMobs属性名
     * 只处理1.8版本唯5能够设置的属性：generic.maxHealth, generic.followRange, generic.knockbackResistance,
     * generic.movementSpeed, generic.attackDamage
     */
    private fun getAttributeMythicName(attributeName: String): String {
        return when (attributeName) {
            "generic.maxHealth" -> "Health"
            "generic.followRange" -> "FollowRange"
            "generic.knockbackResistance" -> "KnockbackResistance"
            "generic.movementSpeed" -> "MovementSpeed"
            "generic.attackDamage" -> "Damage"
            else -> attributeName
        }
    }

    /**
     * 检查是否支持的属性（只处理5个指定属性）
     */
    private fun isSupportedAttribute(attributeName: String): Boolean {
        val supportedAttributes = listOf(
            "generic.maxHealth",
            "generic.followRange",
            "generic.knockbackResistance",
            "generic.movementSpeed",
            "generic.attackDamage"
        )
        return supportedAttributes.contains(attributeName)
    }

    /**
     * 根据物品类型获取默认槽位
     */
    private fun getDefaultSlot(itemType: String): String {
        return when {
            itemType.endsWith("_HELMET", ignoreCase = true) -> "head"
            itemType.endsWith("_CHESTPLATE", ignoreCase = true) -> "chest"
            itemType.endsWith("_LEGGINGS", ignoreCase = true) -> "legs"
            itemType.endsWith("_BOOTS", ignoreCase = true) -> "feet"
            itemType.contains("SWORD", ignoreCase = true) ||
                    itemType.contains("AXE", ignoreCase = true) ||
                    itemType.contains("PICKAXE", ignoreCase = true) -> "mainhand"

            else -> "mainhand" // 默认槽位
        }
    }

    /**
     * 将附魔中文名转换为MythicMobs支持的英文名
     */
    private fun getEnchantmentMythicName(chineseName: String): String {
        return when (chineseName) {
            "保护" -> "PROTECTION"
            "火焰保护" -> "FIRE_PROTECTION"
            "摔落保护" -> "FEATHER_FALLING"
            "爆炸保护" -> "BLAST_PROTECTION"
            "弹射物保护" -> "PROJECTILE_PROTECTION"
            "水下呼吸" -> "RESPIRATION"
            "水下速掘" -> "AQUA_AFFINITY"
            "荆棘" -> "THORNS"
            "深海探索者" -> "DEPTH_STRIDER"
            "锋利" -> "SHARPNESS"
            "亡灵杀手" -> "SMITE"
            "节肢杀手" -> "BANE_OF_ARTHROPODS"
            "击退" -> "KNOCKBACK"
            "火焰附加" -> "FIRE_ASPECT"
            "抢夺" -> "LOOTING"
            "效率" -> "EFFICIENCY"
            "精准采集" -> "SILK_TOUCH"
            "耐久" -> "UNBREAKING"
            "时运" -> "FORTUNE"
            "力量" -> "POWER"
            "冲击" -> "PUNCH"
            "火矢" -> "FLAME"
            "无限" -> "INFINITY"
            "海之眷顾" -> "LUCK_OF_THE_SEA"
            "饵钓" -> "LURE"
            else -> chineseName.uppercase() // 默认转换为大写
        }
    }

    /**
     * 将药水效果中文名转换为MythicMobs支持的英文名
     */
    private fun getPotionEffectMythicName(chineseName: String): String {
        return when (chineseName) {
            "速度" -> "SPEED"
            "缓慢" -> "SLOWNESS"
            "急迫" -> "HASTE"
            "挖掘疲劳" -> "MINING_FATIGUE"
            "力量" -> "STRENGTH"
            "瞬间治疗" -> "INSTANT_HEALTH"
            "瞬间伤害" -> "INSTANT_DAMAGE"
            "跳跃提升" -> "JUMP_BOOST"
            "反胃" -> "NAUSEA"
            "生命恢复" -> "REGENERATION"
            "抗性提升" -> "RESISTANCE"
            "防火" -> "FIRE_RESISTANCE"
            "水下呼吸" -> "WATER_BREATHING"
            "隐身" -> "INVISIBILITY"
            "失明" -> "BLINDNESS"
            "夜视" -> "NIGHT_VISION"
            "饥饿" -> "HUNGER"
            "虚弱" -> "WEAKNESS"
            "中毒" -> "POISON"
            "凋零" -> "WITHER"
            "生命提升" -> "HEALTH_BOOST"
            "伤害吸收" -> "ABSORPTION"
            "饱和" -> "SATURATION"
            else -> chineseName.uppercase()
        }
    }


    /**
     * 物品数据转换的验证函数
     */
    fun validateItemConversion(itemData: ItemData): Boolean {
        return itemData.internalId.isNotEmpty() &&
                itemData.itemType.isNotEmpty() &&
                itemData.internalId.replace("_", "").isNotEmpty()
    }

    /**
     * SpawnerData的扩展函数，将刷怪笼数据转换为MythicMobs格式的YAML配置
     * 修复了YAML格式错误和装备栏序号准确性问题
     */
    fun SpawnerData.transToMMYml(plugin: JavaPlugin): Boolean {
        return try {
            plugin.logger.info("开始转换刷怪笼到MythicMobs YAML格式: ${this.internalId}")

            // 验证必要数据
            if (this.internalId.isNullOrEmpty()) {
                plugin.logger.warning("刷怪笼内部ID为空，跳过写入")
                return false
            }
            // 验证必要数据
            if (this.entityId=="FALLINGSAND") {
                plugin.logger.warning("检测到落沙，跳过写入")
                return false
            }
            if (this.entityId=="ENDERCRYSTAL") {
                plugin.logger.warning("检测到末影水晶，跳过写入")
                return false
            }

            if (this.entityId.isNullOrEmpty()) {
                plugin.logger.warning("实体类型为空，跳过写入")
                return false
            }

            // 清理内部ID（移除特殊字符）
            val cleanInternalId = this.internalId!!.replace("[^a-zA-Z0-9_]".toRegex(), "")

            // 转换实体配置
            val mobsYamlContent = convertToMobsYml(cleanInternalId, plugin)

            // 转换刷怪点配置
            val spawnersYamlContent = convertToSpawnersYml(cleanInternalId)

            // 写入实体配置文件
            writeToMobsYmlFile(plugin, cleanInternalId, mobsYamlContent)

            // 写入刷怪点配置文件
            writeToSpawnersYmlFile(plugin, cleanInternalId, spawnersYamlContent)

            plugin.logger.info("刷怪笼数据已成功转换为MythicMobs YAML格式: $cleanInternalId")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "转换刷怪笼到MythicMobs YAML失败", e)
            false
        }
    }

    /**
     * 将刷怪笼数据转换为MythicMobs实体配置
     * 修复：YAML格式错误和装备序号问题
     */
    private fun SpawnerData.convertToMobsYml(internalId: String, plugin: JavaPlugin): String {
        return buildString {
            append("$internalId:\n")
            append("  Type: ${this@convertToMobsYml.entityId}\n")

            // 处理显示名称
            if (!this@convertToMobsYml.customName.isNullOrEmpty()) {
                val mmDisplay = this@convertToMobsYml.customName!!.colorToStrictMiniMessage()
                append("  Display: \"${escapeYamlString(mmDisplay)}\"\n")
            }

            // 处理属性映射
            val health = getAttributeValue("generic.maxHealth")
            val damage = getAttributeValue("generic.attackDamage")

            append("  Health: $health\n")
            append("  Damage: $damage\n")
            append("  Armor: 0\n")

            // 修复1：正确处理装备序号 - 保持原始顺序，只过滤真正无效的装备
            val validEquipment = this@convertToMobsYml.equipment
                .mapIndexed { index, equipment -> IndexedEquipment(index, equipment) }
                .filter { isValidEquipment(it.equipment) }

            // 修复2：分别处理装备和掉落，但保持原始序号
            val equipmentItems = validEquipment.filter { it.equipment.dropChance == 0f }
            val dropItems = validEquipment.filter { it.equipment.dropChance > 0f }

            // 记录被过滤的无效装备数量
            val totalEquipment = this@convertToMobsYml.equipment.size
            val validEquipmentCount = validEquipment.size
            val filteredCount = totalEquipment - validEquipmentCount

            if (filteredCount > 0) {
                plugin.logger.info("过滤掉 $filteredCount 个无效装备条目（ID为null或空），保留 $validEquipmentCount 个有效装备")
            }

            // 处理其他属性到Options
            val otherAttributes = this@convertToMobsYml.attributes.filter {
                it.name != "generic.maxHealth" && it.name != "generic.attackDamage"
            }

            val hasEquipmentOrDrops = equipmentItems.isNotEmpty() || dropItems.isNotEmpty()
            val hasOtherOptions = otherAttributes.isNotEmpty() || hasEquipmentOrDrops

            // 修复3：正确组织Options部分
            if (hasOtherOptions) {
                append("  Options:\n")

                // 写入其他属性
                otherAttributes.forEach { attr ->
                    when (attr.name) {
                        "generic.movementSpeed" -> append("    MovementSpeed: ${attr.base}\n")
                        "generic.followRange" -> append("    FollowRange: ${attr.base.toInt()}\n")
                        "generic.knockbackResistance" -> append("    KnockbackResistance: ${attr.base}\n")
                    }
                }

                // 修复4：将PreventOtherDrops和PreventRandomEquipment放在Options中，而不是Drops中
                if (hasEquipmentOrDrops) {
                    append("  PreventOtherDrops: true\n")
                    append("  PreventRandomEquipment: true\n")
                }
            }

            // 装备配置 - 使用原始序号对应的正确槽位
            if (equipmentItems.isNotEmpty()) {
                append("  Equipment:\n")
                equipmentItems.forEach { indexedEquip ->
                    val slot = getEquipmentSlotByIndex(indexedEquip.originalIndex)
                    // 确保装备ID有效
                    if (isValidEquipmentId(indexedEquip.equipment.itemId)) {
                        append("    - ${indexedEquip.equipment.itemId}:$slot\n")
                    }
                }
            }

            // 修复5：正确格式化Drops部分
            if (dropItems.isNotEmpty()) {
                append("  Drops:\n")
                dropItems.forEach { indexedDrop ->
                    if (isValidEquipmentId(indexedDrop.equipment.itemId)) {
                        val dropChanceFormatted = "%.2f".format(indexedDrop.equipment.dropChance)
                        // 修复：确保每个掉落项正确缩进
                        append("    - ${indexedDrop.equipment.itemId} ${indexedDrop.equipment.count} $dropChanceFormatted\n")
                    }
                }
            }

            // 修复6：如果没有装备和掉落，但仍然需要Options中的防止掉落设置
            if (!hasOtherOptions && hasEquipmentOrDrops) {
                append("  Options:\n")
                append("    PreventOtherDrops: true\n")
                append("    PreventRandomEquipment: true\n")
            }
        }
    }

    /**
     * 数据类：用于保存装备及其原始序号
     */
    private data class IndexedEquipment(
        val originalIndex: Int, // 原始序号
        val equipment: SpawnerEquipment
    )


    /**
     * 获取属性值，如果不存在则返回原版默认值
     */
    private fun SpawnerData.getAttributeValue(attributeName: String): Double {
        return this.attributes.find { it.name == attributeName }?.base ?: when (attributeName) {
            "generic.maxHealth" -> 20.0
            "generic.attackDamage" -> 1.0
            "generic.movementSpeed" -> 0.2
            "generic.followRange" -> 16.0
            "generic.knockbackResistance" -> 0.0
            else -> 0.0
        }
    }

    /**
     * 根据原始序号获取正确的装备槽位[7](@ref)
     * MythicMobs标准槽位映射：0=HAND, 1=FEET, 2=LEGS, 3=CHEST, 4=HEAD
     */
    private fun getEquipmentSlotByIndex(originalIndex: Int): String {
        return when (originalIndex) {
//            0 -> "HAND"    // 主手
//            1 -> "FEET"    // 脚部
//            2 -> "LEGS"    // 腿部
//            3 -> "CHEST"   // 胸部
//            4 -> "HEAD"    // 头部
            0 -> "0"    // 主手
            1 -> "1"    // 脚部
            2 -> "2"    // 腿部
            3 -> "3"   // 胸部
            4 -> "4"    // 头部
            else -> {
                // 对于超出标准范围的序号，使用模运算映射到有效范围
                val mappedIndex = originalIndex % 5
                getEquipmentSlotByIndex(mappedIndex)
            }
        }
    }

    /**
     * 检查装备是否有效（增强验证）
     * 过滤掉itemId为null、空字符串或"null"的装备，但保留序号信息
     */
    private fun SpawnerData.isValidEquipment(equipment: SpawnerEquipment): Boolean {
        return isValidEquipmentId(equipment.itemId)
    }

    /**
     * 检查装备ID是否有效（增强验证）
     */
    private fun isValidEquipmentId(itemId: String?): Boolean {
        if (itemId.isNullOrEmpty()) return false

        val cleanedId = itemId.trim()
        return cleanedId.isNotEmpty() &&
                cleanedId.lowercase() != "null" &&
                cleanedId != "minecraft:air" &&
                !cleanedId.contains("empty")
    }


    /**
     * 将刷怪笼数据转换为MythicMobs刷怪点配置
     */
    private fun SpawnerData.convertToSpawnersYml(internalId: String): String {
        return buildString {
            append("$internalId:\n")
            append("  MobName: $internalId\n")
            append("  World: world\n")

            // 处理坐标
            val pos = this@convertToSpawnersYml.pos ?: listOf(0.0, 0.0, 0.0)
            append("  X: ${pos.getOrElse(0) { 0.0 }.toInt()}\n")
            append("  Y: ${pos.getOrElse(1) { 0.0 }.toInt()}\n")
            append("  Z: ${pos.getOrElse(2) { 0.0 }.toInt()}\n")

            // 处理生成范围
            append("  Radius: ${this@convertToSpawnersYml.spawnRange*3}\n")
            append("  RadiusY: 10\n")
            append("  LeashRange: ${this@convertToSpawnersYml.spawnRange*6}\n")
            append("  ActivationRange: ${this@convertToSpawnersYml.spawnRange*8}\n")

            // 处理生成数量
            append("  MobsPerSpawn: ${this@convertToSpawnersYml.spawnCount}\n")
            //append("  ActiveMobs: ${this@convertToSpawnersYml.maxNearbyEntities}\n")
            append("  MaxMobs: ${this@convertToSpawnersYml.maxNearbyEntities}\n")

            // 处理冷却时间（tick转换为秒）
            val coolDown = this@convertToSpawnersYml.minSpawnDelay / 20.0
            append("  Cooldown: $coolDown\n")

            // 其他刷怪点设置
            append("  UseTimer: true\n")
            append("  CheckForPlayers: true\n")
            append("  ShowFlames: false\n")
            append("  Breakable: false\n")
        }
    }

    /**
     * 写入MiniMessage实体YAML文件（增量写入，不破坏原有数据）
     */
    private fun writeToMobsYmlFile(plugin: JavaPlugin, id: String, yamlContent: String) {
        val folder = File(plugin.dataFolder, "spawner_output")
        if (!folder.exists()) folder.mkdirs()

        val yamlFile = File(folder, "MMMobsYml.yml")
        writeYamlContent(plugin, yamlFile, id, yamlContent, "实体")
    }

    /**
     * 写入MiniMessage刷怪点YAML文件（增量写入，不破坏原有数据）
     */
    //核心修复点修改机制，以文件个体作为生成
    private fun writeToSpawnersYmlFile(plugin: JavaPlugin, id: String, yamlContent: String) {
        val folder = File(plugin.dataFolder, "spawner_output")
        if (!folder.exists()) folder.mkdirs()
        if (checkSpawnerFileExistsWithSafety(plugin,id)) {
            plugin.logger.warning("检测到重复刷怪点ID，跳过写入: $id")
            return
        }

        //val yamlFile = File(folder, "MMSpawnersYml.yml")
        val yamlFile = File(folder, "${id}.yml")
        writeYamlContent(plugin, yamlFile, id, yamlContent, "刷怪点")
    }
    private fun checkSpawnerFileExistsWithSafety(plugin: JavaPlugin,id:String): Boolean {
        val folder = File(plugin.dataFolder, "spawner_output")
        if (!folder.exists()) {
            folder.mkdirs() // 创建目录（如果不存在）[2](@ref)
        }
        val targetFile = File(folder, "${id}.mml")
        return targetFile.exists()
    }

    /**
     * 通用的YAML文件写入逻辑
     */
    private fun writeYamlContent(plugin: JavaPlugin, yamlFile: File, id: String, yamlContent: String, type: String) {
        try {
            if (yamlFile.exists()) {
                val existingContent = yamlFile.readText()

                // 检查是否已存在相同ID的内容（使用正则表达式匹配顶级键）
                val pattern = Regex("^$id:\\s*\$", RegexOption.MULTILINE)
                if (pattern.containsMatchIn(existingContent)) {
                    plugin.logger.warning("检测到重复${type}ID，跳过写入: $id")
                    return
                }

                // 增量写入：在文件末尾追加新内容
                val newContent = if (existingContent.trim().isNotEmpty()) {
                    if (existingContent.endsWith("\n")) {
                        "$existingContent$yamlContent"
                    } else {
                        "$existingContent\n$yamlContent"
                    }
                } else {
                    yamlContent
                }

                yamlFile.writeText(newContent)
            } else {
                // 文件不存在，直接写入
                yamlFile.writeText(yamlContent)
            }

            plugin.logger.info("MythicMobs${type}YAML数据已保存到: ${yamlFile.absolutePath}")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "写入MythicMobs${type}YAML文件失败", e)
            throw e
        }
    }

    /**
     * VillagerTradeData的扩展函数，将村民交易数据转换为Citizens格式的YAML配置
     * 严格遵循Citizens NPC配置规范，符合MiniMessage格式要求
     */
    fun VillagerTradeData.transToCitizensYml(plugin: JavaPlugin): Boolean {
        return try {
            plugin.logger.info("开始转换村民交易数据到Citizens YAML格式: ${this.villagerKey}")

            // 验证必要数据
            if (this.villagerKey.isBlank()) {
                plugin.logger.warning("村民键为空，跳过写入")
                return false
            }

            // 转换显示名称为MiniMessage格式
            val mmName = this.customName?.colorToStrictMiniMessage() ?: "Unnamed"
            // villagerKey直接使用，不需要MiniMessage转换（作为命令参数）

            // 处理坐标：x和z坐标小数部分统一为.5，y坐标保持原样
            val loc = this.location
            val world = loc.world
            if (world == null) {
                plugin.logger.warning("位置世界为空，跳过写入")
                return false
            }

            val x = Math.floor(loc.x) + 0.5  // x坐标处理为整数部分 + 0.5
            val y = loc.y  // y坐标保持原样
            val z = Math.floor(loc.z) + 0.5  // z坐标处理为整数部分 + 0.5

            // 构建YAML内容
            val yamlContent = buildString {
                append("- name: \"${escapeYamlString(mmName)}\"\n")
                append("  traits:\n")
                append("    location:\n")
                append("      worldid: ${world.uid}\n")
                append("      x: $x\n")
                append("      y: $y\n")
                append("      z: $z\n")
                append("    type: VILLAGER\n")
                append("    lookclose:\n")
                append("      enabled: true\n")
                append("      randomPitchRange:\n")
                append("      - 0.0\n")
                append("      - 0.0\n")
                append("      randomYawRange:\n")
                append("      - 0.0\n")
                append("      - 360.0\n")
                append("    scoreboardtrait:\n")
                append("      tags:\n")
                append("      - CITIZENS_NPC\n")
                append("    hologramtrait: {}\n")
                append("    villagertrait: {}\n")
                append("    profession: ${this@transToCitizensYml.profession}\n")
                append("    commandtrait:\n")
                append("      commands:\n")
                append("      - command: \"plctrade opentrade <p> ${this@transToCitizensYml.villagerKey}\"\n")
                append("        hand: RIGHT\n")
                append("        player: false\n")
                append("        npc: false\n")
                append("        op: false\n")
                append("        cooldown: 0\n")
                append("        globalcooldown: 0\n")
                append("        n: -1\n")
                append("        gn: -1\n")
                append("        delay: 0\n")
                append("        cost: -1.0\n")
                append("        experienceCost: -1\n")
                append("  traitnames: scoreboardtrait,spawned,owner,profession,rotationtrait,commandtrait,lookclose,type,targetable,hologramtrait,location,villagertrait,chunktickettrait\n")
                append("  navigator:\n")
                append("    speedmodifier: 1.0\n")
                append("    avoidwater: false\n")
                append("    usedefaultstuckaction: false\n")
            }

            // 写入Citizens YAML文件
            writeToCitizensYmlFile(plugin, this.villagerKey, yamlContent)
            plugin.logger.info("村民交易数据已成功转换为Citizens YAML格式: ${this.villagerKey}")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "转换村民交易数据到Citizens YAML失败", e)
            false
        }
    }

    /**
     * 写入Citizens YAML文件到trade_output文件夹（增量写入）
     */
    private fun writeToCitizensYmlFile(plugin: JavaPlugin, id: String, yamlContent: String) {
        val folder = File(plugin.dataFolder, "trade_output")
        if (!folder.exists()) folder.mkdirs()

        val yamlFile = File(folder, "saves.yml")

        try {
            if (yamlFile.exists()) {
                val existingContent = yamlFile.readText()

                // 检查是否已存在相同村民键的内容（避免重复）
                if (existingContent.contains("$id")) {
                    plugin.logger.warning("检测到重复村民键，跳过写入: $id")
                    return
                }

                // 增量写入：在文件末尾追加新内容
                val newContent = if (existingContent.trim().isNotEmpty()) {
                    if (existingContent.endsWith("\n")) {
                        "$existingContent$yamlContent"
                    } else {
                        "$existingContent\n$yamlContent"
                    }
                } else {
                    yamlContent
                }

                yamlFile.writeText(newContent)
            } else {
                // 文件不存在，直接写入
                yamlFile.writeText(yamlContent)
            }

            plugin.logger.info("Citizens YAML数据已保存到: ${yamlFile.absolutePath}")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "写入Citizens YAML文件失败", e)
            throw e
        }
    }

    fun ItemStack.getItsTypeInHigherVersion(): String {
        val dyeMappings = mapOf(
            0 to "WHITE_DYE",      // WHITE(0, 15, ...)
            1 to "ORANGE_DYE",    // ORANGE(1, 14, ...)
            2 to "MAGENTA_DYE",   // MAGENTA(2, 13, ...)
            3 to "LIGHT_BLUE_DYE", // LIGHT_BLUE(3, 12, ...)
            4 to "YELLOW_DYE",    // YELLOW(4, 11, ...)
            5 to "LIME_DYE",      // LIME(5, 10, ...)
            6 to "PINK_DYE",     // PINK(6, 9, ...)
            7 to "GRAY_DYE",     // GRAY(7, 8, ...)
            8 to "LIGHT_GRAY_DYE", // SILVER(8, 7, ...) -> 高版本为LIGHT_GRAY_DYE
            9 to "CYAN_DYE",     // CYAN(9, 6, ...)
            10 to "PURPLE_DYE",  // PURPLE(10, 5, ...)
            11 to "LAPIS_LAZULI",    // BLUE(11, 4, ...),高版本为特么的青晶石
            12 to "BROWN_DYE",   // BROWN(12, 3, ...)
            13 to "GREEN_DYE",   // GREEN(13, 2, ...)
            14 to "RED_DYE",     // RED(14, 1, ...)
            15 to "BLACK_DYE"    // BLACK(15, 0, ...
        )
        return when (this.type.name){
            "INK_SACK" ->{
                val dataValue =this.durability
                dyeMappings[(15)-dataValue.toInt()] ?: "INK_SACK"
            }
            "SIGN" -> "OAK_SIGN"
            "MELON" -> "MELON_SLICE"
            "REEDS" -> "SUGAR_CANE"
            "SPECKLED_MELON" -> "GLISTERING_MELON_SLICE"
            "COMMAND" -> "COMMAND_BLOCK"
            "IRON_BARDING" -> "IRON_HORSE_ARMOR"
            "GOLD_BARDING" -> "GOLDEN_HORSE_ARMOR"
            "DIAMOND_BARDING" -> "DIAMOND_HORSE_ARMOR"
            else -> this.type.name
        }
    }
    fun Int.getItsTypeInHigherVersion(): String {
        val dyeMappings = mapOf(
            0 to "WHITE_DYE",      // WHITE(0, 15, ...)
            1 to "ORANGE_DYE",    // ORANGE(1, 14, ...)
            2 to "MAGENTA_DYE",   // MAGENTA(2, 13, ...)
            3 to "LIGHT_BLUE_DYE", // LIGHT_BLUE(3, 12, ...)
            4 to "YELLOW_DYE",    // YELLOW(4, 11, ...)
            5 to "LIME_DYE",      // LIME(5, 10, ...)
            6 to "PINK_DYE",     // PINK(6, 9, ...)
            7 to "GRAY_DYE",     // GRAY(7, 8, ...)
            8 to "LIGHT_GRAY_DYE", // SILVER(8, 7, ...) -> 高版本为LIGHT_GRAY_DYE
            9 to "CYAN_DYE",     // CYAN(9, 6, ...)
            10 to "PURPLE_DYE",  // PURPLE(10, 5, ...)
            11 to "LAPIS_LAZULI",    // BLUE(11, 4, ...)
            12 to "BROWN_DYE",   // BROWN(12, 3, ...)
            13 to "GREEN_DYE",   // GREEN(13, 2, ...)
            14 to "RED_DYE",     // RED(14, 1, ...)
            15 to "BLACK_DYE"    // BLACK(15, 0, ...
        )
        return dyeMappings[(15)-(this.toInt())] ?: "INK_SACK"

    }
    fun String.getItsTypeInHigherVersionAnotherOne(): String {
        return when (this){
            "SIGN" -> "OAK_SIGN"
            "MELON" -> "MELON_SLICE"
            "REEDS" -> "SUGAR_CANE"
            "SPECKLED_MELON" -> "GLISTERING_MELON_SLICE"
            "COMMAND" -> "COMMAND_BLOCK"
            "IRON_BARDING" -> "IRON_HORSE_ARMOR"
            "GOLD_BARDING" -> "GOLDEN_HORSE_ARMOR"
            "DIAMOND_BARDING" -> "DIAMOND_HORSE_ARMOR"
            else -> this


    }
    }
    /**
     * 从物品Lore中提取并组合拼音标识
     */
    fun ItemStack.getFirstTwoZWCharsInTheFirstLineOfLoreAndTheLastTwoZWCharsInTheLastLineOfLore(): String {
        val itemMeta = this.itemMeta ?: return ""
        val lore = itemMeta.lore ?: return ""

        if (lore.isEmpty()) return ""

        // 获取第一行和最后一行
        val firstLine = lore.first()
        val lastLine = lore.last()

        // 从第一行提取前两个汉字
        val firstTwoChars = extractChineseCharacters(firstLine).take(2)
        // 从最后一行提取最后两个汉字
        val lastTwoChars = extractChineseCharacters(lastLine).takeLast(2)

        if (firstTwoChars.isEmpty() || lastTwoChars.isEmpty()) {
            return ""
        }


        return firstTwoChars + lastTwoChars
    }
    /**
     * 从物品Lore中提取并组合拼音标识
     */
    fun List<String>.getFirstTwoZWCharsInTheFirstLineOfLoreAndTheLastTwoZWCharsInTheLastLineOfLore(): String {
        // 获取第一行和最后一行
        val firstLine = this.first()
        val lastLine = this.last()

        // 从第一行提取前两个汉字
        val firstTwoChars = extractChineseCharacters(firstLine).take(2)
        // 从最后一行提取最后两个汉字
        val lastTwoChars = extractChineseCharacters(lastLine).takeLast(2)

        if (firstTwoChars.isEmpty() || lastTwoChars.isEmpty()) {
            return ""
        }


        return firstTwoChars + lastTwoChars
    }

    /**
     * 提取字符串中的汉字字符
     */
    private fun extractChineseCharacters(text: String): String {
        return text.filter { char ->
            // 汉字Unicode范围：\u4E00-\u9FA5
            char in '\u4E00'..'\u9FA5'
        }
    }
}
