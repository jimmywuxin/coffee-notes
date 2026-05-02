# ☕ 咖啡笔记 (Coffee Notes)

一款 Android 咖啡冲煮记录应用，帮你记录每一杯咖啡的风味。

## 功能

- ☕ **豆子管理** — 记录烘焙商、产地、品种、处理法等信息
- 📝 **冲煮记录** — 记录粉量、水温、研磨度、萃取时间等参数
- 📋 **冲煮配方** — 保存常用配方，一键填充参数
- 🏷️ **风味标签** — 预置 + 自定义风味标签
- 📊 **品鉴评分** — 酸度、甜感、苦味、口感、回甘五星评分
- 🔍 **拍照识别** — 本地关键词识别（ML Kit OCR）
- 🤖 **AI 识别** — 小米 MiMo Omni 智能识别（需 API Key）
- 📊 **统计分析** — 冲煮数据统计和风味雷达图
- 💾 **备份/恢复** — 数据导出导入

## 技术栈

- **语言：** Kotlin
- **UI：** Jetpack Compose + Material 3
- **数据库：** Room
- **架构：** MVVM
- **OCR：** ML Kit (中文)
- **AI：** 小米 MiMo Omni API

## 构建

```bash
# 设置环境变量
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10
export ANDROID_HOME=~/android-sdk

# 编译
./gradlew assembleDebug

# 安装到手机
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 许可证

MIT License
