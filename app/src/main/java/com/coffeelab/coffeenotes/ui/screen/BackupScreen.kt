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
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.util.BackupUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var backupFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var showImportDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // Refresh backup list
    LaunchedEffect(Unit) {
        backupFiles = BackupUtil.getBackupFiles(context)
    }

    // Import file picker
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            if (BackupUtil.importDb(context, it)) {
                message = "数据导入成功！请重启 App 生效"
            } else {
                message = "导入失败，请检查文件格式"
            }
        }
    }

    // Share file launcher (Android save dialog)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            try {
                val dbFile = context.getDatabasePath("coffee_notes.db")
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    dbFile.inputStream().use { inp -> inp.copyTo(out) }
                }
                message = "备份完成"
            } catch (e: Exception) {
                message = "备份失败: ${e.message}"
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
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val file = BackupUtil.exportDb(context)
                        message = "已备份到: ${file.name}"
                        backupFiles = BackupUtil.getBackupFiles(context)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出备份")
                }
                OutlinedButton(
                    onClick = {
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
                        exportLauncher.launch("CoffeeNotes_$dateStr.db")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存到文件")
                }
            }

            OutlinedButton(
                onClick = { importLauncher.launch("application/octet-stream") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("从备份文件导入")
            }

            Divider()

            // Message
            message?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Backup file list
            Text("本地备份列表", style = MaterialTheme.typography.titleMedium)
            if (backupFiles.isEmpty()) {
                Text(
                    "暂无备份文件",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(backupFiles) { file ->
                        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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
                                        message = "已删除: ${file.name}"
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

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "备份文件存储在手机内部存储的 CoffeeNotes/backups/ 目录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
