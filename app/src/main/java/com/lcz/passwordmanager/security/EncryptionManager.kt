package com.lcz.passwordmanager.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 加密管理器 - 负责数据的加密和解密
 * 
 * 安全规范：
 * 1. 使用AES-256-GCM模式加密
 * 2. 密钥存储于Android Keystore
 * 3. 每次加密使用随机IV
 * 4. 支持CharArray清零（内存安全）
 * 
 * @author lcz
 * @since 1.0.0
 */
@Singleton
class EncryptionManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        /**
         * Android Keystore提供者名称
         */
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        
        /**
         * 加密算法
         */
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        
        /**
         * 加密模式
         */
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        
        /**
         * 填充方式
         */
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        
        /**
         * 密钥别名
         */
        private const val KEY_ALIAS = "password_encryption_key"
        
        /**
         * GCM认证标签长度（位）
         */
        private const val GCM_TAG_LENGTH = 128
        
        /**
         * IV长度（字节）
         */
        private const val IV_LENGTH = 12

        /**
         * 数据密钥主密码信封的存储位置
         */
        private const val PREFS_NAME = "key_wrap_prefs"
        private const val KEY_WRAPPED_DATA_KEY = "wrapped_data_key"
        private const val KEY_WRAP_SALT = "wrap_salt"

        /**
         * 主密码信封派生参数（仅用于加密 Keystore 中的密钥，不是密码哈希本身）
         */
        private const val WRAP_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val WRAP_ITERATIONS = 100_000
        private const val WRAP_KEY_LENGTH_BITS = 256
        private const val WRAP_SALT_LENGTH = 16
    }
    
    /**
     * 惰性打开 Keystore：KeyStore 不可用时应在使用处报错，而不是让 Singleton 构造直接崩溃
     */
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }
    
    init {
        // 确保密钥存在
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }
    
    /**
     * 生成AES密钥并存储于Keystore
     */
    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEYSTORE)
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // 不需要生物识别即可使用密钥
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    
    /**
     * 从Keystore获取密钥
     */
    private fun getKey(): SecretKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }
    
    /**
     * 加密明文数据
     * 
     * @param plaintext 明文
     * @return Base64编码的密文（包含IV）
     */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance("$ALGORITHM/$BLOCK_MODE/$PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        // 组合IV和密文
        val buffer = ByteBuffer.allocate(IV_LENGTH + ciphertext.size)
        buffer.put(iv)
        buffer.put(ciphertext)
        
        return Base64.encodeToString(buffer.array(), Base64.DEFAULT)
    }
    
    /**
     * 解密密文数据
     * 
     * @param encryptedData Base64编码的密文（包含IV）
     * @return 明文
     */
    fun decrypt(encryptedData: String): String {
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)
        
        val buffer = ByteBuffer.wrap(combined)
        val iv = ByteArray(IV_LENGTH)
        buffer.get(iv)
        
        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)
        
        val cipher = Cipher.getInstance("$ALGORITHM/$BLOCK_MODE/$PADDING")
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
    
    /**
     * 用主密码重新加密 Keystore 中的数据密钥，保存为“主密码信封”。
     * 
     * 目的：密钥文件虽然存放在私有目录，但仅靠 Keystore 时，设备上的指纹/生物特征
     * 可能被本地利用；绑定主密码后，没有主密码就无法还原数据密钥。
     * 每次主密码验证成功后调用（等同于重新加密一次）。
     * 
     * 注意：JVM 端（单元测试）没有 AndroidKeyStore，调用方需容错。
     *
     * @param masterPassword 已验证的主密码
     */
    fun rebindWithMasterPassword(masterPassword: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val salt = ByteArray(WRAP_SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val wrappingKey = deriveKey(masterPassword, salt)

        val rawKey = getKey().encoded
        val cipher = Cipher.getInstance("$ALGORITHM/$BLOCK_MODE/$PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(wrappingKey, ALGORITHM))
        val ciphertext = cipher.doFinal(rawKey)
        rawKey.fill(0)

        val payload = ByteBuffer.allocate(IV_LENGTH + ciphertext.size)
            .put(cipher.iv)
            .put(ciphertext)
            .array()
        prefs.edit()
            .putString(KEY_WRAP_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_WRAPPED_DATA_KEY, Base64.encodeToString(payload, Base64.NO_WRAP))
            .apply()
        wrappingKey.fill(0)
    }

    /**
     * 使用主密码解开数据密钥信封。
     * @return 是否成功（主密码错误或尚未绑定时为 false）
     */
    fun unwrapWithMasterPassword(masterPassword: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val salt = prefs.getString(KEY_WRAP_SALT, null) ?: return false
        val payload = prefs.getString(KEY_WRAPPED_DATA_KEY, null) ?: return false
        return try {
            val wrappingKey = deriveKey(
                masterPassword,
                Base64.decode(salt, Base64.NO_WRAP)
            )
            val data = Base64.decode(payload, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("$ALGORITHM/$BLOCK_MODE/$PADDING")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(wrappingKey, ALGORITHM),
                GCMParameterSpec(GCM_TAG_LENGTH, data, 0, IV_LENGTH)
            )
            val rawKey = cipher.doFinal(data, IV_LENGTH, data.size - IV_LENGTH)
            val sameAsKeystore = MessageDigest.isEqual(rawKey, getKey().encoded)
            rawKey.fill(0)
            wrappingKey.fill(0)
            sameAsKeystore
        } catch (e: GeneralSecurityException) {
            // 主密码错误时 GCM 认证失败
            false
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, WRAP_ITERATIONS, WRAP_KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(WRAP_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    /**
     * 安全清除CharArray
     * 将数组填充为零，防止密码在内存中残留
     * 
     * @param array 需要清除的字符数组
     */
    fun clearCharArray(array: CharArray) {
        array.fill('\u0000')
    }
}