package com.lcz.passwordmanager.di

import android.content.Context
import com.lcz.passwordmanager.security.BiometricAuthManager
import com.lcz.passwordmanager.security.DatabaseKeyManager
import com.lcz.passwordmanager.security.EncryptionManager
import com.lcz.passwordmanager.security.MasterPasswordManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 安全模块依赖注入
 * 
 * 提供加密、生物识别、主密码管理等安全相关组件
 * 
 * @author lcz
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideEncryptionManager(): EncryptionManager {
        return EncryptionManager()
    }

    @Provides
    @Singleton
    fun provideMasterPasswordManager(
        @ApplicationContext context: Context
    ): MasterPasswordManager {
        return MasterPasswordManager(context)
    }

    @Provides
    @Singleton
    fun provideBiometricAuthManager(
        @ApplicationContext context: Context
    ): BiometricAuthManager {
        return BiometricAuthManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabaseKeyManager(
        @ApplicationContext context: Context
    ): DatabaseKeyManager {
        return DatabaseKeyManager(context)
    }
}