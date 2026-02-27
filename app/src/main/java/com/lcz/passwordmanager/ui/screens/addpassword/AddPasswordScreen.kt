package com.lcz.passwordmanager.ui.screens.addpassword

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lcz.passwordmanager.domain.model.Category
import com.lcz.passwordmanager.ui.viewmodel.AddPasswordViewModel
import java.util.regex.Pattern

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
    clipboardMonitor: com.lcz.passwordmanager.service.ClipboardMonitor? = null,
    viewModel: AddPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val context = LocalContext.current
    
    // 页面加载时检查是否有预填数据
    LaunchedEffect(Unit) {
        clipboardMonitor?.detectedCredentials?.collect { credentials ->
            if (credentials != null) {
                if (credentials.username.isNotEmpty()) {
                    viewModel.updateUsername(credentials.username)
                }
                if (credentials.password.isNotEmpty()) {
                    viewModel.updatePassword(credentials.password)
                }
                // 提取标题（第一行）
                val lines = credentials.rawText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isNotEmpty()) {
                    val firstLine = lines[0]
                    if (!firstLine.contains(Regex("(账号|用户名|user|account|密码|password|pwd)", RegexOption.IGNORE_CASE))) {
                        viewModel.updateTitle(firstLine)
                    }
                }
                // 清除剪贴板数据
                clipboardMonitor.clearDetectedCredentials()
            }
        }
    }
    
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
                },
                actions = {
                    // 粘贴按钮 - 从剪贴板读取并自动填充
                    IconButton(
                        onClick = {
                            val clipboardText = readClipboard(context)
                            if (clipboardText != null) {
                                val (username, password, title) = parseCredentials(clipboardText)
                                if (title.isNotEmpty()) {
                                    viewModel.updateTitle(title)
                                }
                                if (username.isNotEmpty()) {
                                    viewModel.updateUsername(username)
                                }
                                if (password.isNotEmpty()) {
                                    viewModel.updatePassword(password)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "粘贴")
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
 * 读取剪贴板内容
 */
private fun readClipboard(context: Context): String? {
    return try {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).text?.toString()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * 从文本中解析账号密码和标题
 */
private fun parseCredentials(text: String): Triple<String, String, String> {
    val normalizedText = text.replace("\n", " ").replace("\r", " ")
    
    // 先尝试识别标题（第一行通常是标题，如 qq、微信等）
    val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    var title = ""
    if (lines.isNotEmpty()) {
        val firstLine = lines[0]
        // 如果第一行不包含账号/密码关键字，且长度较短，视为标题
        if (!firstLine.contains(Regex("(账号|用户名|user|account|密码|password|pwd)", RegexOption.IGNORE_CASE)) && firstLine.length <= 20) {
            title = firstLine
        }
    }
    
    val patterns = listOf(
        // 账号 1233 密码 1231231
        Pattern.compile(
            "(?:账号|用户名|user|account)\\s+(\\S+)\\s*(?:密码|password|pwd)\\s+(\\S+)",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        ),
        // 密码 1231231 账号 1233
        Pattern.compile(
            "(?:密码|password|pwd)\\s+(\\S+)\\s*(?:账号|用户名|user|account)\\s+(\\S+)",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        ),
        // 账号:1233 密码:1231231
        Pattern.compile(
            "(?:账号|用户名|user|account)[:：]\\s*(\\S+)\\s*(?:密码|password|pwd)[:：]\\s*(\\S+)",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        ),
        // 密码:1231231 账号:1233
        Pattern.compile(
            "(?:密码|password|pwd)[:：]\\s*(\\S+)\\s*(?:账号|用户名|user|account)[:：]\\s*(\\S+)",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
    )
    
    for (pattern in patterns) {
        val matcher = pattern.matcher(normalizedText)
        if (matcher.find() && matcher.groupCount() >= 2) {
            return Triple(matcher.group(1) ?: "", matcher.group(2) ?: "", title)
        }
    }
    
    // 如果没有匹配到账号密码格式，返回空
    return Triple("", "", title)
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
        placeholder = { Text("密码") },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            Row {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Lock else Icons.Default.Lock,
                        contentDescription = if (passwordVisible) "隐藏" else "显示"
                    )
                }
                IconButton(onClick = onGeneratePassword) {
                    Icon(Icons.Default.Refresh, contentDescription = "生成")
                }
            }
        },
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
    var expanded by remember { mutableStateOf(false) }
    val categories = Category.getAllCategories()
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedCategory.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("分类") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}