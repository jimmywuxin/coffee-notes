package com.coffeelab.coffeenotes.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.ui.theme.WoodPrimary
import com.coffeelab.coffeenotes.util.BackupReminder
import com.coffeelab.coffeenotes.util.CloudBackupPrefs
import com.coffeelab.coffeenotes.viewmodel.BackupViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/** 统一区块卡片：标题 + 一行说明 + 内容 */
@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

/** 统一状态条：加载进度 / 成功 / 失败 */
@Composable
private fun StatusBar(
    loading: Boolean,
    message: String?,
    isError: Boolean = false
) {
    when {
        loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        message != null -> Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                message,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
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
            scope.launch { viewModel.exportBackup(context, it) }
        }
    }

    // ZIP Import launcher
    val zipImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch { viewModel.importBackup(context, it) }
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ===== 本地：备份与恢复 =====
            SectionCard("本地备份与恢复", "数据+豆子照片生成ZIP文件，换机/误删可恢复") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))
                            zipExportLauncher.launch("CoffeeNotes_$dateStr.zip")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("导出备份")
                    }
                    OutlinedButton(
                        onClick = { zipImportLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("恢复备份")
                    }
                }
            }

            when (val st = backupState) {
                is BackupViewModel.BackupState.Loading -> StatusBar(loading = true, message = null)
                is BackupViewModel.BackupState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(st.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Text(st.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatChip(label = "豆子", value = st.beansCount.toString(), modifier = Modifier.weight(1f))
                                StatChip(label = "记录", value = st.recordsCount.toString(), modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                is BackupViewModel.BackupState.Error -> StatusBar(loading = false, message = st.message, isError = true)
                is BackupViewModel.BackupState.Idle -> {}
            }

            // ===== 云端：备份与恢复 =====
            SectionCard("云端备份与恢复", "WebDAV 网盘自动上传，一键下载恢复") {
                val existingConfig = remember { CloudBackupPrefs.getConfig(context) }
                val cloudConfigured = remember { existingConfig != null }
                var showConfig by remember { mutableStateOf(!cloudConfigured) }

                if (showConfig) {
                    var baseUrl by remember { mutableStateOf(existingConfig?.baseUrl.orEmpty()) }
                    var username by remember { mutableStateOf(existingConfig?.username.orEmpty()) }
                    var password by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = baseUrl, onValueChange = { baseUrl = it },
                        label = { Text("WebDAV 地址") },
                        placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text("账号（坚果云登录邮箱）") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("应用密码") },
                        placeholder = { Text(if (cloudConfigured) "留空保持不变" else "坚果云应用密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (baseUrl.isNotBlank() && username.isNotBlank()) {
                                // 修改场景下密码留空则沿用已存密码（内容屏蔽，不显示明文）
                                val finalPass = password.ifBlank {
                                    CloudBackupPrefs.getConfig(context)?.password.orEmpty()
                                }
                                if (finalPass.isNotBlank()) {
                                    CloudBackupPrefs.saveConfig(
                                        context,
                                        CloudBackupPrefs.CloudConfig(baseUrl, username, finalPass)
                                    )
                                    showConfig = false
                                    viewModel.testCloudConnection(context)
                                }
                            }
                        }) { Text(if (cloudConfigured) "保存" else "保存并测试") }
                        TextButton(onClick = { showConfig = false }) { Text("取消") }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.uploadToCloud(context) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("备份到云端")
                            }
                            OutlinedButton(
                                onClick = { viewModel.listCloudFiles(context) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("恢复到本地")
                            }
                        }
                        // 自动备份周期（与设置页共用同一开关；闹钟到点自动上传云端）
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "自动备份周期",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val presets = listOf(0 to "关闭", 3 to "每3天", 7 to "每周")
                            var reminderDays by remember {
                                mutableIntStateOf(BackupReminder.getIntervalDays(context))
                            }
                            var showCustomDays by remember { mutableStateOf(false) }
                            val isCustom = reminderDays > 0 && presets.none { it.first == reminderDays }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                presets.forEach { (days, label) ->
                                    FilterChip(
                                        selected = reminderDays == days && !isCustom,
                                        onClick = {
                                            reminderDays = days
                                            BackupReminder.setIntervalDays(context, days)
                                        },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                FilterChip(
                                    selected = isCustom,
                                    onClick = { showCustomDays = true },
                                    label = { Text(if (isCustom) "自定义(${reminderDays}天)" else "自定义", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            if (showCustomDays) {
                                var daysText by remember {
                                    mutableStateOf(if (reminderDays > 0) reminderDays.toString() else "3")
                                }
                                AlertDialog(
                                    onDismissRequest = { showCustomDays = false },
                                    title = { Text("自动备份周期") },
                                    text = {
                                        Column {
                                            Text(
                                                "每隔几天自动备份到云端。填 0 表示关闭。",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            OutlinedTextField(
                                                value = daysText,
                                                onValueChange = { input -> daysText = input.filter { it.isDigit() }.take(3) },
                                                label = { Text("间隔（天）") },
                                                placeholder = { Text("如 3") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            val days = daysText.toIntOrNull() ?: 0
                                            reminderDays = days
                                            BackupReminder.setIntervalDays(context, days)
                                            showCustomDays = false
                                        }) { Text("确定") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showCustomDays = false }) { Text("取消") }
                                    }
                                )
                            }
                        }
                        // 修改配置：小字弱化，点击仅进入编辑（不清空配置，密码留空保持不变），防误触
                        TextButton(
                            onClick = { showConfig = true },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("修改配置", style = MaterialTheme.typography.labelSmall) }
                    }
                }

                when (val cs = cloudState) {
                    is BackupViewModel.CloudState.Loading -> StatusBar(loading = true, message = null)
                    is BackupViewModel.CloudState.Success -> StatusBar(loading = false, message = cs.message)
                    is BackupViewModel.CloudState.Error -> StatusBar(loading = false, message = cs.message, isError = true)
                    is BackupViewModel.CloudState.Files -> {
                        if (cs.files.isEmpty()) {
                            Text("云端暂无备份", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            var confirmRestore by remember { mutableStateOf<String?>(null) }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                cs.files.forEach { name ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { confirmRestore = name }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Download, "从云端恢复", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteCloudFile(context, name) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
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
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WoodPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
