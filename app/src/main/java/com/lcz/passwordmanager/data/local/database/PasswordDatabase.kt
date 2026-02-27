package com.lcz.passwordmanager.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lcz.passwordmanager.data.local.dao.PasswordDao
import com.lcz.passwordmanager.data.local.entity.PasswordEntity

/**
 * 密码数据库 - Room实现
 * 
 * @author lcz
 * @since 1.0.0
 */
@Database(
    entities = [PasswordEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PasswordDatabase : RoomDatabase() {
    
    /**
     * 获取密码DAO
     */
    abstract fun passwordDao(): PasswordDao
}