# 咖啡笔记开发环境

## 🚨 装机铁律（2026-08-02 两次事故 + 2026-08-15 一次事故，务必遵守）
- **数据库升级不需要卸载重装**：Room 自带 migration 机制（`AppDatabaseMigrations`），升级 APK 用 `adb install -r` 覆盖即可，数据自动迁移保留。2026-08-02 的 v20→v21 迁移崩溃是迁移 SQL 写错（note 列 DEFAULT 与 schema 不符），修好 SQL 后 `install -r` 就正常升级了，数据没丢
- **`adb uninstall` / `adb shell pm clear` / `adb shell pm reset` 是清空 app 私有数据（Room 数据库 + 豆子照片）的危险操作，无 root 不可恢复**。2026-08-02 两次犯同类错误（一次 uninstall、一次 pm clear），用户 7/27→8/2 数据反复丢失
- **`./gradlew connectedDebugAndroidTest` 测试跑完会自动卸载 app、清空私有数据（AGP 默认行为）！2026-08-15 跑 MigrationTest 时中招，app 被卸载，靠用户 8-14 21:48 的备份才救回**。跑 androidTest 禁止直接用 connected 任务，必须走手动流程：
  ```bash
  ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
  adb shell am instrument -w com.coffeelab.coffeenotes.test/androidx.test.runner.AndroidJUnitRunner
  ```
  （`am instrument` 跑完不卸载 app；MigrationTest 用的是独立 migration-test 库，不碰真实数据）
- 装机默认 `adb install -r`（保留数据）；迁移崩溃排查先看 logcat（Migration didn't properly handle / Expected vs Found 对比），改 SQL 后 `install -r` 重装，绝不先动数据
- **确需卸载/清库（如迁移不可修复、测试空库）：第一步必须先让用户备份（app 内 设置→备份与恢复 → 导出到 /sdcard），或提醒用户备份并得到确认，确认备份文件存在后再动手**；否则禁止任何清库操作
- 操作顺序永远是：备份 → 确认 → 才可清库；备份失败或不确定 = 不清库

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

## 已知技术债（审计 2026-07-27，更新 2026-08-14，详情见 `项目审计报告.md` 与 `改进路线图.md`）

> 已修的不再列（本次已移除：BrewEditScreen 手改 waterAmount、AppDatabase `val now`、依赖升级——CameraX 1.5.0/Coil 3.1.0/Kotlin 2.1.20 均已到位）。以下按「改到相关文件时顺手修」的原则，不急着集中发版。杨杨每次会话读到本文件，在相关改动时主动提醒。

### 数据安全（最优先）
- ~~无 Room Migration 全链路测试~~ **已做（2026-08-15）**：`app/src/androidTest/.../data/MigrationTest.kt` 落地，20→21 迁移测试通过；首次运行即发现并修复 **MIGRATION_20_21 多写 FOREIGN KEY 子句**（实体未声明 FK，schema 校验失败）。注意：schemas 仅 20/21.json，1→19 各段仍无法覆盖，后续补早期 schema JSON 再扩
- 备份仅限本地 ZIP 导出，无云端通道——换机/丢手机即全丢，建议 WebDAV/飞书云盘自动备份（详见改进路线图 1.2）
- 已拍板（2026-08-14）：备份保持**明文 ZIP** + `allowBackup=true`，数据不敏感，明文方便导出查看，**不要再建议加密**
- **跑 androidTest 千万别用 `connectedDebugAndroidTest`（自动卸载 app），用 AGENTS.md 顶部的手动 am instrument 流程**——2026-08-15 事故实录，见装机铁律

### 测试/工程化（有空做）
- `BackupViewModel.kt`（661 行）——纯逻辑最复杂却零测试，补「备份→恢复→数据一致」往返测试
- 无 GitHub Actions CI——push 跑 test+assembleDebug、打 tag 自动出 release APK 挂 Releases（详见改进路线图 2.3）
- `README.md` 版本号滞后（写 2.6.1，实际 2.9.3）

### 性能（改到相关文件时顺手）
- ~~collectAsState 未用 collectAsStateWithLifecycle~~ **已做（2026-08-15）**：全项目 17 个 screen 文件 ~70 处替换完成，无参调用补 `initialValue = flow.value`；gradle.properties 已开 caching+parallel（2.2）
- ~~StatsScreen 每次重组新建 Flow 订阅~~ **已做（2026-08-15）**：getBrewCountForBean/getMonthlyBrewCountsForBean/getMonthlyConsumptionForBean/getTopFlavorTags 均包 remember
- `BrewListScreen.kt:330`——`beans.find{it.id==record.beanId}` 线性查找 O(n×m)，应预构建 `Map<Long,CoffeeBean>`
- `ImageUtils.kt:43`——`decodeStream` 无 `inSampleSize` 预采样，大图 OOM
- `CoffeeBeanDao.kt:37-53`——`searchBeansFull` 10 列 LIKE + 3 表 JOIN 全表扫描
- `CoffeeBeanDao.kt:86-92`——`getInventoryForActiveBeans` 每行两个相关子查询

### 代码质量（重构时顺手）
- `BeanViewModel.kt`（309 行）——God Class：豆子+标签+OCR+图片+赏味期全混，应拆分
- 6 个管理屏（Equipment/Grinder/ImpressionTag/ProcessMethod/RoastDegree/BrewMethod）——拖拽排序+删除弹窗逻辑复制粘贴，应抽可复用组件
- `AppDatabase.kt:178-202`——suspend 版 populate（Equipment/Grinders/BrewMethods）无调用方，死代码（callback 里用的是 Sync 版）
- `AppDatabaseMigrations.kt:185` MIGRATION_15_16——加 equipmentId/grinderId 未 DROP 旧 String 列

### 安全/构建（有空做）
- `proguard-rules.pro:14`——`keep data.entity.**` 过宽
- `app/build.gradle.kts:25-34`——release 未配 signingConfig（产出 unsigned）
- `gradle.properties`——未启用 `caching=true` / `parallel=true`

### 杂项
- `!!` 强解引用散布（BeanDetailScreen/HomeScreen/各管理屏删除弹窗）
- 魔法数字散落（`86400000L`、`15`天、`28`天、`1600`px、`60f` 模糊阈值、`200`ms debounce）
- UI 文案硬编码约 873 处未走 `strings.xml`，不利于国际化（工程量最大，优先级垫底）
