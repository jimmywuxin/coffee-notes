package com.coffeelab.coffeenotes.ui.screen

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayThemeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
    val currentMode = prefs.getString(MainActivity.KEY_THEME_MODE, "system") ?: "system"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("显示模式") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                ThemeOptionItem(
                    icon = Icons.Default.BrightnessAuto,
                    title = "跟随系统",
                    subtitle = "根据系统设置自动切换",
                    selected = currentMode == "system",
                    onClick = {
                        MainActivity.setThemeModeAndRestart(
                            context as Activity,
                            "system"
                        )
                        navController.popBackStack()
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(start = 72.dp)) }

            item {
                ThemeOptionItem(
                    icon = Icons.Default.LightMode,
                    title = "浅色模式",
                    subtitle = "始终使用浅色主题",
                    selected = currentMode == "light",
                    onClick = {
                        MainActivity.setThemeModeAndRestart(
                            context as Activity,
                            "light"
                        )
                        navController.popBackStack()
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(start = 72.dp)) }

            item {
                ThemeOptionItem(
                    icon = Icons.Default.DarkMode,
                    title = "深色模式",
                    subtitle = "始终使用深色主题",
                    selected = currentMode == "dark",
                    onClick = {
                        MainActivity.setThemeModeAndRestart(
                            context as Activity,
                            "dark"
                        )
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}
