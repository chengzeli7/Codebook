# 小梨密码 - 离线密码管理器

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="小梨密码" width="120"/>
</p>

<p align="center">
  <strong>完全离线的本地密码管理器，零网络传输 + AES-256加密 + 智能剪贴板捕获</strong>
</p>

<p align="center">
  <a href="#功能特性">功能特性</a> •
  <a href="#技术栈">技术栈</a> •
  <a href="#架构设计">架构设计</a> •
  <a href="#构建运行">构建运行</a> •
  <a href="#安全说明">安全说明</a>
</p>

---

## 📱 项目背景

小梨密码是一款专为注重隐私安全的用户设计的**完全离线**密码管理器。在数字化时代，密码安全至关重要，但许多密码管理器依赖云端同步，存在数据泄露风险。小梨密码采用"零网络传输"理念，所有数据仅存储于设备本地，通过AES-256加密保护，让用户完全掌控自己的密码数据。

### 核心卖点

- 🔒 **零网络传输** - 完全离线运行，无需网络权限
- 🛡️ **字段级加密** - AES-256-GCM加密敏感字段
- 👆 **生物识别** - 支持指纹/面部识别快速解锁
- 📋 **智能剪贴板** - 自动识别并保存账号密码
- 🚫 **防截屏保护** - 防止敏感信息被截屏/录屏
- 🎨 **极简设计** - 简约黑白灰配色，专注核心功能

---

## ✨ 功能特性

### 核心功能

| 功能模块 | 功能描述 | 优先级 |
|---------|---------|--------|
| 密码管理 | 密码的增删改查、分类管理、本地搜索 | P0 |
| 安全认证 | 生物识别认证、主密码保护、防截屏/录屏 | P0 |
| 智能剪贴板 | 自动检测剪贴板中的账号密码并提示保存 | P1 |
| 密码生成 | 一键生成高强度随机密码 | P1 |

### 分类管理

预设4个默认分类，不支持用户自定义（减少复杂度）：

- 🟦 **社交账号** - 微信、QQ、微博等
- 🟩 **金融银行** - 银行、支付宝、股票等
- 🟧 **工作相关** - 企业邮箱、OA系统等
- ⬜ **其他** - 其他各类账号

### 安全特性

| 安全项目 | 实现方式 |
|---------|---------|
| 字段加密 | 密码字段AES-256-GCM加密 |
| 密钥存储 | 主密钥存储于Android Keystore |
| 内存安全 | 密码字符串使用CharArray，使用后立即清零 |
| 剪贴板安全 | 复制密码后60秒自动清空系统剪贴板 |
| 防截屏 | 全局FLAG_SECURE，禁止系统截屏、录屏 |

---

## 🛠️ 技术栈

### 核心技术

- **Kotlin** - 主要开发语言
- **Jetpack Compose** - 现代声明式UI框架
- **Room** - 本地数据库ORM
- **Hilt** - 依赖注入框架
- **Biometric** - 生物识别认证

### 架构模式

- **MVVM** - Model-View-ViewModel架构
- **Repository模式** - 数据层抽象
- **依赖注入** - 通过Hilt实现

### 依赖库

```kotlin
// AndroidX核心
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

// Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:2.7.5")

// Room数据库
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Hilt依赖注入
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-compiler:2.48")

// 生物识别
implementation("androidx.biometric:biometric:1.1.0")

// 安全库
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

---

## 🏗️ 架构设计

### 项目结构

```
com.lcz.passwordmanager/
├── data/                          # 数据层
│   ├── local/                     # 本地数据源
│   │   ├── dao/                   # Room DAO
│   │   ├── database/              # 数据库配置
│   │   └── entity/                # 数据实体
│   └── repository/                # 仓库层
├── di/                            # 依赖注入
│   ├── DatabaseModule.kt          # 数据库模块
│   └── SecurityModule.kt          # 安全模块
├── domain/                        # 领域层
│   └── model/                     # 领域模型
├── security/                      # 安全层
│   ├── BiometricManager.kt        # 生物识别管理
│   ├── EncryptionManager.kt       # 加密管理
│   ├── MasterPasswordManager.kt   # 主密码管理
│   └── DatabaseKeyManager.kt      # 数据库密钥管理
├── service/                       # 服务层
│   └── ClipboardMonitor.kt        # 剪贴板监控
└── ui/                            # UI层
    ├── navigation/                # 导航
    ├── screens/                   # 页面
    ├── theme/                     # 主题
    └── viewmodel/                 # ViewModel
