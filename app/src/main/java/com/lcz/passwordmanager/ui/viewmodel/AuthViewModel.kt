package com.lcz.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.fragment.app.FragmentActivity
import com.lcz.passwordmanager.security.BiometricAuthManager
import com.lcz.passwordmanager.security.MasterPasswordManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val biometricAuthManager: BiometricAuthManager
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
     * @param password 密码
     * @return 是否设置成功
     */
    fun setMasterPassword(password: String): Boolean {
        return if (masterPasswordManager.setMasterPassword(password)) {
            _authState.value = AuthState.Authenticated
            true
        } else {
            _errorMessage.value = "密码格式不正确，需要6位以上数字或字母组合"
            false
        }
    }

    /**
     * 验证主密码
     * @param password 密码
     * @return 是否验证通过
     */
    fun verifyMasterPassword(password: String): Boolean {
        return if (masterPasswordManager.verifyMasterPassword(password)) {
            _authState.value = AuthState.Authenticated
            true
        } else {
            _errorMessage.value = "密码错误"
            false
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