package com.lcz.passwordmanager

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * 仪器测试示例
 * 
 * @author lcz
 * @since 1.0.0
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    
    @Test
    fun useAppContext() {
        // 获取应用上下文
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.lcz.passwordmanager", appContext.packageName)
    }
}