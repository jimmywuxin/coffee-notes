# ☕ 咖啡笔记 (Coffee Notes)

一款 Android 咖啡冲煮记录应用，帮你记录每一杯咖啡的风味。

## 功能

- ☕ **豆子管理** — 记录烘焙商、产地、品种、处理法，**支持最多6张图片**，支持收藏/归档
- 📝 **冲煮记录** — 记录粉量、水温、研磨度、**注水时长**、萃取时间等参数
- 📋 **冲煮手法** — 保存常用冲煮手法，一键填充参数
- 🔧 **器具管理** — 自定义器具列表（增删改拖拽排序）
- ⚙️ **磨豆机管理** — 自定义磨豆机列表（增删改拖拽排序）
- 🗂️ **归档/收藏** — 豆子支持归档与收藏管理
- 🏷️ **风味标签** — 预置 + 自定义风味标签
- 📊 **品鉴评分** — 酸度、甜感、苦味、口感、回甘五星评分
- 📸 **图片识别** — 本地 OCR 关键词识别（ML Kit 中文）+ MiniMax AI 智能识别
- 📊 **统计分析** — 全量统计 + 单豆统计，水温三段式分布，冲煮时段分析
- 💾 **备份/恢复** — ZIP 格式备份，兼容旧版本格式

## 技术栈

- **语言：** Kotlin
- **UI：** Jetpack Compose + Material 3
- **数据库：** Room
- **架构：** MVVM
- **OCR：** ML Kit（中文）
- **AI：** MiniMax-M2.7 API（图片识别）

## 构建

```bash
# 设置环境变量
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10
export ANDROID_HOME=~/android-sdk

# 编译
cd "/Volumes/mac mini outside/知识库/咖啡笔记项目源码"
./gradlew assembleDebug

# 安装到手机
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 版本

当前版本：**v1.6.2** (versionCode 37)

## 许可证

MIT License