package com.coffeelab.coffeenotes.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.ui.theme.WoodPrimary
import com.coffeelab.coffeenotes.util.CloudBackupPrefs
import com.coffeelab.coffeenotes.viewmodel.BackupViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = WoodPrimary
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    navController: NavController,
    viewModel: BackupViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle(initialValue = viewModel.backupState.value)
    val cloudState by viewModel.cloudState.collectAsStateWithLifecycle(initialValue = viewModel.cloudState.value)

    // ZIP Export launcher
    val zipExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            scope.launch {
                viewModel.exportBackup(context, it)
            }
        }
    }

    // ZIP Import launcher
    val zipImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                viewModel.importBackup(context, it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Download, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("备份与恢复") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Export section
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("导出备份", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "将所有数据（含豆子照片）导出为 ZIP 文件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    Button(
                        onClick = {
                            val dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))
                            zipExportLauncher.launch("CoffeeNotes_$dateStr.zip")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("导出备份")
                    }
                }
            }

            // Import section
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("恢复备份", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "从 ZIP 备份文件恢复全部数据（含照片）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    OutlinedButton(
                        onClick = { zipImportLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("恢复备份")
                    }
                }
            }

            // ===== 云端备份（WebDAV） =====
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("云端备份（坚果云/WebDAV）", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "配置后手动/自动上传 ZIP 到你的网盘，换机、丢手机也能恢复",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    val cloudConfigured = remember { CloudBackupPrefs.isConfigured(context) }
                    var showConfig by remember { mutableStateOf(!cloudConfigured) }

                    if (showConfig) {
                        var baseUrl by remember { mutableStateOf("") }
                        var username by remember { mutableStateOf("") }
                        var password by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = baseUrl, onValueChange = { baseUrl = it },
                            label = { Text("WebDAV 地址") },
                            placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = username, onValueChange = { username = it },
                            label = { Text("账号（坚果云登录邮箱）") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text("应用密码（账户信息页生成，非登录密码）") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                    CloudBackupPrefs.saveConfig(
                                        context,
                                        CloudBackupPrefs.CloudConfig(baseUrl, username, password)
                                    )
                                    showConfig = false
                                    viewModel.testCloudConnection(context)
                                }
                            }) { Text("保存并测试连接") }
                            TextButton(onClick = { showConfig = false }) { Text("取消") }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.uploadToCloud(context) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("上传到云端")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.listCloudFiles(context) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("云端文件") }
                                TextButton(
                                    onClick = {
                                        CloudBackupPrefs.clearConfig(context)
                                        viewModel.resetCloudState()
                                        showConfig = true
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("修改配置") }
                            }
                        }
                    }

                    // Cloud status
                    when (val cs = cloudState) {
                        is BackupViewModel.CloudState.Loading -> {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        is BackupViewModel.CloudState.Success -> {
                            Spacer(Modifier.height(8.dp))
                            Text(cs.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        }
                        is BackupViewModel.CloudState.Error -> {
                            Spacer(Modifier.height(8.dp))
                            Text(cs.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                        is BackupViewModel.CloudState.Files -> {
                            Spacer(Modifier.height(8.dp))
                            if (cs.files.isEmpty()) {
                                Text("云端暂无备份", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            } else {
                                var confirmRestore by remember { mutableStateOf<String?>(null) }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    cs.files.forEach { name ->
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                            IconButton(onClick = { confirmRestore = name }) {
                                                Icon(Icons.Default.Download, "从云端恢复", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { viewModel.deleteCloudFile(context, name) }) {
                                                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                                confirmRestore?.let { fileName ->
                                    AlertDialog(
                                        onDismissRequest = { confirmRestore = null },
                                        title = { Text("确认恢复") },
                                        text = { Text("将从「$fileName」恢复全部数据，覆盖当前内容。确定继续吗？") },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                viewModel.restoreFromCloud(context, fileName)
                                                confirmRestore = null
                                            }) { Text("恢复") }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { confirmRestore = null }) { Text("取消") }
                                        }
                                    )
                                }
                            }
                        }
                        is BackupViewModel.CloudState.Idle -> {}
                    }
                }
            }

            // Status
            when (val state = backupState) {
                is BackupViewModel.BackupState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("处理中...", style = MaterialTheme.typography.bodySmall)
                }
                is BackupViewModel.BackupState.Success -> {
                    val state = backupState as BackupViewModel.BackupState.Success
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    state.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Backup info row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "备份日期",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        state.exportDate.takeIf { it != "unknown" } ?: "—",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "备份版本",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        state.version.takeIf { it != "unknown" } ?: "—",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatChip(
                                    label = "豆子",
                                    value = state.beansCount.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                StatChip(
                                    label = "冲煮记录",
                                    value = state.recordsCount.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                is BackupViewModel.BackupState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            state.message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                is BackupViewModel.BackupState.Idle -> {}
            }
        }
    }
}