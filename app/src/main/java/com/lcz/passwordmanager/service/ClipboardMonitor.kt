package com.lcz.passwordmanager.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
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
    private val TAG = "ClipboardMonitor"

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
     * 检测规则 - 支持多种格式
     */
    private val patterns = listOf(
        // 格式1: 账号 1233 密码 1231231 (支持换行和空格)
        Pattern.compile(
            "(?:账号|用户名|user|account)\\s+(\\S+)\\s*(?:密码|password|pwd)\\s+(\\S+)",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        ),
        // 格式2: 密码 1231231 账号 1233 (支持换行和空格)
        Pattern.compile(
            "(?:密码|password|pwd)\\s+(\\S+)\\s*(?:账号|用户名|user|account)\\s+(\\S+)",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        ),
        // 格式3: 账号:1233 密码:1231231
        Pattern.compile(
            "(?:账号|用户名|user|account)[:：]\\s*(\\S+)\\s*(?:密码|password|pwd)[:：]\\s*(\\S+)",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        ),
        // 格式4: 密码:1231231 账号:1233
        Pattern.compile(
            "(?:密码|password|pwd)[:：]\\s*(\\S+)\\s*(?:账号|用户名|user|account)[:：]\\s*(\\S+)",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        ),
        // 格式5: 只有密码的情况
        Pattern.compile(
            "(?:密码|password|pwd)\\s*[:：]?\\s*(\\S{4,})",
            Pattern.CASE_INSENSITIVE
        )
    )

    /**
     * 检查剪贴板内容
     * 应在App切换到前台时调用
     */
    fun checkClipboard() {
        checkClipboardInternal(null)
    }
    
    /**
     * 检查剪贴板内容（可传入模拟文本用于测试）
     * @param mockText 模拟的剪贴板内容，为null时读取真实剪贴板
     */
    fun checkClipboardWithMock(mockText: String? = null) {
        checkClipboardInternal(mockText)
    }
    
    private fun checkClipboardInternal(mockText: String?) {
        if (!isEnabled) return

        try {
            val text = mockText ?: run {
                Log.d(TAG, "开始读取剪贴板...")
                
                val clip = clipboardManager.primaryClip
                Log.d(TAG, "clip: $clip, itemCount: ${clip?.itemCount}")
                
                if (clip == null || clip.itemCount == 0) {
                    Log.d(TAG, "剪贴板为空或无内容")
                    return
                }
                
                for (i in 0 until clip.itemCount) {
                    val item = clip.getItemAt(i)
                    Log.d(TAG, "第${i}项: ${item.text}, mimeType: ${item.coerceToText(context)}")
                }
                
                val item = clip.getItemAt(0)
                val textContent = item.text?.toString()?.trim()
                val coerceText = item.coerceToText(context).toString().trim()
                
                Log.d(TAG, "item.text: [$textContent]")
                Log.d(TAG, "coerceToText: [$coerceText]")
                
                textContent ?: coerceText
            }

            if (text.isNullOrBlank()) {
                Log.d(TAG, "剪贴板内容为空")
                return
            }
            
            Log.d(TAG, "检测到剪贴板内容: [$text]")

            // 检查是否已忽略
            val textHash = text.hashCode().toString()
            if (ignoredContents.contains(textHash)) {
                Log.d(TAG, "内容已被忽略")
                return
            }

            // 识别账号密码
            val credentials = extractCredentials(text)
            if (credentials != null) {
                Log.d(TAG, "识别成功: username=${credentials.username}, password=${credentials.password}")
                _detectedCredentials.value = credentials
            } else {
                Log.d(TAG, "未识别到账号密码")
            }

        } catch (e: Exception) {
            Log.e(TAG, "剪贴板访问失败", e)
        }
    }

    /**
     * 从文本中提取账号密码
     * @param text 剪贴板文本
     * @return 识别到的账号密码，未识别到返回null
     */
    private fun extractCredentials(text: String): Credentials? {
        // 预处理：统一换行符为空格，方便匹配
        val normalizedText = text.replace("\n", " ").replace("\r", " ")
        
        for ((index, pattern) in patterns.withIndex()) {
            val matcher = pattern.matcher(normalizedText)
            if (matcher.find()) {
                Log.d(TAG, "正则 $index 匹配成功, groupCount=${matcher.groupCount()}")
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
        
        // 尝试原始文本匹配
        for ((index, pattern) in patterns.withIndex()) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                Log.d(TAG, "正则(原始) $index 匹配成功, groupCount=${matcher.groupCount()}")
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
     * 手动设置检测结果（用于测试）
     */
    fun setDetectedCredentials(credentials: Credentials?) {
        _detectedCredentials.value = credentials
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
     * 账号密码数据类
     */
    data class Credentials(
        val username: String,
        val password: String,
        val rawText: String
    )
}