```

### 数据流架构

```
UI Layer (Compose)
    ↓
ViewModel (StateFlow)
    ↓
Repository (Business Logic)
    ↓
Data Layer (Room)
    ↓
Android Keystore (Key Management)
```

### 安全架构

```
用户输入
    ↓
生物识别 / 主密码验证
    ↓
从Keystore获取加密密钥
    ↓
AES-256-GCM解密密码字段
    ↓
展示给用户
```

---

## 🚀 构建运行

### 环境要求

- **Android Studio**: Hedgehog (2023.1.1) 或更高版本
- **JDK**: 17 或更高版本
- **Android SDK**: API 24 - API 34
- **Kotlin**: 1.9.20 或更高版本

### 构建步骤

1. **克隆项目**

```bash
git clone https://github.com/yourusername/xiaoli-password.git
cd xiaoli-password
```

2. **使用Android Studio打开**

   打开Android Studio，选择 `Open an existing project`，选择项目根目录。

3. **同步Gradle**

   点击 `Sync Now` 同步Gradle依赖。

4. **运行应用**

   连接设备或启动模拟器，点击 `Run` 按钮运行应用。

### 命令行构建

```bash
# 调试构建
./gradlew assembleDebug

# 发布构建
./gradlew assembleRelease

# 运行测试
./gradlew test  

# 运行仪器测试
./gradlew connectedAndroidTest
```

---

## 🧪 测试用例

### 单元测试

```bash
./gradlew test
```

测试覆盖：
- 加密/解密逻辑
- 密码验证逻辑
- 数据转换逻辑

### 仪器测试

```bash
./gradlew connectedAndroidTest
```

测试覆盖：
- 数据库操作
- 生物识别流程
- UI交互测试

### 安全测试

| 测试项 | 测试方法 | 预期结果 |
|-------|---------|---------|
| 截屏保护 | `adb shell screencap` | 黑屏/无法截屏 |
| 字段加密 | 导出数据库查看 | 密码字段为密文 |
| 内存安全 | Root环境下内存Dump | 无法搜索到明文密码 |

---

## 🔐 安全说明

### 数据安全

1. **完全离线**: 应用不请求任何网络权限，数据不会离开设备
2. **字段加密**: 密码字段使用AES-256-GCM加密
3. **硬件绑定**: 加密密钥存储于Android Keystore，与设备绑定
4. **内存保护**: 敏感数据使用CharArray，使用后立即清零

### 风险提示

⚠️ **重要提醒**:
- 数据仅保存在本机，卸载或清除数据将永久丢失
- 更换设备后，密码数据无法迁移（安全设计）
- 请牢记主密码，忘记后无法恢复

### 权限说明

| 权限 | 用途 | 是否必需 |
|-----|------|---------|
| `USE_BIOMETRIC` | 生物识别认证 | 否（可回退到主密码） |
| `FOREGROUND_SERVICE` | 剪贴板前台检测 | 否（可选功能） |

---

## 📋 已知问题与Roadmap

### 已知问题

1. **Android 10+剪贴板限制**: 后台应用无法读取剪贴板，仅在前台时检测
2. **生物识别误判**: 面部识别在弱光环境下识别率较低
3. **数据库加密**: 当前版本使用普通数据库，字段级加密保护密码数据

### Roadmap

- [ ] 密码强度检测
- [ ] 自动填充服务（Android Autofill）
- [ ] 导入/导出加密备份
- [ ] 深色模式优化
- [ ] 多语言支持
- [ ] 数据库整体加密（待SQLCipher 16KB兼容版本发布）

---

## 👨‍💻 作者与贡献者

### 作者

**lcz** - 项目创建者和主要开发者

### 贡献指南

欢迎提交Issue和Pull Request！

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

### 代码规范

- 遵循Kotlin官方代码规范
- 所有公共API必须添加KDoc注释
- 关键业务逻辑添加中文注释
- 提交前运行 `./gradlew ktlintCheck`

---

## 📄 许可证

本项目采用 [MIT许可证](LICENSE) 开源。

---

## 🙏 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代Android UI工具包
- [AndroidX Biometric](https://developer.android.com/jetpack/androidx/releases/biometric) - 生物识别库
- [AndroidX Security](https://developer.android.com/jetpack/androidx/releases/security) - 安全加密库

---

<p align="center">
  <strong>小梨密码 - 守护您的数字安全</strong>
</p>

<p align="center">
  Made with ❤️ by lcz
</p>