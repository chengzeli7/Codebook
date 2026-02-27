package com.lcz.passwordmanager.ui.screens.addpassword

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lcz.passwordmanager.domain.model.Category
import com.lcz.passwordmanager.ui.viewmodel.AddPasswordViewModel

/**
 * 添加密码页面 - 极简风格
 * 
 * @author lcz
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    
    LaunchedEffect(saveState) {
        when (saveState) {
            is AddPasswordViewModel.SaveState.Success -> {
                viewModel.resetState()
                onNavigateBack()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("添加密码") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("标题") },
                placeholder = { Text("例如：微信") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::updateUsername,
                label = { Text("账号") },
                placeholder = { Text("用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            MinimalPasswordField(
                password = uiState.password,
                onPasswordChange = viewModel::updatePassword,
                onGeneratePassword = { viewModel.generatePassword() },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            MinimalCategorySelector(
                selectedCategory = uiState.category,
                onCategorySelected = viewModel::updateCategory
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::updateUrl,
                label = { Text("网址") },
                placeholder = { Text("https://") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::updateNote,
                label = { Text("备注") },
                placeholder = { Text("其他信息") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = viewModel::savePassword,
                enabled = uiState.isValid && saveState !is AddPasswordViewModel.SaveState.Saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saveState is AddPasswordViewModel.SaveState.Saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("保存")
                }
            }
            
            if (saveState is AddPasswordViewModel.SaveState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (saveState as AddPasswordViewModel.SaveState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 极简密码输入框
 */
@Composable
private fun MinimalPasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    onGeneratePassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("密码") },
        placeholder = { Text("输入或生成") },
        trailingIcon = {
            Row {
                IconButton(onClick = onGeneratePassword) {
                    Icon(Icons.Default.Refresh, contentDescription = "生成")
                }
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Lock else Icons.Filled.Lock,
                        contentDescription = if (passwordVisible) "隐藏" else "显示"
                    )
                }
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        modifier = modifier
    )
}

/**
 * 极简分类选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalCategorySelector(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Category.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) }
            )
        }
    }
}