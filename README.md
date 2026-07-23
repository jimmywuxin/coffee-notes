# ☕ 咖啡笔记 (Coffee Notes)

一款 Android 咖啡冲煮记录应用，帮你记录每一杯咖啡的风味。

---

## 功能特点

### 🫘 豆子管理
- 记录烘焙商、产地、产区、庄园、品种、处理法、烘焙度等完整信息
- 处理法、烘焙度支持**下拉选择预设 + 自定义输入**
- 支持最多 6 张豆子照片，网格展示、点击放大
- 支持收藏、归档，**收藏筛选状态跨页持久化**
- 列表拖动排序，多选批量删除
- 养豆期/赏味期自动计算（根据烘焙度配置），截止日期一目了然
- 官方萃取建议：粉量、粉水比、水温、注水时长、萃取时长
- **OCR 图片识别**：拍豆袋自动提取烘焙信息（ML Kit 中文 OCR）
- 基础信息紧凑 `·` 分隔展示，缺字段自动跳过

### ☕ 冲煮记录
- 记录粉量、水温、研磨度、注水时长、萃取时间等参数
- 酸度、甜感、苦味、口感、回甘五星品鉴评分
- 内置**双阶段冲煮计时器**，一键填入注水/萃取时长
- 支持冰冲（加冰量及 bypass 注水量）
- 风味雷达图（有评分记录时自动生成）

### 📋 冲煮手法
- 保存常用冲煮手法（器具、研磨度、水温、粉水比等）
- 一键填充所有参数到冲煮记录

### 🏷️ 风味标签
- 预置常用风味词 + 自由输入自定义
- 豆子详情页直观展示

### 🔧 器具 & 磨豆机管理
- 自定义器具/磨豆机列表，支持增删改、拖拽排序
- 以**外键 ID 关联**冲煮记录，改名/删除不丢数据

### 📊 统计分析
- 全量统计 + 单豆统计
- 水温三段式分布、冲煮时段分析、评分趋势

### 💾 数据管理
- ZIP 格式备份/恢复，兼容旧版本格式
- 烘焙度、处理法、养豆/赏味配置、购买记录全部纳入备份
- 一键清空所有数据

---

## 技术栈

- **语言：** Kotlin
- **UI：** Jetpack Compose + Material Design 3
- **数据库：** Room（KSP 注解处理）
- **架构：** MVVM（ViewModel + Repository + Flow）
- **导航：** Compose Navigation
- **OCR：** ML Kit Text Recognition（中文 + 拉丁文双模型）
- **图片加载：** Coil 3
- **序列化：** Gson
- **构建：** Gradle 8.14.1 + Android Gradle Plugin 8.10.0，JDK 21（字节码目标 17）

### 项目结构

```
app/src/main/java/com/coffeelab/coffeenotes/
├── data/
│   ├── entity/        # Room @Entity 数据表定义
│   ├── dao/           # Room @Dao 数据访问接口
│   ├── repository/    # CoffeeRepository：数据访问统一入口
│   ├── AppDatabase.kt # 数据库定义 + 版本
│   └── AppDatabaseMigrations.kt  # 数据库迁移脚本
├── ui/
│   ├── screen/        # 各页面 Composable
│   ├── component/     # 复用组件（含 DraggableItem 拖拽组件）
│   ├── navigation/    # 路由定义
│   └── theme/         # 主题/配色（日系原木风）
├── viewmodel/         # 各业务 ViewModel
├── util/              # 工具（ImageUtils / BackupUtil / OCRProcessor / engine/）
└── MainActivity.kt
```

---

## 安装步骤

### 1. 环境要求

| 工具 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 21（Homebrew `openjdk@21`） | 字节码目标为 17 |
| Android SDK | 35（compileSdk / targetSdk） | minSdk 30 |
| Gradle | 8.14.1 | 项目内置 `./gradlew`，无需单独安装 |
| 设备 | Android 11（API 30）及以上 | 用于安装调试包 |

### 2. 准备 Android SDK 路径

`local.properties` 已指向本机 SDK 目录：

```properties
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

若在其他机器开发，请修改该文件指向你的 `android-sdk` 目录，或设置环境变量：

```bash
export ANDROID_HOME=/path/to/android-sdk
```

### 3. 配置 JDK

建议使用 Homebrew 的 `openjdk@21`，设置 `JAVA_HOME`：

```bash
# macOS (Homebrew)
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
```

> 注：本机 `/usr/libexec/java_home` 不可用，请勿依赖它；直接把 `JAVA_HOME` 指向 brew 的 openjdk@21 目录即可。

### 4. 获取源码

```bash
git clone https://github.com/jimmywuxin/coffee-notes.git
cd coffee-notes
```

### 5. 编译 Debug 包

```bash
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

如需清理后重新构建：

```bash
./gradlew clean assembleDebug
```

### 6. 安装到手机

确保手机已开启「开发者选项 → USB 调试」并连接电脑：

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

无实体机时可连接第三方模拟器，或用 Android Studio 的 AVD（本仓库开发机未预装 AVD）。

### 7. 运行单元测试

