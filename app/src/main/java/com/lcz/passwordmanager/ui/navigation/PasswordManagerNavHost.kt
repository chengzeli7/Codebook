package com.lcz.passwordmanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lcz.passwordmanager.ui.screens.auth.AuthScreen
import com.lcz.passwordmanager.ui.screens.passwordlist.PasswordListScreen
import com.lcz.passwordmanager.ui.screens.addpassword.AddPasswordScreen
import com.lcz.passwordmanager.ui.screens.passworddetail.PasswordDetailScreen
import com.lcz.passwordmanager.ui.viewmodel.AuthViewModel

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
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    
    // 根据认证状态确定起始页面
    val startDestination = when (authState) {
        is AuthViewModel.AuthState.Authenticated -> NavRoutes.PASSWORD_LIST
        else -> NavRoutes.AUTH
    }
    
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
                }
            )
        }
        
        // 添加密码页面
        composable(NavRoutes.ADD_PASSWORD) {
            AddPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
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
}