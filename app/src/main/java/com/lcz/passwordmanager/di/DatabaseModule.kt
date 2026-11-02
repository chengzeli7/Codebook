package com.lcz.passwordmanager.di

import android.content.Context
import androidx.room.Room
import com.lcz.passwordmanager.data.local.dao.PasswordDao
import com.lcz.passwordmanager.data.local.database.PasswordDatabase
import com.lcz.passwordmanager.security.DatabaseKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 * 
 * 提供Room数据库和DAO的依赖注入
 * 
 * @author lcz
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "passwords.db"

    /** SQLCipher 密钥字节的临时使用后清零，避免明文密钥常驻内存 */
    private fun ByteArray.wipe(): ByteArray {
        fill(0)
        return this
    }

    init {
        // 加载 SQLCipher 原生库（每个进程只需一次）
        System.loadLibrary("sqlcipher")
    }

    /**
     * 提供数据库实例
     * 
     * 使用 SQLCipher 加密数据库文件，密钥由 [DatabaseKeyManager] 生成并保存；
     * 这样密码等敏感数据落盘时也是密文，而不仅是依赖 SQLite 的明文文件权限。
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyManager: DatabaseKeyManager
    ): PasswordDatabase {
        val passphrase = keyManager.getOrCreateDatabaseKey().toByteArray()
        return try {
            Room.databaseBuilder(
                context.applicationContext,
                PasswordDatabase::class.java,
                DATABASE_NAME
            ).openHelperFactory(SupportOpenHelperFactory(passphrase))
                .build()
        } finally {
            passphrase.wipe()
        }
    }

    /**
     * 提供密码DAO
     */
    @Provides
    @Singleton
    fun providePasswordDao(database: PasswordDatabase): PasswordDao {
        return database.passwordDao()
    }
}