# 咖啡笔记开发环境

## 开发环境
- Java: /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
- Android SDK: ~/Library/Android/sdk
- Gradle: ./gradlew (项目内)
- Node: ~/.npm-global/bin (v20.x)
- npm全局目录: ~/.npm-global/bin

## 项目结构
- 项目路径: /Users/wuxin/dev/coffee-notes
- 主要源码: app/src/main/java/com/coffeelab/coffeenotes/
- UI层: ui/screen/、ui/component/
- 数据层: data/entity/、data/dao/
- ViewModel: viewmodel/

## 常用命令
- 构建Debug: ./gradlew assembleDebug
- 构建Release: ./gradlew assembleRelease
- 清理: ./gradlew clean
- 运行测试: ./gradlew test

## 技术栈
- Kotlin + Jetpack Compose
- Room数据库
- Material Design 3
- Compose Navigation

## 注意事项
- 模拟器: Pixel 6 API 34（配置在项目里）
- API等级: minSdk 26, targetSdk 34