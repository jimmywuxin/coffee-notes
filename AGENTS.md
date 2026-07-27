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

## 已知技术债（审计 2026-07-27，详情见 `项目审计报告.md`）

> 已修的不再列。以下按「改到相关文件时顺手修」的原则，不急着集中发版。杨杨每次会话读到本文件，在相关改动时主动提醒。

### 性能（改到相关文件时顺手）
- `collectAsState` 未用 `collectAsStateWithLifecycle`（全项目）——后台仍采集 Flow，切 `lifecycle-runtime-compose` 已在依赖里
- `StatsScreen.kt:125,155,289,328,363,399,494,503`——每次重组新建 Flow 订阅，应 `remember(beanId)`
- `BrewListScreen.kt:330`——`beans.find{it.id==record.beanId}` 线性查找 O(n×m)，应预构建 `Map<Long,CoffeeBean>`
- `ImageUtils.kt:43`——`decodeStream` 无 `inSampleSize` 预采样，大图 OOM
- `CoffeeBeanDao.kt:37-53`——`searchBeansFull` 10 列 LIKE + 3 表 JOIN 全表扫描
- `CoffeeBeanDao.kt:86-92`——`getInventoryForActiveBeans` 每行两个相关子查询

### 代码质量（重构时顺手）
- `BeanViewModel.kt`（300+ 行）——God Class：豆子+标签+OCR+图片+赏味期全混，应拆分
- 6 个管理屏（Equipment/Grinder/ImpressionTag/ProcessMethod/RoastDegree/BrewMethod）——拖拽排序+删除弹窗逻辑复制粘贴，应抽可复用组件
- `BrewEditScreen.kt:164`——注释承诺"手改不覆盖 waterAmount"，代码无此判断，手改值被冲掉
- `AppDatabase.kt:176-234`——suspend 版 populate 无调用方，死代码
- `AppDatabase.kt:128`——`val now` 声明未使用
- `AppDatabaseMigrations.kt` MIGRATION_15_16——加 equipmentId/grinderId 未 DROP 旧 String 列

### 安全/构建（有空做）
- `AndroidManifest.xml:11`——`allowBackup=true`，adb 可导出明文 db
- `BackupUtil.kt:16-30`——备份明文无加密
- `proguard-rules.pro:14`——`keep data.entity.**` 过宽
- `app/build.gradle.kts:25-34`——release 未配 signingConfig（产出 unsigned）
- `gradle.properties`——未启用 `caching=true` / `parallel=true`

### 杂项
- `!!` 强解引用散布（BeanDetailScreen/HomeScreen/各管理屏删除弹窗）
- 魔法数字散落（`86400000L`、`15`天、`28`天、`1600`px、`60f` 模糊阈值、`200`ms debounce）
- UI 文案未走 `strings.xml`，不利于国际化
- 依赖可升级：CameraX 1.5.0、Coil 3.1.0、Kotlin 2.1.20 均有较新补丁
