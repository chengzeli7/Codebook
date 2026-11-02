package com.lcz.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.fragment.app.FragmentActivity
import com.lcz.passwordmanager.security.BiometricAuthManager
import com.lcz.passwordmanager.security.EncryptionManager
import com.lcz.passwordmanager.security.MasterPasswordManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 认证ViewModel
 * 
 * 管理应用认证流程：
 * 1. 首次启动设置主密码
 * 2. 生物识别认证
 * 3. 主密码验证
 * 
 * @author lcz
 * @since 1.0.0
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val masterPasswordManager: MasterPasswordManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    /**
     * 认证状态
     */
    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * 生物识别可用性
     */
    private val _biometricStatus = MutableStateFlow(
        biometricAuthManager.checkBiometricAvailability()
    )
    val biometricStatus: StateFlow<BiometricAuthManager.BiometricStatus> = _biometricStatus.asStateFlow()

    /**
     * 错误信息
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 是否首次启动
     */
    val isFirstLaunch: Boolean
        get() = masterPasswordManager.isFirstLaunch()

    /**
     * 是否启用生物识别
     */
    val isBiometricEnabled: Boolean
        get() = masterPasswordManager.isBiometricEnabled()

    init {
        checkAuthState()
    }

    /**
     * 检查认证状态
     */
    fun checkAuthState() {
        when {
            masterPasswordManager.isFirstLaunch() -> {
                _authState.value = AuthState.FirstLaunch
            }
            masterPasswordManager.isMasterPasswordSet() -> {
                _authState.value = AuthState.RequireAuth
            }
            else -> {
                _authState.value = AuthState.FirstLaunch
            }
        }
    }

    /**
     * 设置主密码
     * PBKDF2 派生在 IO 线程执行，结果通过 onResult 回调回主线程。
     * @param password 密码
     * @param onResult 是否设置成功
     */
    fun setMasterPassword(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.Default) {
                masterPasswordManager.setMasterPassword(password)
            }
            if (success) {
                // 主密码验证通过后，用主密码加密 Keystore 生成的密钥，
                // 使指纹等生物特征单独无法解密数据
                withContext(Dispatchers.Default) { rebindDataKey(password) }
                _authState.value = AuthState.Authenticated
            } else {
                _errorMessage.value = "密码格式不正确，需要6位以上数字或字母组合"
            }
            onResult(success)
        }
    }

    /**
     * 验证主密码
     * PBKDF2 派生在 IO 线程执行，结果通过 onResult 回调回主线程。
     * @param password 密码
     * @param onResult 是否验证通过
     */
    fun verifyMasterPassword(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.Default) {
                masterPasswordManager.verifyMasterPassword(password)
            }
            if (success) {
                withContext(Dispatchers.Default) { rebindDataKey(password) }
                _authState.value = AuthState.Authenticated
            } else {
                _errorMessage.value = "密码错误"
            }
            onResult(success)
        }
    }

    /**
     * 启动生物识别认证
     * @param activity FragmentActivity
     */
    fun authenticateWithBiometric(activity: FragmentActivity) {
        viewModelScope.launch {
            val result = biometricAuthManager.authenticate(activity)
            when (result) {
                is BiometricAuthManager.AuthResult.Success -> {
                    _authState.value = AuthState.Authenticated
                }
                is BiometricAuthManager.AuthResult.Cancelled -> {
                    // 用户取消，保持当前状态
                }
                is BiometricAuthManager.AuthResult.Locked -> {
                    _errorMessage.value = "生物识别已锁定，请使用主密码"
                }
                is BiometricAuthManager.AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    /**
     * 启用/禁用生物识别
     * @param enabled 是否启用
     */
    fun setBiometricEnabled(enabled: Boolean) {
        masterPasswordManager.setBiometricEnabled(enabled)
    }

    /**
     * 用已验证的主密码绑定数据加密密钥。
     * 失败不影响进入应用（数据密钥仍加密保存在磁盘上），仅记录以便排查。
     */
    private fun rebindDataKey(password: String) {
        try {
            encryptionManager.rebindWithMasterPassword(password)
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "数据密钥绑定主密码失败", e)
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 认证状态密封类
     */
    sealed class AuthState {
        object Checking : AuthState()      // 检查中
        object FirstLaunch : AuthState()   // 首次启动
        object RequireAuth : AuthState()   // 需要认证
        object Authenticated : AuthState() // 已认证
    }
}