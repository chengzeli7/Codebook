package com.lcz.passwordmanager.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lcz.passwordmanager.data.local.dao.PasswordDao
import com.lcz.passwordmanager.data.local.entity.PasswordEntity

/**
 * 密码数据库 - Room实现
 * 
 * 数据库本体由 SQLCipher 加密（见 [com.lcz.passwordmanager.di.DatabaseModule]），
 * 因此关闭 Room 自带的 schema 导出。
 * 
 * @author lcz
 * @since 1.0.0
 */
@Database(
    entities = [PasswordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PasswordDatabase : RoomDatabase() {
    
    /**
     * 获取密码DAO
     */
    abstract fun passwordDao(): PasswordDao
}
