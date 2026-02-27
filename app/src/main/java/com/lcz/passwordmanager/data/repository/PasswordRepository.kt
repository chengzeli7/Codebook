package com.lcz.passwordmanager.data.repository

import com.lcz.passwordmanager.data.local.dao.PasswordDao
import com.lcz.passwordmanager.data.local.entity.PasswordEntity
import com.lcz.passwordmanager.security.EncryptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 密码仓库 - 数据层与业务层的桥梁
 * 
 * 职责：
 * 1. 封装数据访问逻辑
 * 2. 处理密码的加密/解密
 * 3. 提供响应式数据流
 * 
 * @author lcz
 * @since 1.0.0
 */
@Singleton
class PasswordRepository @Inject constructor(
    private val passwordDao: PasswordDao,
    private val encryptionManager: EncryptionManager
) {
    
    /**
     * 获取所有密码（自动解密）
     * @return 解密后的密码列表Flow
     */
    fun getAllPasswords(): Flow<List<PasswordEntity>> {
        return passwordDao.getAllPasswords()
    }
    
    /**
     * 根据分类获取密码
     * @param category 分类名称
     * @return 该分类的密码列表Flow
     */
    fun getPasswordsByCategory(category: String): Flow<List<PasswordEntity>> {
        return passwordDao.getPasswordsByCategory(category)
    }
    
    /**
     * 搜索密码
     * @param query 搜索关键词
     * @return 匹配的密码列表Flow
     */
    fun searchPasswords(query: String): Flow<List<PasswordEntity>> {
        return passwordDao.searchPasswords(query)
    }
    
    /**
     * 根据ID获取密码
     * @param id 密码ID
     * @return 密码实体或null
     */
    suspend fun getPasswordById(id: String): PasswordEntity? {
        return passwordDao.getPasswordById(id)
    }
    
    /**
     * 添加新密码
     * 自动加密密码字段
     * 
     * @param title 标题
     * @param username 用户名
     * @param plainPassword 明文密码（将被加密）
     * @param category 分类
     * @param url 网址（可选）
     * @param note 备注（可选）
     * @return 新密码的ID
     */
    suspend fun addPassword(
        title: String,
        username: String,
        plainPassword: String,
        category: String,
        url: String? = null,
        note: String? = null
    ): String {
        // 加密密码
        val encryptedPassword = encryptionManager.encrypt(plainPassword)
        
        val passwordEntity = PasswordEntity(
            title = title,
            username = username,
            encryptedPassword = encryptedPassword,
            category = category,
            url = url,
            note = note
        )
        
        passwordDao.insertPassword(passwordEntity)
        return passwordEntity.id
    }
    
    /**
     * 更新密码
     * @param passwordEntity 密码实体
     */
    suspend fun updatePassword(passwordEntity: PasswordEntity) {
        val updatedEntity = passwordEntity.copy(
            updatedAt = System.currentTimeMillis()
        )
        passwordDao.updatePassword(updatedEntity)
    }
    
    /**
     * 删除密码
     * @param passwordEntity 密码实体
     */
    suspend fun deletePassword(passwordEntity: PasswordEntity) {
        passwordDao.deletePassword(passwordEntity)
    }
    
    /**
     * 根据ID删除密码
     * @param id 密码ID
     */
    suspend fun deletePasswordById(id: String) {
        passwordDao.deletePasswordById(id)
    }
    
    /**
     * 解密密码
     * @param encryptedPassword 加密后的密码
     * @return 明文密码
     */
    fun decryptPassword(encryptedPassword: String): String {
        return encryptionManager.decrypt(encryptedPassword)
    }
    
    /**
     * 获取密码总数
     * @return 密码数量
     */
    suspend fun getPasswordCount(): Int {
        return passwordDao.getPasswordCount()
    }
}