核心 OCR/字典/日期解析逻辑为纯 Kotlin 实现，可在 JVM 直接测试：

```bash
./gradlew test
```

---

## 使用示例

### 应用使用流程

1. **添加豆子**：在「豆子」页点击 `+`，手动填写或点击「从相册选择图片识别」拍下豆袋，ML Kit 自动抽取烘焙商、产地、品种、烘焙日期等字段（识别错误会被记录，下次自动纠正）。
2. **记录冲煮**：在「冲煮」页新建记录，选择豆子与器具/磨豆机，填入粉量、水温、研磨度；可用内置**双阶段计时器**一键填入注水/萃取时长，并打五星品鉴评分。
3. **查看分析**：在「设置 → 统计总览」查看水温分布、冲煮时段、评分趋势；进入单豆详情可看该豆专属统计与库存余量。
4. **备份数据**：在「设置 → 备份与恢复」导出 ZIP 备份，换机或重装后可一键恢复（含豆子、冲煮、配置、购买记录等全量数据）。

### 开发示例：数据如何流动（MVVM + Room）

新增一个数据功能时，通常沿「Entity → DAO → Repository → ViewModel → Screen」五步扩展。下面是真实的豆子数据流片段：

**① 定义数据表（`data/entity/CoffeeBean.kt`）**

```kotlin
@Entity(tableName = "coffee_beans")
data class CoffeeBean(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roaster: String = "",
    val name: String = "",
    val roastDate: Long? = null,
    val isFavorite: Boolean = false,
    // …其余字段省略
)
```

**② 声明数据访问（`data/dao/CoffeeBeanDao.kt`）**

```kotlin
@Dao
interface CoffeeBeanDao {
    @Query("SELECT * FROM coffee_beans WHERE isArchived = 0 ORDER BY sortOrder ASC")
    fun getActiveBeans(): Flow<List<CoffeeBean>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bean: CoffeeBean): Long

    @Update
    suspend fun update(bean: CoffeeBean)
}
```

**③ 汇聚到 Repository（`data/repository/CoffeeRepository.kt`）**

```kotlin
class CoffeeRepository(private val db: AppDatabase) {
    val activeBeans: Flow<List<CoffeeBean>> = db.coffeeBeanDao().getActiveBeans()
    suspend fun insertBean(bean: CoffeeBean): Long = db.coffeeBeanDao().insert(bean)
}
```

**④ 在 ViewModel 暴露状态（`viewmodel/BeanViewModel.kt`）**

```kotlin
class BeanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val activeBeans = repository.activeBeans

    fun toggleFavorite(bean: CoffeeBean) {
        viewModelScope.launch {
            repository.updateBean(bean.copy(isFavorite = !bean.isFavorite))
        }
    }
}
```

**⑤ 在 Compose 页面消费**

```kotlin
val beans by viewModel.activeBeans.collectAsState(initial = emptyList())
LazyColumn {
    items(beans) { bean ->
        BeanCard(bean, onFavoriteClick = { viewModel.toggleFavorite(bean) })
    }
}
```

> 关键约定：DAO 返回 `Flow` 实现响应式刷新；写操作均为 `suspend` 并在 `viewModelScope.launch` 中调用；数据库变更必须配套迁移脚本（见 `AppDatabaseMigrations.kt`）。

---

## 贡献指南

欢迎参与改进咖啡笔记！请遵循以下流程：

### 分支与提交

- 主分支为 `main`，请勿直接在主分支上做大规模改动，建议从 `main` 切出 `feature/xxx` 分支开发。
- 提交信息清晰描述改动，参考风格：
  ```
  v2.6.1: 修复冲煮手法编辑后排序置顶

  - 保存时保留原始 sortOrder 与 updatedAt
  - 新建仍走原排序逻辑
  ```

### 代码规范

- 语言统一 **Kotlin**，遵循官方 [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)。
- UI 使用 **Jetpack Compose**，优先用 `Material 3` 组件，复用 `ui/component/` 下已有组件（如 `DraggableItem`）。
- 数据库操作放在 **Repository**，不要在 ViewModel/UI 中直接拼 SQL。
- 新增/修改数据表时，必须：
  1. 在 `AppDatabase.kt` 提升 `databaseVersion`；
  2. 在 `AppDatabaseMigrations.kt` 提供对应的 `MIGRATION_x_y`，保证旧用户平滑升级；
  3. 涉及外键/字段重命名时注意兼容历史备份格式。

### 测试

- 纯逻辑（OCR 抽取、字典匹配、日期解析等）保持不依赖 Android framework，便于在 JVM 单测。
- 提交前运行 `./gradlew test` 确保测试通过。

### Issue 与 PR

- 提 Bug 请附：**系统版本 / 应用版本（设置 → 关于）**、复现步骤、期望与实际表现、相关截图或日志。
- 功能建议请说明使用场景与预期交互。
- PR 请聚焦单一主题，描述「做了什么 / 为什么 / 如何验证」。

---

## 版本

当前版本：**2.6.1** (versionCode 76)

完整更新历史见 [CHANGELOG.md](./CHANGELOG.md)。

## 许可证

[MIT License](./LICENSE)
