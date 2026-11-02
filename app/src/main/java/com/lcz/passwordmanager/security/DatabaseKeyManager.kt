package com.lcz.passwordmanager.security

import android.content.Context
import android.content.SharedPreferences
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库密钥管理器
 * 
 * 职责：
 * 1. 生成和管理SQLCipher数据库加密密钥
 * 2. 经 Keystore AES-GCM 加密后保存密钥，随应用卸载删除
 * 3. 支持密钥轮换和恢复
 * 
 * 安全架构：
 * - 数据库密钥：随机生成的256位密钥，用于SQLCipher
 * - 存储位置：SharedPreferences 文件中保存的是 Keystore 加密后的信封，
 *   磁盘上不存在明文密钥。
 *   注：Jetpack Security 的 EncryptedSharedPreferences 自 1.1.0 起已废弃，
 *   因此改为 Keystore 自管的 AES-256-GCM 封装（[KeystoreCipher]）。
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
     * 安全存储实例
     */
    private val securePrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    private val keystoreCipher: KeystoreCipher by lazy { KeystoreCipher() }

    private fun readSecureString(prefKey: String): String? {
        val stored = securePrefs.getString(prefKey, null) ?: return null
        if (!KeystoreCipher.isEnvelope(stored)) {
            // 兼容旧版本遗留的明文值：立即加密并改写
            val migrated = keystoreCipher.encrypt(stored)
            securePrefs.edit().putString(prefKey, migrated).apply()
            return stored
        }
        return try {
            keystoreCipher.decrypt(stored)
        } catch (e: GeneralSecurityException) {
            // 密钥丢失或被篡改：按“未保存”处理，后续会生成新密钥
            android.util.Log.e("DatabaseKeyManager", "解密 $prefKey 失败", e)
            null
        }
    }

    private fun writeSecureString(prefKey: String, value: String) {
        securePrefs.edit()
            .putString(prefKey, keystoreCipher.encrypt(value))
            .apply()
    }
    
    /**
     * 获取或创建数据库密钥
     * 
     * @return SQLCipher数据库密钥（字符串形式）
     */
    fun getOrCreateDatabaseKey(): String {
        val existingKey = readSecureString(KEY_DB_PASSPHRASE)
        
        return if (existingKey != null) {
            existingKey
        } else {
            // 生成新的随机密钥
            val newKey = generateSecureRandomKey()
            writeSecureString(KEY_DB_PASSPHRASE, newKey)
            newKey
        }
    }
    
    /**
     * 获取数据库密钥（如果不存在返回null）
     */
    fun getDatabaseKey(): String? {
        return readSecureString(KEY_DB_PASSPHRASE)
    }
    
    /**
     * 重新生成数据库密钥
     * 警告：这将使现有数据库无法访问，仅在首次设置或重置时使用
     */
    fun regenerateDatabaseKey(): String {
        val newKey = generateSecureRandomKey()
        writeSecureString(KEY_DB_PASSPHRASE, newKey)
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
        writeSecureString(
            KEY_MASTER_PASSWORD_SALT,
            android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
        )
    }
    
    /**
     * 获取主密码盐值
     * @return 盐值，不存在返回null
     */
    fun getMasterPasswordSalt(): ByteArray? {
        val saltString = readSecureString(KEY_MASTER_PASSWORD_SALT)
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