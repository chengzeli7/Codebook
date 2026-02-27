package com.lcz.passwordmanager.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 主密码管理器 - 管理用户主密码的设置和验证
 * 
 * 安全特性：
  * 1. 主密码使用SHA-256哈希存储
 * 2. 使用EncryptedSharedPreferences加密存储
 * 3. 支持生物识别绑定
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
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * 检查是否是首次启动
     * @return 是否首次启动
     */
    fun isFirstLaunch(): Boolean {
        return encryptedPrefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    /**
     * 检查是否已设置主密码
     * @return 是否已设置
     */
    fun isMasterPasswordSet(): Boolean {
        return encryptedPrefs.getString(KEY_PASSWORD_HASH, null) != null
    }
    
    /**
     * 设置主密码
     * 
     * @param password 主密码（6位以上数字或字母组合）
     * @return 是否设置成功
     */
    fun setMasterPassword(password: String): Boolean {
        if (!isPasswordValid(password)) {
            return false
        }
        
        val hashedPassword = hashPassword(password)
        encryptedPrefs.edit()
            .putString(KEY_PASSWORD_HASH, hashedPassword)
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
        
        return true
    }
    
    /**
     * 验证主密码
     * 
     * @param password 输入的密码
     * @return 是否验证通过
     */
    fun verifyMasterPassword(password: String): Boolean {
        val storedHash = encryptedPrefs.getString(KEY_PASSWORD_HASH, null)
            ?: return false
        
        val inputHash = hashPassword(password)
        return storedHash == inputHash
    }
    
    /**
     * 检查是否启用了生物识别
     * @return 是否启用
     */
    fun isBiometricEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    /**
     * 设置生物识别启用状态
     * @param enabled 是否启用
     */
    fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit()
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
    fun isPasswordValid(password: String): Boolean {
        if (password.length < 6) return false
        
        // 检查是否只包含数字和字母
        return password.all { it.isDigit() || it.isLetter() }
    }
    
    /**
     * 使用SHA-256哈希密码
     * 
     * @param password 明文密码
     * @return 哈希值（Base64编码）
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return android.util.Base64.encodeToString(hashBytes, android.util.Base64.DEFAULT)
    }
    
    /**
     * 清除所有数据（用于重置）
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
}