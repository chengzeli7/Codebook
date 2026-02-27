package com.lcz.passwordmanager.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库密钥管理器
 * 
 * 职责：
 * 1. 生成和管理SQLCipher数据库加密密钥
 * 2. 使用EncryptedSharedPreferences安全存储密钥
 * 3. 支持密钥轮换和恢复
 * 
 * 安全架构：
 * - 数据库密钥：随机生成的256位密钥，用于SQLCipher
 * - 主密钥：存储于Keystore，用于加密SharedPreferences
 * - 密钥层次：Keystore主密钥 -> EncryptedSharedPreferences -> 数据库密钥
 * 
 * @author lcz
 * @since 1.0.0
 */
@Singleton
class DatabaseKeyManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val PREFS_FILE = "secure_prefs"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
        private const val KEY_MASTER_PASSWORD_SALT = "master_password_salt"
        
        // 密钥长度（字节）
        private const val PASSPHRASE_LENGTH = 32
    }
    
    /**
     * 加密的SharedPreferences实例
     * 使用AES-256加密，密钥存储于Keystore
     */
    private val securePrefs: SharedPreferences by lazy {
        createEncryptedSharedPreferences()
    }
    
    /**
     * 创建加密的SharedPreferences
     */
    private fun createEncryptedSharedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * 获取或创建数据库密钥
     * 
     * @return SQLCipher数据库密钥（字符串形式）
     */
    fun getOrCreateDatabaseKey(): String {
        val existingKey = securePrefs.getString(KEY_DB_PASSPHRASE, null)
        
        return if (existingKey != null) {
            existingKey
        } else {
            // 生成新的随机密钥
            val newKey = generateSecureRandomKey()
            securePrefs.edit()
                .putString(KEY_DB_PASSPHRASE, newKey)
                .apply()
            newKey
        }
    }
    
    /**
     * 获取数据库密钥（如果不存在返回null）
     */
    fun getDatabaseKey(): String? {
        return securePrefs.getString(KEY_DB_PASSPHRASE, null)
    }
    
    /**
     * 重新生成数据库密钥
     * 警告：这将使现有数据库无法访问，仅在首次设置或重置时使用
     */
    fun regenerateDatabaseKey(): String {
        val newKey = generateSecureRandomKey()
        securePrefs.edit()
            .putString(KEY_DB_PASSPHRASE, newKey)
            .apply()
        return newKey
    }
    
    /**
     * 生成安全的随机密钥
     * 
     * @return Base64编码的随机密钥
     */
    private fun generateSecureRandomKey(): String {
        val random = SecureRandom()
        val keyBytes = ByteArray(PASSPHRASE_LENGTH)
        random.nextBytes(keyBytes)
        return android.util.Base64.encodeToString(keyBytes, android.util.Base64.NO_WRAP)
    }
    
    /**
     * 保存主密码盐值
     * 用于主密码的PBKDF2密钥派生
     * 
     * @param salt 随机盐值
     */
    fun saveMasterPasswordSalt(salt: ByteArray) {
        securePrefs.edit()
            .putString(KEY_MASTER_PASSWORD_SALT, android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP))
            .apply()
    }
    
    /**
     * 获取主密码盐值
     * @return 盐值，不存在返回null
     */
    fun getMasterPasswordSalt(): ByteArray? {
        val saltString = securePrefs.getString(KEY_MASTER_PASSWORD_SALT, null)
        return saltString?.let {
            android.util.Base64.decode(it, android.util.Base64.NO_WRAP)
        }
    }
    
    /**
     * 清除所有安全存储的数据
     * 用于注销或重置应用
     */
    fun clearAll() {
        securePrefs.edit().clear().apply()
    }
}