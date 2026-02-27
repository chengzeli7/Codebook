package com.lcz.passwordmanager.data.local.dao

import androidx.room.*
import com.lcz.passwordmanager.data.local.entity.PasswordEntity
import com.lcz.passwordmanager.data.local.model.CategoryCount
import kotlinx.coroutines.flow.Flow

/**
 * 密码数据访问对象（DAO）
 * 
 * 提供密码数据的增删改查操作
 * 所有操作均为协程挂起函数，支持Flow响应式数据流
 * 
 * @author lcz
 * @since 1.0.0
 */
@Dao
interface PasswordDao {
    
    /**
     * 获取所有密码条目（按创建时间倒序）
     * @return 密码列表Flow，数据变化时自动更新
     */
    @Query("SELECT * FROM passwords ORDER BY created_at DESC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>
    
    /**
     * 根据分类筛选密码
     * @param category 分类名称
     * @return 该分类下的密码列表Flow
     */
    @Query("SELECT * FROM passwords WHERE category = :category ORDER BY created_at DESC")
    fun getPasswordsByCategory(category: String): Flow<List<PasswordEntity>>
    
    /**
     * 模糊搜索密码
     * 支持搜索标题、用户名、URL
     * 
     * @param query 搜索关键词
     * @return 匹配的密码列表Flow
     */
    @Query("""
        SELECT * FROM passwords 
        WHERE title LIKE '%' || :query || '%' 
           OR username LIKE '%' || :query || '%' 
           OR url LIKE '%' || :query || '%'
        ORDER BY 
            CASE 
                WHEN title LIKE :query || '%' THEN 0
                WHEN title LIKE '%' || :query || '%' THEN 1
                ELSE 2
            END,
            created_at DESC
    """)
    fun searchPasswords(query: String): Flow<List<PasswordEntity>>
    
    /**
     * 根据ID获取单个密码
     * @param id 密码条目ID
     * @return 密码实体，不存在时返回null
     */
    @Query("SELECT * FROM passwords WHERE id = :id LIMIT 1")
    suspend fun getPasswordById(id: String): PasswordEntity?
    
    /**
     * 插入新密码
     * @param password 密码实体
     * @return 插入的行ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntity): Long
    
    /**
     * 更新密码信息
     * @param password 密码实体（必须包含有效ID）
     */
    @Update
    suspend fun updatePassword(password: PasswordEntity)
    
    /**
     * 删除指定密码
     * @param password 密码实体
     */
    @Delete
    suspend fun deletePassword(password: PasswordEntity)
    
    /**
     * 根据ID删除密码
     * @param id 密码条目ID
     */
    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deletePasswordById(id: String)
    
    /**
     * 获取密码总数
     * @return 密码条目数量
     */
    @Query("SELECT COUNT(*) FROM passwords")
    suspend fun getPasswordCount(): Int
    
    /**
     * 获取各分类的密码数量统计
     * @return 分类名称和数量的映射
     */
    @Query("SELECT category, COUNT(*) as count FROM passwords GROUP BY category")
// 必须返回 DTO 列表，Room 才能正确映射查询到的两列
    fun getCategoryCounts(): Flow<List<CategoryCount>>
}