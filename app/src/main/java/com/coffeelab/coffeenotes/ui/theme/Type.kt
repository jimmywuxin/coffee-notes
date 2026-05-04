package com.coffeelab.coffeenotes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// 日系治愈原木风圆角 & 字体规范
// ============================================================

// --- 统一圆角：18dp ---
val CoffeeShapes = Shapes(
    extraSmall = RoundedCornerShape(18.dp),   // 小元素（chips 等）
    small = RoundedCornerShape(18.dp),         // 小卡片
    medium = RoundedCornerShape(18.dp),        // 中等卡片/按钮
    large = RoundedCornerShape(18.dp),         // 大卡片
    extraLarge = RoundedCornerShape(24.dp)     // 对话框/BottomSheet 顶部
)

// --- 日系圆润字体规范 ---
val CoffeeTypography = Typography(
    // 页面大标题
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,    // 标题稍粗
        fontSize = 22.sp,
        lineHeight = 30.sp,                  // 1.36 行高
        letterSpacing = 0.sp
    ),
    // 卡片标题
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // Section 标题
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    // 分组标签
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    // 正文
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,       // 正文字体纤细
        fontSize = 15.sp,
        lineHeight = 26.sp,                  // 宽松行高 1.73
        letterSpacing = 0.sp
    ),
    // 说明文字
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 22.sp,                  // 宽松行高 1.69
        letterSpacing = 0.sp
    ),
    // 标签文字
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    // 小标签
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    // 最小文字
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)
