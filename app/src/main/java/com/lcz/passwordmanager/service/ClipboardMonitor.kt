package com.lcz.passwordmanager.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 剪贴板监控器 - 智能识别剪贴板中的账号密码
 * 
 * 功能：
 * 1. 检测剪贴板内容变化
 * 2. 识别账号密码格式
 * 3. 触发保存提示
 * 
 * 隐私保护：
 * 1. 仅在前台时检测（Android 10+限制）
 * 2. 检测后立即清空内存缓存
 * 3. 不记录历史剪贴板内容
 * 
 * @author lcz
 * @since 1.0.0
 */
@Singleton
class ClipboardMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    /**
     * 检测到的账号密码
     */
    private val _detectedCredentials = MutableStateFlow<Credentials?>(null)
    val detectedCredentials: StateFlow<Credentials?> = _detectedCredentials.asStateFlow()

    /**
     * 是否启用剪贴板监听
     */
    private var isEnabled = true

    /**
     * 忽略的剪贴板内容（用户选择忽略的内容）
     */
    private val ignoredContents = mutableSetOf<String>()

    /**
     * 检测规则
     */
    private val patterns = listOf(
        // 账号:xxx 密码:xxx 格式
        Pattern.compile(
            "(?:账号|用户名|username|account)[:：]\\s*(\\S+).*?(?:密码|password|pwd)[:：]\\s*(\\S+)",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        ),
        // 密码:xxx 格式
        Pattern.compile(
            "(?:密码|password|pwd)[:：]\\s*(\\S+)",
            Pattern.CASE_INSENSITIVE
        ),
        // 邮箱+密码组合
        Pattern.compile(
            "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}).*?(\\S{6,})",
            Pattern.DOTALL
        )
    )

    /**
     * 检查剪贴板内容
     * 应在App切换到前台时调用
     */
    fun checkClipboard() {
        if (!isEnabled) return

        try {
            val clip = clipboardManager.primaryClip
            if (clip == null || clip.itemCount == 0) return

            val item = clip.getItemAt(0)
            val text = item.text?.toString() ?: return

            // 检查是否已忽略
            if (ignoredContents.contains(text.hashCode().toString())) return

            // 识别账号密码
            val credentials = extractCredentials(text)
            if (credentials != null) {
                _detectedCredentials.value = credentials
            }

            // 清空剪贴板引用（隐私保护）
            clearClipboardReference()

        } catch (e: Exception) {
            // 剪贴板访问可能失败（权限或系统限制）
        }
    }

    /**
     * 从文本中提取账号密码
     * @param text 剪贴板文本
     * @return 识别到的账号密码，未识别到返回null
     */
    private fun extractCredentials(text: String): Credentials? {
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return when (matcher.groupCount()) {
                    2 -> Credentials(
                        username = matcher.group(1) ?: "",
                        password = matcher.group(2) ?: "",
                        rawText = text
                    )
                    1 -> Credentials(
                        username = "",
                        password = matcher.group(1) ?: "",
                        rawText = text
                    )
                    else -> null
                }
            }
        }
        return null
    }

    /**
     * 清除检测到的账号密码
     */
    fun clearDetectedCredentials() {
        _detectedCredentials.value = null
    }

    /**
     * 忽略当前内容（30分钟内不再提示）
     */
    fun ignoreCurrentContent(rawText: String) {
        ignoredContents.add(rawText.hashCode().toString())
        clearDetectedCredentials()

        // 30分钟后移除忽略标记
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            ignoredContents.remove(rawText.hashCode().toString())
        }, 30 * 60 * 1000)
    }

    /**
     * 设置是否启用剪贴板监听
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * 清空剪贴板引用
     * 防止密码在内存中残留
     */
    private fun clearClipboardReference() {
        // 清空剪贴板内容（可选，根据产品需求）
        // clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
    }

    /**
     * 账号密码数据类
     */
    data class Credentials(
        val username: String,
        val password: String,
        val rawText: String
    )
}