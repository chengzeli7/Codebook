package com.lcz.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lcz.passwordmanager.data.repository.PasswordRepository
import com.lcz.passwordmanager.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 添加/编辑密码ViewModel
 * 
 * 管理密码条目的创建和编辑
 * 
 * @author lcz
 * @since 1.0.0
 */
@HiltViewModel
class AddPasswordViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository
) : ViewModel() {

    /**
     * 表单状态
     */
    private val _uiState = MutableStateFlow(AddPasswordUiState())
    val uiState: StateFlow<AddPasswordUiState> = _uiState.asStateFlow()

    /**
     * 保存状态
     */
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    /**
     * 更新标题
     */
    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
        validateForm()
    }

    /**
     * 更新用户名
     */
    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
        validateForm()
    }

    /**
     * 更新密码
     */
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
        validateForm()
    }

    /**
     * 更新分类
     */
    fun updateCategory(category: Category) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    /**
     * 更新URL
     */
    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(url = url)
    }

    /**
     * 更新备注
     */
    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    /**
     * 生成随机密码
     * @param length 密码长度
     */
    fun generatePassword(length: Int = 16) {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val password = (1..length)
            .map { chars.random() }
            .joinToString("")
        _uiState.value = _uiState.value.copy(password = password)
        validateForm()
    }

    /**
     * 保存密码
     */
    fun savePassword() {
        if (!validateForm()) {
            _saveState.value = SaveState.Error("请填写必填项")
            return
        }

        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                val state = _uiState.value
                passwordRepository.addPassword(
                    title = state.title,
                    username = state.username,
                    plainPassword = state.password,
                    category = state.category.name,
                    url = state.url.takeIf { it.isNotBlank() },
                    note = state.note.takeIf { it.isNotBlank() }
                )
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "保存失败")
            }
        }
    }

    /**
     * 验证表单
     */
    private fun validateForm(): Boolean {
        val state = _uiState.value
        val isValid = state.title.isNotBlank() &&
                state.username.isNotBlank() &&
                state.password.isNotBlank()
        
        _uiState.value = state.copy(isValid = isValid)
        return isValid
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _uiState.value = AddPasswordUiState()
        _saveState.value = SaveState.Idle
    }

    /**
     * UI状态数据类
     */
    data class AddPasswordUiState(
        val title: String = "",
        val username: String = "",
        val password: String = "",
        val category: Category = Category.OTHER,
        val url: String = "",
        val note: String = "",
        val isValid: Boolean = false
    )

    /**
     * 保存状态密封类
     */
    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }
}