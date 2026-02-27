package com.lcz.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lcz.passwordmanager.data.local.entity.PasswordEntity
import com.lcz.passwordmanager.data.repository.PasswordRepository
import com.lcz.passwordmanager.domain.model.Category
import com.lcz.passwordmanager.domain.model.PasswordItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 密码列表ViewModel
 * 
 * 管理密码列表的展示、搜索、筛选等UI状态
 * 
 * @author lcz
 * @since 1.0.0
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class PasswordListViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository
) : ViewModel() {

    /**
     * 搜索关键词
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * 当前选中的分类筛选
     */
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    /**
     * 密码列表状态
     */
    val passwordList: StateFlow<List<PasswordItem>> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }
        .debounce(300) // 防抖300ms
        .flatMapLatest { (query, category) ->
            when {
                query.isNotBlank() -> passwordRepository.searchPasswords(query)
                category != null -> passwordRepository.getPasswordsByCategory(category.name)
                else -> passwordRepository.getAllPasswords()
            }
        }
        .map { entities ->
            entities.map { it.toPasswordItem() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 加载状态
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 错误状态
     */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 更新搜索关键词
     * @param query 搜索词
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 选择分类筛选
     * @param category 分类，null表示全部
     */
    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
    }

    /**
     * 删除密码
     * @param id 密码ID
     */
    fun deletePassword(id: String) {
        viewModelScope.launch {
            try {
                passwordRepository.deletePasswordById(id)
            } catch (e: Exception) {
                _error.value = "删除失败: ${e.message}"
            }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 将实体转换为领域模型
     */
    private fun PasswordEntity.toPasswordItem(): PasswordItem {
        return PasswordItem(
            id = id,
            title = title,
            username = username,
            password = "", // 默认不解密，需要时单独获取
            category = Category.fromName(category),
            url = url,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}