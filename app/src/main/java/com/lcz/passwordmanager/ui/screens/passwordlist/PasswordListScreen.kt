package com.lcz.passwordmanager.ui.screens.passwordlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lcz.passwordmanager.domain.model.Category
import com.lcz.passwordmanager.domain.model.PasswordItem
import com.lcz.passwordmanager.ui.theme.*
import com.lcz.passwordmanager.ui.viewmodel.PasswordListViewModel

/**
 * 密码列表页面 - 极简风格
 * 
 * @author lcz
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordListScreen(
    onAddPassword: () -> Unit,
    onPasswordClick: (String) -> Unit,
    viewModel: PasswordListViewModel = hiltViewModel()
) {
    val passwordList by viewModel.passwordList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val errorMessage by viewModel.error.collectAsState()
    
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("小梨密码") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPassword,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索栏 - 极简
            MinimalSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // 分类筛选 - 极简
            MinimalCategoryFilter(
                selectedCategory = selectedCategory,
                onCategorySelected = viewModel::selectCategory,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 密码列表
            if (passwordList.isEmpty()) {
                MinimalEmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(passwordList) { password ->
                        MinimalPasswordCard(
                            password = password,
                            onClick = { onPasswordClick(password.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 极简搜索栏
 */
@Composable
private fun MinimalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("搜索") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "清除")
                }
            }
        },
        singleLine = true,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium
    )
}

/**
 * 极简分类筛选
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalCategoryFilter(
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(null) + Category.getAllCategories()
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.take(5).forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category?.displayName ?: "全部") }
            )
        }
    }
}

/**
 * 极简密码卡片
 */
@Composable
private fun MinimalPasswordCard(
    password: PasswordItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = password.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = password.username,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 极简空状态
 */
@Composable
private fun MinimalEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无密码",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}