package com.lcz.passwordmanager.di

import android.content.Context
import com.lcz.passwordmanager.data.local.dao.PasswordDao
import com.lcz.passwordmanager.data.local.database.PasswordDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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

    /**
     * 提供数据库实例
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PasswordDatabase {
        return androidx.room.Room.databaseBuilder(
            context.applicationContext,
            PasswordDatabase::class.java,
            DATABASE_NAME
        ).build()
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