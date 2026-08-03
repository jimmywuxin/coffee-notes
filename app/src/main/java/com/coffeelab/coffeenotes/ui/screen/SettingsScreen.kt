package com.coffeelab.coffeenotes.ui.screen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.MainActivity
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
    val currentMode = prefs.getString(MainActivity.KEY_THEME_MODE, "system") ?: "system"
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showClearProgress by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    // 备份提醒周期（天）：0=关闭，7=每周，14=每两周，30=每月
    val reminderDays = com.coffeelab.coffeenotes.util.BackupReminder.getIntervalDays(context)

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("设置")
                    }
                },
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
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ===== 数据 =====
            item {
                SettingsGroup(title = "数据") {
                    SettingsItem(
                        icon = Icons.Default.Download,
                        title = "备份",
                        subtitle = "导出数据到本地文件",
                        onClick = { navController.navigate(Screen.Backup.route) }
                    )
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "备份提醒",
                        subtitle = if (reminderDays > 0) "每 ${reminderDays} 天提醒" else "关闭",
                        onClick = { showReminderDialog = true }
                    )
                }
            }

            // ===== 显示 =====
            item {
                SettingsGroup(title = "显示") {
                    SettingsItem(
                        icon = Icons.Default.BrightnessAuto,
                        title = "显示模式",
                        subtitle = when (currentMode) {
                            "light" -> "浅色模式"
                            "dark" -> "深色模式"
                            else -> "跟随系统"
                        },
                        onClick = { navController.navigate(Screen.DisplayTheme.route) }
                    )
                }
            }

            // ===== 工具 =====
            item {
                SettingsGroup(title = "工具") {
                    SettingsItem(
                        icon = Icons.Default.Analytics,
                        title = "统计总览",
                        subtitle = "查看冲煮数据统计",
                        onClick = { navController.navigate(Screen.Stats.createRoute()) }
                    )
                    SettingsItem(
                        icon = Icons.Default.AccountTree,
                        title = "冲煮手法管理",
                        subtitle = "添加或编辑冲煮手法",
                        onClick = { navController.navigate(Screen.BrewMethodList.route) }
                    )
                    SettingsItem(
                        icon = Icons.Default.LocalCafe,
                        title = "器具管理",
                        subtitle = "添加或编辑咖啡器具",
                        onClick = { navController.navigate(Screen.EquipmentManagement.route) }
                    )
                    SettingsItem(
                        icon = Icons.Default.Refresh,
                        title = "磨豆机管理",
                        subtitle = "添加或编辑磨豆机",
                        onClick = { navController.navigate(Screen.GrinderManagement.route) }
                    )
                }
            }

            // ===== 咖啡豆工具 =====
            item {
                SettingsGroup(title = "咖啡豆工具") {
                    SettingsItem(
                        icon = Icons.Default.Label,
                        title = "印象标签管理",
                        subtitle = "管理自定义印象标签",
                        onClick = { navController.navigate(Screen.ImpressionTagManagement.route) }
                    )
                    SettingsItem(
                        icon = Icons.Default.LocalFireDepartment,
                        title = "烘焙度配置",
                        subtitle = "管理烘焙度、养豆期和赏味期",
                        onClick = { navController.navigate(Screen.RoastDegreeConfigManagement.route) }
                    )
                    SettingsItem(
                        icon = Icons.Default.WaterDrop,
                        title = "处理法管理",
                        subtitle = "管理咖啡豆处理方式",
                        onClick = { navController.navigate(Screen.ProcessMethodManagement.route) }
                    )
                }
            }

            // ===== 关于 =====
            item {
                SettingsGroup(title = "关于") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "关于咖啡笔记",
                        subtitle = "版本信息与应用介绍",
                        onClick = { navController.navigate(Screen.About.route) }
                    )
                }
            }

            // ===== 危险区域 =====
            item {
                SettingsGroup(title = "危险区域", isDestructive = true) {
                    SettingsItem(
                        icon = Icons.Default.DeleteForever,
                        title = "清空所有数据",
                        subtitle = "删除所有咖啡豆、冲煮记录和手法",
                        onClick = { showClearDataDialog = true },
                        isDestructive = true
                    )
                }
            }
        }
    }

    // 备份提醒周期选择 Dialog（自定义天数）
    if (showReminderDialog) {
        var daysText by remember { mutableStateOf(if (reminderDays > 0) reminderDays.toString() else "7") }
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("备份提醒") },
            text = {
                Column {
                    Text(
                        "每隔几天提醒你导出备份，防止数据丢失。填 0 表示关闭。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { input -> daysText = input.filter { it.isDigit() }.take(3) },
                        label = { Text("提醒间隔（天）") },
                        placeholder = { Text("如 7") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val days = daysText.toIntOrNull() ?: 0
                    if (days > 0) {
                        // Android 13+ 需要通知权限；未授权时先请求（回调里应用设置）
                        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            (context as? MainActivity)?.requestNotificationPermission { granted ->
                                if (granted) {
                                    com.coffeelab.coffeenotes.util.BackupReminder.setIntervalDays(context, days)
                                } else {
                                    com.coffeelab.coffeenotes.util.BackupReminder.setIntervalDays(context, 0)
                                }
                            }
                        } else {
                            com.coffeelab.coffeenotes.util.BackupReminder.setIntervalDays(context, days)
                        }
                    } else {
                        com.coffeelab.coffeenotes.util.BackupReminder.setIntervalDays(context, 0)
                    }
                    showReminderDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) { Text("取消") }
            }
        )
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { if (!showClearProgress) showClearDataDialog = false },
            title = { Text("确认清空数据") },
            text = {
                if (showClearProgress) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("正在清空数据...")
                    }
                } else {
                    Text("此操作不可逆。所有咖啡豆、冲煮记录、手法和器具将被永久删除。\n\n建议先备份数据。")
                }
            },
            confirmButton = {
                if (!showClearProgress) {
                    TextButton(
                        onClick = {
                            showClearProgress = true
                            scope.launch {
                                try {
                                    val db = AppDatabase.getInstance(navController.context)
                                    db.brewRecordDao().deleteAll()
                                    db.brewMethodDao().deleteAll()
                                    db.coffeeBeanDao().deleteAll()
                                    db.equipmentDao().deleteAll()
                                    db.grinderDao().deleteAll()
                                    db.roastDegreeDao().deleteAll()
                                    db.processMethodDao().deleteAll()
                                    db.restPeriodConfigDao().deleteAll()
                                    db.peakFlavorConfigDao().deleteAll()
                                    db.purchaseRecordDao().deleteAll()
                                    db.impressionTagDao().deleteAll()
                                    db.stockAdjustmentDao().deleteAll()
                                    showClearDataDialog = false
                                    showClearProgress = false
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    showClearProgress = false
                                }
                            }
                        }
                    ) { Text("确认清空", color = MaterialTheme.colorScheme.error) }
                }
            },
            dismissButton = {
                if (!showClearProgress) {
                    TextButton(onClick = { showClearDataDialog = false }) { Text("取消") }
                }
            }
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
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
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 设置分组：13sp/Medium 分组标题 + 白色圆角卡片容器（无硬线分割）。
 * 符合 UI_STYLE_GUIDE 7.3：设置项白卡、圆角18dp、间距14dp。
 */
