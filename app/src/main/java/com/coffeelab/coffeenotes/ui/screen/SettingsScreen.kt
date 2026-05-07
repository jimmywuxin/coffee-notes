package com.coffeelab.coffeenotes.ui.screen

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showClearProgress by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                Text(
                    text = "数据",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.FileDownload,
                    title = "备份",
                    subtitle = "导出数据到本地文件",
                    onClick = { navController.navigate(Screen.Backup.route) }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                Text(
                    text = "工具",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Analytics,
                    title = "统计",
                    subtitle = "查看冲煮数据统计",
                    onClick = { navController.navigate(Screen.Stats.createRoute()) }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.LocalCafe,
                    title = "冲煮手法",
                    subtitle = "管理冲煮手法",
                    onClick = { navController.navigate(Screen.BrewMethodList.route) }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Build,
                    title = "管理器具",
                    subtitle = "添加或编辑咖啡器具",
                    onClick = { navController.navigate(Screen.EquipmentManagement.route) }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Tune,
                    title = "磨豆机管理",
                    subtitle = "添加或编辑磨豆机",
                    onClick = { navController.navigate(Screen.GrinderManagement.route) }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Archive,
                    title = "已归档的豆子",
                    subtitle = "查看已归档的咖啡豆",
                    onClick = { navController.navigate(Screen.ArchiveList.route) }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "关于咖啡笔记",
                    subtitle = "版本信息与应用介绍",
                    onClick = { navController.navigate(Screen.About.route) }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                Text(
                    text = "危险区域",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item {
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
                    Text("此操作不可逆。所有咖啡豆、冲煮记录、配方和器具将被永久删除。\n\n建议先备份数据。")
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
