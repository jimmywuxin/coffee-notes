package com.coffeelab.coffeenotes.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.util.BackupUtil
import com.coffeelab.coffeenotes.viewmodel.BackupViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    navController: NavController,
    viewModel: BackupViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupState by viewModel.backupState.collectAsState()

    var backupFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Refresh backup list
    LaunchedEffect(Unit) {
        backupFiles = BackupUtil.getBackupFiles(context)
    }

    // JSON Export launcher
    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                viewModel.exportBackup(context, it)
            }
        }
    }

    // JSON Import launcher
    val jsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                viewModel.importBackup(context, it)
            }
        }
    }

    // Raw DB Import launcher
    val dbImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            if (BackupUtil.importDb(context, it)) {
                viewModel.resetState()
                // Show toast
                Toast.makeText(context, "数据导入成功！请重启 App 生效", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "导入失败，请检查文件格式", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Raw DB Export launcher
    val dbExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            try {
                val dbFile = context.getDatabasePath("coffee_notes.db")
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    dbFile.inputStream().use { inp -> inp.copyTo(out) }
                }
                Toast.makeText(context, "备份完成", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "备份失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💾 备份管理") },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // JSON Backup Section
            Text("☕ 咖啡笔记备份", style = MaterialTheme.typography.titleMedium)
            Text(
                "导出为 JSON 格式，可跨设备恢复",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
                        jsonExportLauncher.launch("coffee-notes-backup-$dateStr.json")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出备份")
                }
                OutlinedButton(
                    onClick = { jsonImportLauncher.launch("application/json") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("恢复备份")
                }
            }

            // Status message
            when (val state = backupState) {
                is BackupViewModel.BackupState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("处理中...", style = MaterialTheme.typography.bodySmall)
                }
                is BackupViewModel.BackupState.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            state.message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is BackupViewModel.BackupState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            state.message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                is BackupViewModel.BackupState.Idle -> { }
            }

            HorizontalDivider()

            // Raw DB Backup Section
            Text("📁 数据库备份", style = MaterialTheme.typography.titleMedium)
            Text(
                "直接备份数据库文件，恢复后需重启 App",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val file = BackupUtil.exportDb(context)
                        backupFiles = BackupUtil.getBackupFiles(context)
                        Toast.makeText(context, "已备份到: ${file.name}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("本地备份")
                }
                OutlinedButton(
                    onClick = {
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
                        dbExportLauncher.launch("CoffeeNotes_$dateStr.db")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存到文件")
                }
            }

            OutlinedButton(
                onClick = { dbImportLauncher.launch("application/octet-stream") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("从数据库文件导入")
            }

            HorizontalDivider()

            // Local backup list
            Text("本地备份列表", style = MaterialTheme.typography.titleMedium)
            if (backupFiles.isEmpty()) {
                Text(
                    "暂无备份文件",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(backupFiles) { file ->
                        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        dateFormat.format(Date(file.lastModified())),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        BackupUtil.deleteBackup(file)
                                        backupFiles = BackupUtil.getBackupFiles(context)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                "备份文件存储在手机内部存储的 CoffeeNotes/backups/ 目录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
