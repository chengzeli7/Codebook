package com.lcz.passwordmanager.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * 主密码派生与格式校验的单元测试
 *
 * 不依赖模拟器：Context 使用 Mockito 桩对象，派生算法直接在 JVM 上验证。
 */
class PasswordHashingTest {

    /** 不访问 SharedPreferences 的占位 Context，仅用于构造被测对象 */
    private fun testingContext(): android.content.Context = mock()

    private lateinit var manager: MasterPasswordManager

    @Before
    fun setUp() {
        manager = MasterPasswordManager(testingContext())
    }

    @Test
    fun passwordValidation_acceptsOnlyAsciiLettersAndDigitsWithSixOrMoreChars() {
        assertTrue(manager.isPasswordValid("abc123"))
        assertTrue(manager.isPasswordValid("ABCDEF"))
        assertFalse(manager.isPasswordValid("abc12"))
        assertFalse(manager.isPasswordValid("abc 123"))
        assertFalse(manager.isPasswordValid("密码123456"))
        // 全角字母数字不能再绕过校验
        assertFalse(manager.isPasswordValid("ａｂｃ１２３"))
    }

    @Test
    fun deriveKeyBytes_isDeterministicForSameSalt() {
        val salt = ByteArray(16) { it.toByte() }
        val first = manager.deriveKeyBytes("abc123", salt)
        val second = manager.deriveKeyBytes("abc123", salt)
        assertArrayEquals(first, second)
        assertNotEquals(0, first.size)
    }

    @Test
    fun deriveKeyBytes_changesWithSaltAndPassword() {
        val saltA = ByteArray(16) { 1 }
        val saltB = ByteArray(16) { 2 }
        val base = manager.deriveKeyBytes("abc123", saltA)
        assertFalse(base.contentEquals(manager.deriveKeyBytes("abc123", saltB)))
        assertFalse(base.contentEquals(manager.deriveKeyBytes("abc124", saltA)))
    }

    @Test
    fun deriveKeyBytes_isNotPlainSha256() {
        val salt = ByteArray(16)
        val derived = manager.deriveKeyBytes("abc123", salt)
        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
            .digest("abc123".toByteArray())
        assertFalse(derived.contentEquals(sha256))
    }
}
