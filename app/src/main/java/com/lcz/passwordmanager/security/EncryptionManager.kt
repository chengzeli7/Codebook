package com.lcz.passwordmanager.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
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
class EncryptionManager @Inject constructor() {
    
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
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
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
     * 安全清除CharArray
     * 将数组填充为零，防止密码在内存中残留
     * 
     * @param array 需要清除的字符数组
     */
    fun clearCharArray(array: CharArray) {
        array.fill('\u0000')
    }
}