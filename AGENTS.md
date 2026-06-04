# 咖啡笔记开发环境

## 开发环境
- Java: Homebrew openjdk 21.0.11（`/usr/bin/java`），`/usr/libexec/java_home` 不可用
  - Gradle 用 21 跑，产物字节码降到 17（`app/build.gradle.kts`: sourceCompatibility/targetCompatibility = 17, jvmTarget = "17"）
- Android SDK: /opt/homebrew/share/android-commandlinetools（见 `local.properties` 中的 `sdk.dir`）
  - 注意：`~/Library/Android/sdk` 在本机不存在，别再引用
- Gradle: ./gradlew（项目内，已固定 8.14.1）
- Node: v24.16.0（`/usr/local/bin/node`）
- npm 全局目录: /Users/wuxin/.npm-global（已装 lark-cli / mmx / openclaw 等）
- adb: /opt/homebrew/share/android-commandlinetools/platform-tools/adb

## 项目结构
- 项目路径: /Users/wuxin/dev/coffee-notes
- 主要源码: app/src/main/java/com/coffeelab/coffeenotes/
- 顶层包: data / ui / util / viewmodel（外加 MainActivity.kt）
- UI 层: ui/screen/、ui/component/、ui/navigation/、ui/theme/
- 数据层: data/entity/、data/dao/、data/repository/，外加 AppDatabase.kt / AppDatabaseMigrations.kt / Converters.kt
- ViewModel: viewmodel/
- 工具: util/（含 ImageUtils / BackupUtil / OCRProcessor / engine/）
- 当前分支: main

## 常用命令
- 构建 Debug: ./gradlew assembleDebug
- 构建 Release: ./gradlew assembleRelease（当前未配 signingConfig，产出 `app-release-unsigned.apk`，需自己签名）
- 清理: ./gradlew clean
- 运行测试: ./gradlew test
- 设备列表: /opt/homebrew/share/android-commandlinetools/platform-tools/adb devices -l

## 技术栈
- Kotlin + Jetpack Compose（BOM 2025.05.01）
- Room 数据库（2.7.1，注解处理走 KSP）
- Material Design 3
- Compose Navigation（2.9.0）
- ML Kit Text Recognition（含中文识别）
- CameraX、Coil 3

## 注意事项
- SDK 版本（见 `app/build.gradle.kts`）: compileSdk 35, minSdk 30, targetSdk 35
- 本机当前没有 AVD（`~/Library/Android/sdk` 不存在，cmdline-tools 在 homebrew 路径下）
- adb daemon 在 sandbox 里跑不起来，需要在主机环境执行（用 `require_escalated` 模式通过）
