package com.lcz.passwordmanager.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 密码分类枚举
 * 
 * 预设4个默认分类，不支持用户自定义（减少复杂度）
 * 
 * @property displayName 显示名称
 * @property icon 图标
 * 
 * @author lcz
 * @since 1.0.0
 */
enum class Category(
    val displayName: String,
    val icon: ImageVector
) {
    /**
     * 社交账号
     */
    SOCIAL("社交账号", Icons.Default.Person),
    
    /**
     * 金融银行
     */
    FINANCE("金融银行", Icons.Default.Favorite),
    
    /**
     * 工作相关
     */
    WORK("工作相关", Icons.Default.Star),
    
    /**
     * 其他
     */
    OTHER("其他", Icons.Default.Home);
    
    companion object {
        /**
         * 根据名称获取分类
         * @param name 分类名称
         * @return 分类枚举，找不到时返回OTHER
         */
        fun fromName(name: String): Category {
            return entries.find { it.name == name } ?: OTHER
        }
        
        /**
         * 获取所有分类
         */
        fun getAllCategories(): List<Category> = entries.toList()
    }
}