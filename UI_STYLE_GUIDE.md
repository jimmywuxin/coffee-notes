# 咖啡笔记 · 日系治愈原木风 UI 规范

> 风格定位：日系极简治愈感，原木咖啡馆氛围。干净温柔、低对比度、不刺眼。适合长期日常使用。

---

## 1. 配色体系

### 主色调（暖色系）
| 用途 | 色值 | 说明 |
|------|------|------|
| 页面背景 | `#FAF8F5` | 暖米白，大面积使用 |
| 卡片背景 | `#FFFFFF` | 纯白卡片，与背景区分 |
| 卡片背景（次级） | `#F5F1EB` | 浅燕麦，用于分组背景 |
| 原木主色 | `#C4A882` | 原木浅棕，用于主按钮、高亮 |
| 原木深色 | `#A68B5B` | 深一档，用于悬停/按下态 |

### 辅助色
| 用途 | 色值 | 说明 |
|------|------|------|
| 莫兰迪浅茶绿 | `#A8B5A0` | 次要标签、图标、装饰 |
| 雾灰 | `#B8B4AF` | 禁用态、次要文字、占位符 |
| 分割线/边框 | `#E8E4DE` | 微暖灰，用于极淡边框 |

### 强调色
| 用途 | 色值 | 说明 |
|------|------|------|
| 浅焦糖奶茶 | `#D4A574` | 评分星星、强调元素、链接 |
| 焦糖深色 | `#C08B5C` | 按钮悬停态 |

### 文字色
| 用途 | 色值 | 说明 |
|------|------|------|
| 主文字 | `#4A4A4A` | 正文、标题，深暖灰，非纯黑 |
| 次要文字 | `#8A8A8A` | 副标题、说明文字 |
| 占位符文字 | `#C0BCB8` | 空状态、hint 文字 |

---

## 2. 圆角规范

**统一圆角：18px**

适用于：
- 所有卡片：`Card`、`Surface` → `shape = RoundedCornerShape(18.dp)`
- 所有按钮：`Button` → `shape = RoundedCornerShape(18.dp)`
- 输入框：`TextField` → `shape = RoundedCornerShape(18.dp)`
- 芯片：`AssistChip`、`FilterChip` → `shape = RoundedCornerShape(18.dp)`
- 图片圆角：`Modifier.clip(RoundedCornerShape(18.dp))`
- 对话框：`AlertDialog` → `shape = RoundedCornerShape(24.dp)`

---

## 3. 间距规范

| 场景 | 间距值 | 说明 |
|------|--------|------|
| 页面水平留白 | `20dp` | 页面两侧留白，日系大留白感 |
| 卡片内边距 | `16dp` | 卡片内部元素间距 |
| 卡片间距 | `14dp` | 相邻卡片之间 |
| 分组间距 | `24dp` | 不同区块之间 |
| 元素间距 | `10dp~12dp` | 同行元素之间 |
| 页面顶部安全区 | `16dp` | 内容与 TopAppBar 之间 |

**禁止使用：**
- ❌ 硬线分割（`HorizontalDivider` 除非在危险操作区）
- ❌ 高饱和色块背景
- ❌ 过于紧凑的布局

---

## 4. 字体规范

### 字体方案
- **主字体：** 系统默认（Roboto）+ 日系圆润感通过字重控制
- **字重分布：**
  - 标题：`FontWeight.SemiBold`（600）
  - 正文：`FontWeight.Normal`（400）
  - 说明文字：`FontWeight.Light`（300）

### 字号规范
| 场景 | 字号 | 字重 |
|------|------|------|
| 页面大标题 | `22sp` | SemiBold |
| 卡片标题 | `18sp` | SemiBold |
| 分组标签 | `13sp` | Medium |
| 正文 | `15sp` | Normal |
| 说明文字 | `13sp` | Normal |
| 最小文字 | `12sp` | Normal |

### 行高
- 正文行高：`1.7`（宽松日系感）
- 标题行高：`1.4`

---

## 5. 阴影规范

**轻柔和哑光阴影（无厚重投影）：**

```kotlin
// 卡片阴影
Modifier.shadow(
    elevation = 2.dp,
    shape = RoundedCornerShape(18.dp),
    ambientColor = Color(0x0A000000),
    spotColor = Color(0x0A000000)
)

// 按钮阴影（可选）
Modifier.shadow(
    elevation = 3.dp,
    shape = RoundedCornerShape(18.dp)
)
```

---

## 6. 图标规范

### 图标风格
- **线条粗细：** 1.5dp（细线条感）
- **风格：** 线性简约，日系无色彩图标
- **颜色：** 与文字颜色统一（`onSurface` 色）
- **圆角端点：** 所有线条端点圆弧

