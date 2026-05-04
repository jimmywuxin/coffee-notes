package com.coffeelab.coffeenotes.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// 日系柔和阴影色彩
private val SoftShadowColor = Color(0x0A000000)   // 极淡黑，柔和阴影
private val CardElevation = 2.dp                        // 卡片轻柔阴影

// ============================================================
// 浅色主题
// ============================================================
private val LightColorScheme = lightColorScheme(
    // 主色（原木浅棕）
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,

    // 辅助色（焦糖）
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    // 第三色（莫兰迪茶绿）
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    // 背景 & 表面
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    // 边框 & 轮廓
    outline = Outline,
    outlineVariant = OutlineVariant,

    // 错误色
    error = Error,
    onError = OnError
)

// ============================================================
// 深色主题
// ============================================================
private val DarkColorScheme = darkColorScheme(
    primary = WoodLight,
    onPrimary = WoodPrimaryDark,
    primaryContainer = WoodPrimaryDark,
    onPrimaryContainer = WoodLight,

    secondary = Caramel,
    onSecondary = Color.White,
    secondaryContainer = CaramelDark,
    onSecondaryContainer = Color(0xFFF5EBE0),

    tertiary = SageMuted,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3A4A35),
    onTertiaryContainer = Color(0xFFD8E4D0),

    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    outline = DarkOutline,
    outlineVariant = Color(0xFF353230),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

// ============================================================
// 主题入口
// ============================================================
@Composable
fun CoffeeNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 状态栏跟随主色调
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CoffeeTypography,
        shapes = CoffeeShapes,
        content = content
    )
}
