package com.coffeelab.coffeenotes.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// 日系治愈原木风配色体系
// 风格：纯日系极简治愈感，原木咖啡馆氛围
// 干净温柔、低对比度、不刺眼，适合长期日常使用
// ============================================================

// --- 主色：原木系 ---
val WoodPrimary = Color(0xFFC4A882)       // 原木浅棕（主按钮/高亮）
val WoodPrimaryDark = Color(0xFFA68B5B)   // 原木深色（悬停/按下态）
val WoodLight = Color(0xFFE8DFD0)         // 浅燕麦（选中态背景）

// --- 辅助色 ---
val SageMuted = Color(0xFFA8B5A0)         // 莫兰迪浅茶绿（次要标签/图标）
val FogGray = Color(0xFFB8B4AF)            // 雾灰（禁用态/次要文字）

// --- 强调色 ---
val Caramel = Color(0xFFD4A574)           // 浅焦糖奶茶（评分星星/强调）
val CaramelDark = Color(0xFFC08B5C)       // 焦糖深色（按钮悬停态）

// --- 背景色 ---
val CreamBackground = Color(0xFFFAF8F5)  // 暖米白（页面背景）
val OatMeal = Color(0xFFF5F1EB)           // 浅燕麦（分组背景/次级卡片）
val PureWhite = Color(0xFFFFFFFF)         // 纯白卡片背景

// --- 文字色 ---
val TextDeep = Color(0xFF4A4A4A)          // 深暖灰（主文字，非纯黑）
val TextMid = Color(0xFF8A8A8A)           // 中灰（次要文字）
val TextHint = Color(0xFFC0BCB8)          // 浅灰（占位符/hint）

// --- 边框/分割线 ---
val BorderLight = Color(0xFFE8E4DE)       // 微暖灰（极淡边框）

// --- 危险色（暖红系，低饱和日系感）---
val DangerMuted = Color(0xFFC07070)       // 降低饱和度的危险红
val ErrorContainer = Color(0xFFF2DDDD)    // 暖粉底（低库存/警示卡片背景）
val OnErrorContainer = Color(0xFF6B3030)  // 深红棕（警示卡片上的文字）
val DarkErrorContainer = Color(0xFF4A2E2E)    // 深色模式暖红底
val DarkOnErrorContainer = Color(0xFFE8B8B8)  // 深色模式警示文字

// ============================================================
// 语义色映射（适配 MaterialTheme colorScheme）
// ============================================================
val Primary = WoodPrimary
val OnPrimary = Color.White
val PrimaryContainer = WoodLight
val OnPrimaryContainer = WoodPrimaryDark

val Secondary = Caramel
val OnSecondary = Color.White
val SecondaryContainer = Color(0xFFF5EBE0)
val OnSecondaryContainer = CaramelDark

val Tertiary = SageMuted
val OnTertiary = Color.White
val TertiaryContainer = Color(0xFFE8EDE4)
val OnTertiaryContainer = Color(0xFF5A6650)

val Background = CreamBackground
val OnBackground = TextDeep
val Surface = PureWhite
val OnSurface = TextDeep
val SurfaceVariant = OatMeal
val OnSurfaceVariant = TextMid
val Outline = BorderLight
val OutlineVariant = Color(0xFFF0EDE8)

val Error = DangerMuted
val OnError = Color.White

// --- Dark Theme（保持深色但不改变色调）---
val DarkBackground = Color(0xFF1E1C1A)
val DarkSurface = Color(0xFF252320)
val DarkSurfaceVariant = Color(0xFF3A3835)
val DarkOnBackground = Color(0xFFE8E4DF)
val DarkOnSurface = Color(0xFFE8E4DF)
val DarkOnSurfaceVariant = Color(0xFFB8B4AF)
val DarkOutline = Color(0xFF5A5650)
