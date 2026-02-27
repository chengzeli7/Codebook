package com.lcz.passwordmanager.domain.model

/**
 * 密码项领域模型
 * 
 * 用于UI层展示，包含解密后的密码
 * 
 * @property id 唯一标识
 * @property title 标题/站点名称
 * @property username 用户名
 * @property password 明文密码（仅在需要时解密）
 * @property category 分类
 * @property url 网址
 * @property note 备注
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 * 
 * @author lcz
 * @since 1.0.0
 */
data class PasswordItem(
    val id: String,
    val title: String,
    val username: String,
    val password: String = "",
    val category: Category,
    val url: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)