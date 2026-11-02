package com.lcz.passwordmanager.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.KeyGenerator
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
    
    companion object {
        private const val BIOMETRIC_AUTH_BINDING_KEY = "biometric_auth_binding_key"
    }
    
    private val biometricManager = BiometricManager.from(context)
    
    /**
     * 设备凭证自上次生物识别后是否未被修改。
     * lazy 求值：Keystore 可能抛异常，放主线程初始化会导致启动崩溃。
     */
    private val deviceCredentialUnchanged: Boolean by lazy { checkDeviceCredentialUnchanged() }
    
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
        
        // 密钥未设置生物识别绑定，仅校验生物特征可通过“新录入指纹”绕过，
        // 因此必须再确认设备凭证（PIN/图案/密码）未被其他人修改
        if (!deviceCredentialUnchanged) {
            continuation.resume(AuthResult.Error("设备的锁屏密码已变更，请使用主密码解锁"))
            return@suspendCancellableCoroutine
        }
        
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
        
        prompt.authenticate(buildPromptInfo(title, subtitle, description))
        
        continuation.invokeOnCancellation {
            prompt.cancelAuthentication()
        }
    }
    
    /**
     * 构建认证弹窗配置。
     * 
     * androidx.biometric 1.1.0 对认证器组合有硬性限制：
     * - API 30+ 可单独使用 [BiometricManager.Authenticators.BIOMETRIC_STRONG]，
     *   且该组合与 setNegativeButtonText 互斥；
     * - API 24-29 设置 BIOMETRIC_STRONG 时系统会隐式注入 DEVICE_CREDENTIAL，
     *   此时调用 setNegativeButtonText 会抛 IllegalArgumentException（部分 ROM 上为 NPE）。
     */
    private fun buildPromptInfo(
        title: String,
        subtitle: String,
        description: String
    ): BiometricPrompt.PromptInfo {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
        // 两种版本都只声明生物特征：API 30+ 单独使用 BIOMETRIC_STRONG；
        // API 24-29 由系统隐式补齐设备凭证。负按钮仅在 API 30+ 可用。
        builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setNegativeButtonText("取消")
        }
        return builder.build()
    }
    
    /**
     * 校验设备凭证（锁屏密码）自上次使用生物识别后未被修改。
     * 创建带 setUserAuthenticationRequired 的 Keystore 密钥，锁屏指纹/PIN/图案变化时系统会使其失效，
     * 此后必须改用主密码解锁，并在下次设置主密码时重新绑定。
     */
    private fun checkDeviceCredentialUnchanged(): Boolean {
        val keyStore = loadKeyStore() ?: return true
        if (keyStore.containsAlias(BIOMETRIC_AUTH_BINDING_KEY)) {
            return try {
                keyStore.getEntry(BIOMETRIC_AUTH_BINDING_KEY, null) != null
            } catch (e: UnrecoverableKeyException) {
                false
            }
        }
        return createDeviceCredentialBinding()
    }
    
    private fun createDeviceCredentialBinding(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Keystore 的 setUserAuthenticationRequired 从 API 26 起可用
            return true
        }
        return try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                BIOMETRIC_AUTH_BINDING_KEY,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun loadKeyStore(): KeyStore? {
        return try {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        } catch (e: Exception) {
            null
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