@Composable
private fun SettingsGroup(
    title: String,
    isDestructive: Boolean = false,
    items: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            items()
        }
    }
}

@Composable
fun ThemeModeSelector() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
    val currentMode = prefs.getString(MainActivity.KEY_THEME_MODE, "system") ?: "system"
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            // Header row – always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (currentMode == "dark") Icons.Default.DarkMode
                        else if (currentMode == "light") Icons.Default.LightMode
                        else Icons.Default.BrightnessAuto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "显示模式",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (currentMode) {
                            "light" -> "浅色模式"
                            "dark" -> "深色模式"
                            else -> "跟随系统"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable options
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ThemeRadioItem(
                    icon = Icons.Default.BrightnessAuto,
                    title = "跟随系统",
                    selected = currentMode == "system",
                    onClick = {
                        MainActivity.setThemeModeAndRestart(
                            context as android.app.Activity,
                            "system"
                        )
                    }
                )
                ThemeRadioItem(
                    icon = Icons.Default.LightMode,
                    title = "浅色模式",
                    selected = currentMode == "light",
                    onClick = {
                        MainActivity.setThemeModeAndRestart(
                            context as android.app.Activity,
                            "light"
                        )
                    }
                )
                ThemeRadioItem(
                    icon = Icons.Default.DarkMode,
                    title = "深色模式",
                    selected = currentMode == "dark",
                    onClick = {
                        MainActivity.setThemeModeAndRestart(
                            context as android.app.Activity,
                            "dark"
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeRadioItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 56.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}
