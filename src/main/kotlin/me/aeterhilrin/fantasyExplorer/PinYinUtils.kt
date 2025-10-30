package me.aeterhilrin.fantasyExplorer

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination

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
        // 移除其他特殊字符
        cleaned = cleaned.replace("[^a-zA-Z0-9\\u4e00-\\u9fa5]".toRegex(), "")
        return cleaned
    }
    /**
     * String类型扩展函数 - 清理字符串
     * 效果等同于PinyinUtils.cleanString()
     */
    fun String.toBeCleaned(): String {
        // 移除Minecraft颜色代码（§后跟0-9a-fk-or字符）
        var cleaned = this.replace(Regex("§[0-9a-fk-or]"), "")
        // 移除其他特殊字符，只保留字母、数字、中文和空格
        cleaned = cleaned.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]"), "")
        return cleaned.trim()
    }

    /**
     * String类型扩展函数 - 快速转换为拼音
     * 效果等同于PinyinUtils.toPinyin()，但使用更简洁的调用方式
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
            sb.toString()
        } catch (e: BadHanyuPinyinOutputFormatCombination) {
            this // 转换失败时返回原字符串
        }
    }

    /**
     * 扩展函数 - 清理并转换为拼音（链式操作）
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
}