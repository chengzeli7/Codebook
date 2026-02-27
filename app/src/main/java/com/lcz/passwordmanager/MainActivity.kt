package com.lcz.passwordmanager

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.lcz.passwordmanager.ui.navigation.PasswordManagerNavHost
import com.lcz.passwordmanager.ui.theme.小梨密码Theme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主Activity - 应用入口界面
 * 
 * 安全特性：
 * 1. 设置FLAG_SECURE防止截屏/录屏
 * 2. 支持生物识别认证流程
 * 3. 管理应用生命周期安全状态
 * 
 * @author lcz
 * @since 1.0.0
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用Edge-to-Edge显示
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 设置防截屏保护（默认开启）
        setupScreenshotProtection()
        
        setContent {
            小梨密码Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PasswordManagerNavHost()
                }
            }
        }
    }
    
    /**
     * 设置防截屏保护
     * 添加FLAG_SECURE标志，防止系统截屏和录屏
     * 符合安全规范NFR-001要求
     */
    private fun setupScreenshotProtection() {
        // 从SharedPreferences读取用户设置，默认开启
        val prefs = getSharedPreferences("security_prefs", MODE_PRIVATE)
        val allowScreenshot = prefs.getBoolean("allow_screenshot", false)
        
        if (!allowScreenshot) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }
}