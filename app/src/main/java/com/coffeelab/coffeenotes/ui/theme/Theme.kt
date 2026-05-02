package com.coffeelab.coffeenotes.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MediumBrown,
    onPrimary = WarmWhite,
    primaryContainer = CoffeeGoldLight,
    secondary = CoffeeGold,
    onSecondary = WarmWhite,
    background = WarmWhite,
    surface = SurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    outline = LightBrown
)

private val DarkColorScheme = darkColorScheme(
    primary = LightBrown,
    onPrimary = DarkBrown,
    primaryContainer = Brown40,
    secondary = CoffeeGold,
    onSecondary = DarkBrown,
    background = SurfaceDark,
    surface = SurfaceDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    outline = Brown60
)

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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CoffeeTypography,
        content = content
    )
}
