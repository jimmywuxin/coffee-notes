package com.coffeelab.coffeenotes.ui.screen

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.RestPeriodConfig
import com.coffeelab.coffeenotes.data.entity.RoastDegree
import com.coffeelab.coffeenotes.viewmodel.RestPeriodConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestPeriodConfigManagementScreen(
    navController: NavController,
    viewModel: RestPeriodConfigViewModel = viewModel()
) {
    val configs by viewModel.allConfigs.collectAsState(initial = emptyList())
    val roastDegrees by viewModel.allRoastDegrees.collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<RestPeriodConfig?>(null) }
    var selectedRoastDegreeId by remember { mutableStateOf<Long?>(null) }
    var daysInput by remember { mutableStateOf("") }

    fun roastDegreeName(id: Long): String = roastDegrees.find { it.id == id }?.name ?: "未知"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("养豆期管理")
                    }
                },
                actions = {
                    IconButton(onClick = { selectedRoastDegreeId = null; daysInput = ""; showAddDialog = true }) {
                        Icon(Icons.Default.Add, "添加配置", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (configs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无配置\n点击右上角 + 添加", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(configs, key = { it.id }) { config ->
                    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(roastDegreeName(config.roastDegreeId), style = MaterialTheme.typography.bodyLarge)
                                Text("养豆 ${config.restDays} 天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row {
                                IconButton(onClick = {
                                    editingConfig = config
                                    selectedRoastDegreeId = config.roastDegreeId
                                    daysInput = config.restDays.toString()
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteConfig(config) }) {
                                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加养豆期配置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = roastDegrees.find { it.id == selectedRoastDegreeId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("烘焙度") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 260.dp)) {
                            roastDegrees.forEach { rd ->
                                DropdownMenuItem(
                                    text = { Text(rd.name) },
                                    onClick = { selectedRoastDegreeId = rd.id; expanded = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = daysInput,
                        onValueChange = { daysInput = it.filter { c -> c.isDigit() } },
                        label = { Text("养豆天数") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val days = daysInput.toIntOrNull()
                    if (selectedRoastDegreeId != null && days != null && days > 0) {
                        viewModel.addOrUpdateConfig(selectedRoastDegreeId!!, days)
                        showAddDialog = false
                    }
                }, enabled = selectedRoastDegreeId != null && daysInput.toIntOrNull()?.let { it > 0 } == true) {
                    Text("添加")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }

    // Edit dialog
    if (showEditDialog && editingConfig != null) {
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("编辑养豆期配置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = roastDegrees.find { it.id == selectedRoastDegreeId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("烘焙度") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 260.dp)) {
                            roastDegrees.forEach { rd ->
                                DropdownMenuItem(
                                    text = { Text(rd.name) },
                                    onClick = { selectedRoastDegreeId = rd.id; expanded = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = daysInput,
                        onValueChange = { daysInput = it.filter { c -> c.isDigit() } },
                        label = { Text("养豆天数") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val days = daysInput.toIntOrNull()
                    if (selectedRoastDegreeId != null && days != null && days > 0) {
                        viewModel.updateConfig(editingConfig!!.copy(roastDegreeId = selectedRoastDegreeId!!, restDays = days))
                        showEditDialog = false
                    }
                }, enabled = selectedRoastDegreeId != null && daysInput.toIntOrNull()?.let { it > 0 } == true) {
                    Text("保存")
                }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("取消") } }
        )
    }
}
