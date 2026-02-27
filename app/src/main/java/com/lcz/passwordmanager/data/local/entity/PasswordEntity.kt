package com.lcz.passwordmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 密码实体类 - Room数据库表定义
 * 
 * 数据安全说明：
 * 1. 密码字段使用AES-256加密后存储（encryptedPassword）
 * 2. 数据库整体使用SQLCipher加密
 * 3. 密钥存储于Android Keystore
 * 
 * @property id 唯一标识符（UUID）
 * @property title 站点/应用名称（明文存储）
 * @property username 用户名/账号（明文或轻度混淆）
 * @property encryptedPassword AES加密后的密码（Base64编码）
 * @property category 分类枚举（SOCIAL/FINANCE/WORK/OTHER）
 * @property url 关联网址（可选）
 * @property note 备注信息（可选）
 * @property createdAt 创建时间戳
 * @property updatedAt 最后更新时间戳
 * 
 * @author lcz
 * @since 1.0.0
 */
@Entity(
    tableName = "passwords",
    indices = [
        Index(value = ["category"]),
        Index(value = ["title"]),
        Index(value = ["created_at"])
    ]
)
data class PasswordEntity(
    
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "username")
    val username: String,
    
    /**
     * 加密后的密码
     * 使用AES-256-GCM模式加密，密钥存储于Keystore
     */
    @ColumnInfo(name = "encrypted_password")
    val encryptedPassword: String,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "url")
    val url: String? = null,
    
    @ColumnInfo(name = "note")
    val note: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)