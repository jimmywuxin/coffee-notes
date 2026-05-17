# Project Agent Instructions

This file provides guidance to AI agents (DeepSeek TUI, Claude Code, etc.) when working with code in this repository.

## 开发行为准则（必读）

**最重要：动手前必须先问**
- 发现问题先停下来分析原因，不要反复重试
- 打算怎么改、为什么这样改，先说清楚，等 Jimmy 确认后再动手
- 路径没有权限、工具调用失败，先停下来问，不要自己绕圈子找替代方案
- 遇到不确定的地方，宁可问清楚再继续，不要猜着干

**改完后必须说明**
- 改了哪些文件、哪几行、为什么改
- 如果改完编译/运行有问题，立即停下来说明，不要继续自行修复

**禁止行为**
- 不经确认就批量删除文件
- 不经确认就改版本号、版本名
- 多次尝试用不同方法做同一件事（一次不行就问）

## 项目概况

咖啡笔记 Android App（Kotlin + Jetpack Compose + Material 3）

- 包名：com.coffeelab.coffeenotes
- 当前版本：versionCode 38 / versionName 1.6.3
- minSdk 30 / targetSdk 34

## Build and Development Commands

```bash
# 进入项目目录
cd "/Volumes/mac mini outside/知识库/咖啡笔记项目源码"

# 设置 JAVA_HOME（必须）
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# 构建 Debug APK（手机测试用）
./gradlew assembleDebug

# 构建 Release APK（签名版，发 GitHub 用）
./gradlew assembleRelease

# 安装到手机（Debug）
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 安装到手机前先卸载（避免缓存问题）
adb uninstall com.coffeelab.coffeenotes && adb install -r app/build/outputs/apk/debug/app-debug.apk

# clean 后重新构建（versionCode 不刷新时用）
./gradlew clean assembleDebug
```

## 关键文件

| 文件 | 说明 |
|------|------|
| `app/build.gradle.kts` | 版本号（versionCode/versionName）在这里 |
| `app/src/main/java/com/coffeelab/coffeenotes/` | 主要源码 |
| `app/src/main/res/` | 资源文件（布局、图片、字符串等） |
| `app/schemas/` | Room 数据库 schema |
| `CHANGELOG.md` | 版本记录 |
| `README.md` | 项目说明 |

## 重要坑点

### R8 fullMode 与 Gson
- Gson 的 `TypeToken` 匿名内部类会被 R8 混淆破坏，导致 JSON 反序列化崩溃
- 如需保护，在 `app/proguard-rules.pro` 里加：
  ```
  -keepattributes Signature
  -keep class com.google.gson.reflect.TypeToken { *; }
  -keep class * extends com.google.gson.reflect.TypeToken
  ```

### 数据层
- 使用 Room 数据库，操作在 `data/local/` 目录下
- 备份格式为 JSON + ZIP（Gson 序列化）
- 备份文件名格式：`coffeenotes_backup_YYYYMMDD_HHMMSS.zip`

### UI 层
- 使用 Jetpack Compose + Material 3
- 主要屏幕：`ui/screen/`
- 通用组件：`ui/components/`

## Commit Messages

使用 conventional commits：
- `feat:` 新功能
- `fix:` 修复 bug
- `docs:` 文档
- `refactor:` 重构（不影响功能）
- `chore:` 构建/工具类修改

每次 commit 前确认改动内容，commit message 要简洁说明做了什么。