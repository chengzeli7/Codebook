package com.lcz.passwordmanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 小梨密码 - 应用程序入口
 * 
 * 核心特性：
 * 1. 完全离线运行，零网络传输
 * 2. 硬件级加密保护
 * 3. 生物识别认证
 * 
 * @author lcz
 * @since 1.0.0
 */
@HiltAndroidApp
class PasswordManagerApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // 初始化应用级配置
        initializeSecurity()
    }
    
    /**
     * 初始化安全配置
     * 包括KeyStore、加密参数等
     */
    private fun initializeSecurity() {
        // 安全配置在SecurityModule中通过Hilt注入管理
    }
}