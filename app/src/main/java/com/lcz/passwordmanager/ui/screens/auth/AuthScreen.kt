package com.lcz.passwordmanager.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lcz.passwordmanager.security.BiometricAuthManager
import com.lcz.passwordmanager.ui.viewmodel.AuthViewModel

/**
 * 认证页面 - iOS风格
 * 
 * @author lcz
 * @since 1.0.0
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val biometricStatus by viewModel.biometricStatus.collectAsState()
    
    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            onAuthSuccess()
        }
    }
    
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearError()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (authState) {
            is AuthViewModel.AuthState.FirstLaunch -> {
                iOSFirstLaunchScreen(
                    onSetPassword = { password ->
                        viewModel.setMasterPassword(password)
                    },
                    errorMessage = errorMessage
                )
            }
            is AuthViewModel.AuthState.RequireAuth,
            is AuthViewModel.AuthState.Checking -> {
                iOSAuthRequiredScreen(
                    viewModel = viewModel,
                    biometricStatus = biometricStatus,
                    errorMessage = errorMessage
                )
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * iOS风格首次启动设置密码界面
 */
@Composable
private fun iOSFirstLaunchScreen(
    onSetPassword: (String) -> Unit,
    errorMessage: String?
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        
        // 锁图标 - 圆形背景
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 标题
        Text(
            text = "小梨密码",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 副标题
        Text(
            text = "创建您的主密码",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // 密码输入框 - iOS风格
        IOSPasswordField(
            value = password,
            onValueChange = { 
                password = it
                showError = false
            },
            label = "主密码",
            placeholder = "至少6位",
            isVisible = passwordVisible,
            onVisibilityChange = { passwordVisible = it },
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 确认密码输入框
        IOSPasswordField(
            value = confirmPassword,
            onValueChange = { 
                confirmPassword = it
                showError = false
            },
            label = "确认密码",
            placeholder = "再次输入",
            isVisible = confirmPasswordVisible,
            onVisibilityChange = { confirmPasswordVisible = it },
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (password == confirmPassword && password.length >= 6) {
                        onSetPassword(password)
                    } else {
                        showError = true
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        // 错误提示
        if (showError || errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage ?: "密码格式不正确或两次输入不一致",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 设置按钮 - iOS风格圆角按钮
        Button(
            onClick = {
                focusManager.clearFocus()
                if (password == confirmPassword && password.length >= 6) {
                    onSetPassword(password)
                } else {
                    showError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = password.isNotBlank() && confirmPassword.isNotBlank()
        ) {
            Text(
                text = "创建密码",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 提示文字
        Text(
            text = "数据仅保存在本机，请牢记您的密码",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * iOS风格认证界面
 */
@Composable
private fun iOSAuthRequiredScreen(
    viewModel: AuthViewModel,
    biometricStatus: BiometricAuthManager.BiometricStatus,
    errorMessage: String?
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        
        // 锁图标
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 标题
        Text(
            text = "小梨密码",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 副标题
        Text(
            text = "输入主密码解锁",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // 生物识别按钮
        if (biometricStatus == BiometricAuthManager.BiometricStatus.AVAILABLE &&
            viewModel.isBiometricEnabled && activity != null
        ) {
            // 生物识别按钮 - iOS风格
            OutlinedButton(
                onClick = {
                    activity?.let { viewModel.authenticateWithBiometric(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "使用生物识别",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 分隔线
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "或",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // 密码输入框
        IOSPasswordField(
            value = password,
            onValueChange = { password = it },
            label = "主密码",
            placeholder = "输入密码",
            isVisible = passwordVisible,
            onVisibilityChange = { passwordVisible = it },
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.verifyMasterPassword(password)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        // 错误提示
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 解锁按钮
        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.verifyMasterPassword(password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = password.isNotBlank()
        ) {
            Text(
                text = "解锁",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/**
 * iOS风格密码输入框
 */
@Composable
private fun IOSPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { onVisibilityChange(!isVisible) }) {
                Icon(
                    imageVector = if (isVisible) Icons.Filled.Person else Icons.Filled.Lock,
                    contentDescription = if (isVisible) "隐藏密码" else "显示密码",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    )
}