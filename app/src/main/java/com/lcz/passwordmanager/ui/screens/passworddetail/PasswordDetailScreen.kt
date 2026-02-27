package com.lcz.passwordmanager.ui.screens.passworddetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lcz.passwordmanager.domain.model.Category
import com.lcz.passwordmanager.ui.theme.*
import com.lcz.passwordmanager.ui.viewmodel.PasswordDetailViewModel

/**
 * 密码详情页面 - 极简风格
 * 
 * @author lcz
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: PasswordDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val passwordItem by viewModel.passwordItem.collectAsState()
    val showPassword by viewModel.showPassword.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("密码详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                passwordItem?.let { item ->
                    MinimalPasswordDetailContent(
                        passwordItem = item,
                        showPassword = showPassword,
                        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                        onCopyToClipboard = { label, text ->
                            copyToClipboard(context, label, text)
                        },
                        onDelete = {
                            viewModel.deletePassword(onNavigateBack)
                        }
                    )
                } ?: run {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "密码不存在",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 极简密码详情内容
 */
@Composable
private fun MinimalPasswordDetailContent(
    passwordItem: com.lcz.passwordmanager.domain.model.PasswordItem,
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onCopyToClipboard: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = passwordItem.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = passwordItem.category.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        MinimalDetailItem(
            label = "账号",
            value = passwordItem.username,
            onCopy = { onCopyToClipboard("账号", passwordItem.username) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        MinimalPasswordDetailItem(
            password = passwordItem.password,
            showPassword = showPassword,
            onToggleVisibility = onTogglePasswordVisibility,
            onCopy = { onCopyToClipboard("密码", passwordItem.password) }
        )
        
        passwordItem.url?.let { url ->
            if (url.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                MinimalDetailItem(
                    label = "网址",
                    value = url,
                    onCopy = { onCopyToClipboard("网址", url) }
                )
            }
        }
        
        passwordItem.note?.let { note ->
            if (note.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                MinimalDetailItem(
                    label = "备注",
                    value = note,
                    onCopy = { onCopyToClipboard("备注", note) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("删除")
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个密码吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 极简详情项
 */
@Composable
private fun MinimalDetailItem(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            TextButton(onClick = onCopy) {
                Text("复制")
            }
        }
    }
}

/**
 * 极简密码详情项
 */
@Composable
private fun MinimalPasswordDetailItem(
    password: String,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "密码",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (showPassword) password else "••••••••",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            TextButton(onClick = onToggleVisibility) {
                Text(if (showPassword) "隐藏" else "显示")
            }
            
            TextButton(onClick = onCopy) {
                Text("复制")
            }
        }
    }
}

/**
 * 复制到剪贴板
 */
private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}