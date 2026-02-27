package com.lcz.passwordmanager.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lcz.passwordmanager.service.ClipboardMonitor
import com.lcz.passwordmanager.ui.screens.auth.AuthScreen
import com.lcz.passwordmanager.ui.screens.passwordlist.PasswordListScreen
import com.lcz.passwordmanager.ui.screens.addpassword.AddPasswordScreen
import com.lcz.passwordmanager.ui.screens.passworddetail.PasswordDetailScreen
import com.lcz.passwordmanager.ui.viewmodel.AuthViewModel

private const val TAG = "NavHost"

/**
 * 应用导航宿主
 * 
 * 管理应用页面导航流程
 * 
 * @author lcz
 * @since 1.0.0
 */
@Composable
fun PasswordManagerNavHost(
    navController: NavHostController = rememberNavController(),
    clipboardMonitor: ClipboardMonitor,
    onNavigateToAddPassword: (String?, String?) -> Unit = { _, _ -> }
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val detectedCredentials by clipboardMonitor.detectedCredentials.collectAsState()
    
    // 根据认证状态确定起始页面
    val startDestination = when (authState) {
        is AuthViewModel.AuthState.Authenticated -> NavRoutes.PASSWORD_LIST
        else -> NavRoutes.AUTH
    }
    
    // 检测到剪贴板账号密码时显示弹窗
    var showCredentialDialog by remember { mutableStateOf(false) }
    var pendingCredentials by remember { mutableStateOf<ClipboardMonitor.Credentials?>(null) }
    
    // 监听剪贴板检测结果
    LaunchedEffect(detectedCredentials) {
        val credentials = detectedCredentials
        val state = authState
        Log.d(TAG, "LaunchedEffect: credentials=$credentials, state=$state")
        if (credentials != null && state is AuthViewModel.AuthState.Authenticated) {
            Log.d(TAG, "显示弹窗: username=${credentials.username}")
            pendingCredentials = credentials
            showCredentialDialog = true
        }
    }
    
    // 监听生命周期变化，每次回到前台延迟2秒后检查剪贴板
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 延迟2秒后再检查剪贴板（等待系统准备好）
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    clipboardMonitor.checkClipboard()
                }, 2000)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            // 认证页面
            composable(NavRoutes.AUTH) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(NavRoutes.PASSWORD_LIST) {
                            popUpTo(NavRoutes.AUTH) { inclusive = true }
                        }
                    }
                )
            }
            
            // 密码列表页面
            composable(NavRoutes.PASSWORD_LIST) {
                PasswordListScreen(
                    onAddPassword = {
                        navController.navigate(NavRoutes.ADD_PASSWORD)
                    },
                    onPasswordClick = { passwordId ->
                        navController.navigate(NavRoutes.passwordDetail(passwordId))
                    },
                    clipboardMonitor = clipboardMonitor
                )
            }
            
            // 添加密码页面
            composable(NavRoutes.ADD_PASSWORD) {
                AddPasswordScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    clipboardMonitor = clipboardMonitor
                )
            }
            
            // 密码详情页面
            composable(
                route = NavRoutes.PASSWORD_DETAIL,
                arguments = listOf(
                    navArgument("passwordId") { type = NavType.StringType }
                )
            ) {
                PasswordDetailScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        // 剪贴板检测到账号密码时的提示弹窗
        if (showCredentialDialog && pendingCredentials != null) {
            val credentials = pendingCredentials!!
            
            AlertDialog(
                onDismissRequest = {
                    showCredentialDialog = false
                    clipboardMonitor.clearDetectedCredentials()
                },
                title = {
                    Text("检测到账号密码")
                },
                text = {
                    Text(
                        "是否保存以下账号密码？\n\n" +
                        if (credentials.username.isNotEmpty()) "账号: ${credentials.username}\n" else "" +
                        "密码: ${credentials.password}"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCredentialDialog = false
                            clipboardMonitor.clearDetectedCredentials()
                            // 导航到添加密码页面
                            onNavigateToAddPassword(credentials.username, credentials.password)
                            navController.navigate(NavRoutes.ADD_PASSWORD)
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCredentialDialog = false
                            clipboardMonitor.ignoreCurrentContent(credentials.rawText)
                        }
                    ) {
                        Text("忽略")
                    }
                }
            )
        }
    }
}