### 图标替换建议
| 原有图标 | 替换建议 |
|------|------|
| `Icons.Default.Star` | 保留，颜色改为焦糖色 |
| `Icons.Default.ArrowBack` | 保留，日系箭头 |
| `Icons.Default.Analytics` | → 手冲壶或咖啡杯线性图标 |
| `Icons.Default.Coffee` | 保留 |
| `Icons.Default.Tune` | → 齿轮线性细线 |
| `Icons.Default.Delete` | → 细线垃圾桶 |
| `Icons.Default.Edit` | → 细线铅笔 |
| `Icons.Default.Info` | → 细线圆形"i" |

> 注：Material Icons Extended 本身已接近简约线性风格，保留使用即可，无需替换。

---

## 7. 逐页 UI 微调方案

### 7.1 首页（HomeScreen）
- **背景：** `#FAF8F5`
- **筛选栏 Chip：** 圆角 18dp，背景 `#F5F1EB`，选中时背景 `#E8DFD0`
- **冲煮记录卡片：**
  - 圆角 18dp
  - 背景白色，轻微阴影
  - 去除所有硬线边框
  - 评分星星颜色改为 `#D4A574`
- **FAB：** 圆角 18dp，背景 `#C4A882`

### 7.2 豆子详情页（BeanDetailScreen）
- **雷达图区域：** 背景 `#F5F1EB`，圆角 18dp，内边距加宽
- **风味标签 Chip：** 圆角 18dp，背景 `#E8DFD0`，文字 `#8A8A8A`
- **操作按钮：** `OutlinedButton` 圆角 18dp，边框色 `#E8E4DE`

### 7.3 设置页（SettingsScreen）
- **背景：** `#FAF8F5`
- **分组标签文字：** 字号 13sp，颜色 `#8A8A8A`（onSurfaceVariant），字重 Medium
- **设置项：** 按分组收进白色卡片（圆角 18dp、轻微阴影），项与项之间不画硬线、靠 14dp 垂直间距自然分隔；项内图标主色、右侧 ChevronRight 弱化
- **危险操作区：** 保留红色，但降低饱和度为 `#C07070`（error 语义色）

### 7.4 关于页（AboutScreen）
- **背景：** `#FAF8F5`
- **主卡片：** 圆角 24dp，背景白色，阴影轻柔
- **技术栈列表：** 背景 `#F5F1EB`，圆角 12dp

### 7.5 统计页（StatsScreen）
- **卡片：** 圆角 18dp，背景白色，阴影 2dp
- **进度条：** 背景 `#E8E4DE`，填充色 `#C4A882`
- **各维度评分：** 星星颜色 `#D4A574`

### 7.6 冲煮记录编辑页（BrewEditScreen）
- **StarRatingRow：** 选中星色 `#D4A574`，未选中 `#E8E4DE`
- **FilterChip：** 选中背景 `#E8DFD0`，圆角 18dp
- **保存按钮：** 背景 `#C4A882`，圆角 18dp，文字白色

### 7.7 全局通用
- **TopAppBar：**
  - 背景原木棕 `#C4A882`（primary），标题/图标白色（onPrimary）——日系原木感品牌区
  - 状态栏跟随主色（浅色模式原木棕、深色模式 `#E8DFD0` 浅燕麦），`isAppearanceLightStatusBars` 随主题切换
  - 底部阴影去除或极轻
- **BottomSheet：** 圆角顶部 24dp，背景白色
- **对话框：** 圆角 24dp，背景白色
- **警示色（赏味期倒计时 / 低库存 / 危险操作）：** 统一走 error 语义色——浅色 `#F2DDDD` 容器底 + `#6B3030` 深红棕文字 + `#C07070` 强调；深色模式 `#4A2E2E` 容器底 + `#E8B8B8` 文字，禁止硬编码色值

---

## 8. 实施优先级

### P0（立即实施）
1. 配色替换：colors.xml + 主题色
2. 全局圆角统一为 18dp
3. 页面留白加大
4. 卡片阴影轻柔化

### P1（第二批）
5. 字体字重规范化
6. 星星颜色统一为焦糖色
7. TopAppBar 风格调整

### P2（后续优化）
8. 筛选 Chip 风格优化
9. 标签 Chip 风格优化
10. 图标线条精细化

---

## 9. 实施文件清单

| 文件 | 修改内容 |
|------|---------|
| `ui/theme/Color.kt` | 配色色值定义 |
| `ui/theme/Theme.kt` | 主题、阴影、圆角默认 |
| `ui/theme/Type.kt` | 字体规范 |
| `res/values/strings.xml` | 字符串资源 |
| 各个 Screen 文件 | 圆角、背景色、间距调整 |

> 注：实际文件路径需在项目中确认 theme 目录结构。
