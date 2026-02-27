package com.lcz.passwordmanager.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 生物识别管理器 - 处理指纹/面部识别认证
 * 
 * 功能：
 * 1. 检测设备生物识别能力
 * 2. 触发生物识别认证流程
 * 3. 处理认证结果回调
 * 
 * @author lcz
 * @since 1.0.0
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    private val context: Context
) {
    
    private val biometricManager = BiometricManager.from(context)
    
    /**
     * 检查生物识别可用性
     * @return 生物识别状态
     */
    fun checkBiometricAvailability(): BiometricStatus {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.UNKNOWN
        }
    }
    
    /**
     * 检查是否支持设备凭证（PIN/密码/图案）
     * @return 是否支持
     */
    fun isDeviceCredentialAvailable(): Boolean {
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    /**
     * 启动生物识别认证（挂起函数）
     * 
     * @param activity FragmentActivity
     * @param title 标题
     * @param subtitle 副标题
     * @param description 描述
     * @return 认证结果
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "验证身份",
        subtitle: String = "使用指纹或面部识别解锁",
        description: String = "请验证您的身份以访问密码管理器"
    ): AuthResult = suspendCancellableCoroutine { continuation ->
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                continuation.resume(AuthResult.Success)
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        continuation.resume(AuthResult.Cancelled)
                    }
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        continuation.resume(AuthResult.Locked)
                    }
                    else -> {
                        continuation.resume(AuthResult.Error(errString.toString()))
                    }
                }
            }
            
            override fun onAuthenticationFailed() {
                // 认证失败但可重试，不恢复协程
            }
        }
        
        val prompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        
        prompt.authenticate(promptInfo)
        
        continuation.invokeOnCancellation {
            prompt.cancelAuthentication()
        }
    }
    
    /**
     * 生物识别状态枚举
     */
    enum class BiometricStatus {
        AVAILABLE,      // 可用
        NO_HARDWARE,    // 无硬件
        HW_UNAVAILABLE, // 硬件不可用
        NOT_ENROLLED,   // 未录入生物特征
        UNKNOWN         // 未知状态
    }
    
    /**
     * 认证结果密封类
     */
    sealed class AuthResult {
        object Success : AuthResult()
        object Cancelled : AuthResult()
        object Locked : AuthResult()
        data class Error(val message: String) : AuthResult()
    }
}