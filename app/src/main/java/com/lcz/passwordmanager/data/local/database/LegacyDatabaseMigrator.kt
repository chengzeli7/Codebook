package com.lcz.passwordmanager.data.local.database

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.lcz.passwordmanager.security.DatabaseKeyManager
import net.zetetic.database.sqlcipher.SQLiteDatabase as SqlCipherDatabase
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 旧版本明文数据库迁移器
 *
 * 旧版本虽然声明了 SQLCipher 依赖，但打开数据库时从未使用加密，`passwords.db` 是普通 SQLite 文件。
 * 新版本统一用 SQLCipher 打开同一路径，因此必须先识别旧库并转换为加密库，
 * 否则用户会丢失全部已保存密码。
 *
 * 流程（任一步失败都保留原文件，并回退为明文模式，宁可先用不可恢复的数据也不丢数据）：
 * 1. 文件头为 `SQLite format 3` 即未加密库（SQLCipher 库文件头是随机盐，不会命中）；
 * 2. 用普通 SQLite 打开并执行 SQLCipher 官方迁移写法：ATTACH 加密库 + `sqlcipher_export('encrypted')`；
 * 3. 用新密钥打开临时库读取 passwords 校验，成功后原子替换旧库文件。
 */
@Singleton
class LegacyDatabaseMigrator @Inject constructor(
    private val keyManager: DatabaseKeyManager
) {

    companion object {
        private const val TAG = "LegacyDatabaseMigrator"

        /** 普通 SQLite 文件头（SQLCipher 加密后不会出现） */
        private val PLAIN_SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

        private const val TMP_SUFFIX = ".encrypting"
        private const val BAK_SUFFIX = ".bak"
    }

    /**
     * 在 Room 打开数据库之前调用。
     *
     * @param databaseFile 目标数据库文件
     * @return true 表示数据库现已加密（已迁移或本来就是 SQLCipher 库）；false 表示仍是明文库
     */
    fun migrateIfNeeded(databaseFile: File): Boolean {
        if (!databaseFile.exists()) return true
        if (!isPlainSqlite(databaseFile)) return true

        Log.i(TAG, "检测到旧版明文数据库，开始迁移")
        val tmpFile = File(databaseFile.parentFile, databaseFile.name + TMP_SUFFIX)
        tmpFile.delete()
        val passphrase = keyManager.getOrCreateDatabaseKey()

        return try {
            if (!exportToEncrypted(databaseFile, tmpFile, passphrase)) {
                tmpFile.delete()
                Log.e(TAG, "旧库导出失败，保留原明文数据库")
                return false
            }
            validateEncrypted(tmpFile, passphrase)
            val replaced = replaceAtomically(databaseFile, tmpFile)
            if (!replaced) {
                Log.e(TAG, "加密库替换失败，保留原明文数据库")
            }
            replaced
        } catch (e: Exception) {
            tmpFile.delete()
            Log.e(TAG, "迁移失败，保留原明文数据库并回退为明文模式", e)
            false
        }
    }

    private fun exportToEncrypted(source: File, target: File, passphrase: String): Boolean {
        val escapedKey = passphrase.replace("'", "''")
        // 用 SQLCipher 库的“无密码”入口，以普通 SQLite 方式打开旧库
        val plainDb = SqlCipherDatabase.openDatabase(
            source.path,
            null as SqlCipherDatabase.CursorFactory?,
            SQLiteDatabase.OPEN_READONLY
        )
        try {
            plainDb.rawExecSQL(
                "ATTACH DATABASE '${target.path.replace("'", "''")}' AS encrypted KEY '$escapedKey'"
            )
            plainDb.rawExecSQL("SELECT sqlcipher_export('encrypted')")
            plainDb.rawExecSQL("DETACH DATABASE encrypted")
        } finally {
            plainDb.close()
        }
        return target.exists()
    }

    private fun validateEncrypted(file: File, passphrase: String) {
        val db = SqlCipherDatabase.openDatabase(
            file.path,
            passphrase,
            null,
            SQLiteDatabase.OPEN_READONLY,
            null
        )
        try {
            db.rawQuery("SELECT COUNT(*) FROM passwords", null).use { cursor ->
                check(cursor.moveToFirst())
            }
        } finally {
            db.close()
        }
    }

    private fun replaceAtomically(target: File, tmp: File): Boolean {
        val backup = File(target.path + BAK_SUFFIX)
        backup.delete()
        if (!target.renameTo(backup)) return false
        if (!tmp.renameTo(target)) {
            // 回滚：恢复原库，保证用户数据仍可用
            backup.renameTo(target)
            tmp.delete()
            return false
        }
        backup.delete()
        target.parentFile?.listFiles()
            ?.filter { it.name.startsWith(target.name) && it.name.endsWith("-journal") }
            ?.forEach { it.delete() }
        return true
    }

    /**
     * 判断文件是否为未加密的普通 SQLite 库。
     */
    fun isPlainSqlite(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(PLAIN_SQLITE_HEADER.size)
                input.read(header) == header.size && MessageDigest.isEqual(header, PLAIN_SQLITE_HEADER)
            }
        } catch (e: Exception) {
            false
        }
    }
}
