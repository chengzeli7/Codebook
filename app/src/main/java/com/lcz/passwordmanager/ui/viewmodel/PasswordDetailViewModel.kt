package com.lcz.passwordmanager.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lcz.passwordmanager.data.repository.PasswordRepository
import com.lcz.passwordmanager.domain.model.Category
import com.lcz.passwordmanager.domain.model.PasswordItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 密码详情ViewModel
 * 
 * 管理密码详情页的展示和操作
 * 
 * @author lcz
 * @since 1.0.0
 */
@HiltViewModel
class PasswordDetailViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * 密码ID
     */
    private val passwordId: String = savedStateHandle["passwordId"] ?: ""

    /**
     * 密码详情状态
     */
    private val _passwordItem = MutableStateFlow<PasswordItem?>(null)
    val passwordItem: StateFlow<PasswordItem?> = _passwordItem.asStateFlow()

    /**
     * 是否显示密码
     */
    private val _showPassword = MutableStateFlow(false)
    val showPassword: StateFlow<Boolean> = _showPassword.asStateFlow()

    /**
     * 加载状态
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 错误信息
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPassword()
    }

    /**
     * 加载密码详情
     */
    private fun loadPassword() {
        if (passwordId.isBlank()) {
            _errorMessage.value = "密码ID无效"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entity = passwordRepository.getPasswordById(passwordId)
                if (entity != null) {
                    // 解密密码
                    val decryptedPassword = passwordRepository.decryptPassword(entity.encryptedPassword)
                    _passwordItem.value = PasswordItem(
                        id = entity.id,
                        title = entity.title,
                        username = entity.username,
                        password = decryptedPassword,
                        category = Category.fromName(entity.category),
                        url = entity.url,
                        note = entity.note,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                } else {
                    _errorMessage.value = "密码不存在"
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 切换密码显示状态
     */
    fun togglePasswordVisibility() {
        _showPassword.value = !_showPassword.value
    }

    /**
     * 删除密码
     */
    fun deletePassword(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                passwordRepository.deletePasswordById(passwordId)
                onDeleted()
            } catch (e: Exception) {
                _errorMessage.value = "删除失败: ${e.message}"
            }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _errorMessage.value = null
    }
}