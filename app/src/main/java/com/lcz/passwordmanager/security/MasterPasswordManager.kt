package com.lcz.passwordmanager.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 主密码管理器 - 管理用户主密码的设置和验证
 * 
 * 安全特性：
 * 1. 主密码使用 PBKDF2-HMAC-SHA256（迭代 31 万次 + 随机盐）派生出哈希后存储，
 *    不使用可被彩虹表快速还原的裸 SHA-256
 * 2. 支持生物识别绑定
 * 3. 主密码哈希只保存在应用私有目录，随应用卸载删除
 * 
 * @author lcz
 * @since 1.0.0
 */
@Singleton
class MasterPasswordManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "master_password_prefs"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PASSWORD_SALT = "password_salt"

        // PBKDF2 参数，遵循 OWASP 对 PBKDF2-HMAC-SHA256 的推荐值
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 310_000
        private const val PBKDF2_KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16

        /**
         * 纯格式校验，不读取任何存储，便于在不依赖 Android 的测试中使用
         */
        fun isValidPasswordFormat(password: String): Boolean {
            if (password.length < 6) return false
            // 只允许 ASCII 字母与数字，避免全角/非拉丁字符绕过
            return password.all { it in '0'..'9' || it in 'a'..'z' || it in 'A'..'Z' }
        }
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 检查是否是首次启动
     * @return 是否首次启动
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    /**
     * 检查是否已设置主密码
     * @return 是否已设置
     */
    fun isMasterPasswordSet(): Boolean {
        return prefs.getString(KEY_PASSWORD_HASH, null) != null
    }
    
    /**
     * 设置主密码
     * 
     * PBKDF2 计算量较大，请在后台线程（如协程）中调用。
     * 
     * @param password 主密码（6位以上数字或字母组合）
     * @return 是否设置成功
     */
    fun setMasterPassword(password: String): Boolean {
        if (!isPasswordValid(password)) {
            return false
        }
        
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val hashedPassword = hashPassword(password, salt)
        prefs.edit()
            .putString(KEY_PASSWORD_HASH, hashedPassword)
            .putString(KEY_PASSWORD_SALT, android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP))
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
        
        return true
    }
    
    /**
     * 验证主密码
     * 
     * PBKDF2 计算量较大，请在后台线程（如协程）中调用。
     * 
     * @param password 输入的密码
     * @return 是否验证通过
     */
    fun verifyMasterPassword(password: String): Boolean {
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null)
            ?: return false
        val salt = prefs.getString(KEY_PASSWORD_SALT, null)
            ?: return false
        val saltBytes = android.util.Base64.decode(salt, android.util.Base64.NO_WRAP)
        
        val inputHash = hashPassword(password, saltBytes)
        // 常量时间比较，避免通过响应时间推测哈希
        return MessageDigest.isEqual(
            inputHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }
    
    /**
     * 检查是否启用了生物识别
     * @return 是否启用
     */
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    /**
     * 设置生物识别启用状态
     * @param enabled 是否启用
     */
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }
    
    /**
     * 验证密码格式
     * 要求：6位以上，数字或字母组合
     * 
     * @param password 密码
     * @return 是否有效
     */
    fun isPasswordValid(password: String): Boolean = isValidPasswordFormat(password)
    
    /**
     * 使用 PBKDF2-HMAC-SHA256 从主密码派生出哈希
     * 
     * @param password 明文密码
     * @param salt 随机盐值
     * @return 哈希值（Base64编码）
     */
    private fun hashPassword(password: String, salt: ByteArray): String {
        return android.util.Base64.encodeToString(
            deriveKeyBytes(password, salt),
            android.util.Base64.NO_WRAP
        )
    }

    /**
     * PBKDF2-HMAC-SHA256 派生，返回原始字节（便于单元测试与复用）
     */
    internal fun deriveKeyBytes(password: String, salt: ByteArray): ByteArray {
        val passwordChars = password.toCharArray()
        val spec = PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        try {
            return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                .generateSecret(spec)
                .encoded
        } finally {
            // 及时清理内存中的明文密码
            spec.clearPassword()
            passwordChars.fill('\u0000')
        }
    }
    
    /**
     * 清除所有数据（用于重置）
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}