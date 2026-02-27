package com.lcz.passwordmanager.ui.navigation

/**
 * 导航路由定义
 * 
 * @author lcz
 * @since 1.0.0
 */
object NavRoutes {
    
    /**
     * 认证页面
     */
    const val AUTH = "auth"
    
    /**
     * 密码列表页面
     */
    const val PASSWORD_LIST = "password_list"
    
    /**
     * 添加密码页面
     */
    const val ADD_PASSWORD = "add_password"
    
    /**
     * 密码详情页面
     * @param passwordId 密码ID
     */
    const val PASSWORD_DETAIL = "password_detail/{passwordId}"
    
    /**
     * 构建密码详情路由
     */
    fun passwordDetail(passwordId: String): String {
        return "password_detail/$passwordId"
    }
    
    /**
     * 设置页面
     */
    const val SETTINGS = "settings"
}