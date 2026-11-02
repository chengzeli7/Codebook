package com.lcz.passwordmanager.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 基于 Android Keystore 的 AES-256-GCM 加密封装
 * 
 * 用于保护需要落盘的少量敏感值（如 SQLCipher 数据库密钥、盐值）。
 * 密钥本身由 Keystore 保管、不可导出；磁盘上只保存 base64(iv + 密文)。
 * 
 * 密文格式：首字符为 [PREFIX] 的 base64 字符串，绑定包名与 Keystore 密钥，
 * 复制到其他应用或其他设备后无法解密。
 */
internal class KeystoreCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val TRANSFORMATION = "$ALGORITHM/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12

        const val PREFIX = "v1:"

        private const val DEFAULT_KEY_ALIAS = "secure_prefs_key"

        /**
         * 判断一个已存储的值是否为 Keystore 信封（而非旧版本的明文）
         */
        fun isEnvelope(value: String): Boolean = value.startsWith(PREFIX)
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /**
     * 加密并返回可落盘的信封字符串
     */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val payload = ByteBuffer.allocate(IV_LENGTH + ciphertext.size)
            .put(cipher.iv)
            .put(ciphertext)
            .array()
        return PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    /**
     * 解开信封
     * @throws GeneralSecurityException 密钥丢失或被篡改时抛出
     */
    @Suppress("ThrowsCount")
    fun decrypt(envelope: String): String {
        require(isEnvelope(envelope)) { "不是有效的加密信封" }
        val payload = Base64.decode(envelope.removePrefix(PREFIX), Base64.NO_WRAP)
        if (payload.size <= IV_LENGTH) {
            throw GeneralSecurityException("密文长度不合法")
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH, payload, 0, IV_LENGTH)
        )
        val plaintext = cipher.doFinal(payload, IV_LENGTH, payload.size - IV_LENGTH)
        return String(plaintext, Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getEntry(keyAlias, null)
        if (existing is KeyStore.SecretKeyEntry) {
            return existing.secretKey
        }
        val generator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
