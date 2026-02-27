# ProGuard规则

# 保留Compose相关
-keep class androidx.compose.** { *; }
-keep class androidx.compose.material3.** { *; }

# 保留Room相关
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# 保留Hilt相关
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent

# 保留SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# 保留序列化
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# 保留Kotlin元数据
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
