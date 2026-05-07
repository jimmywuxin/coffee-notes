package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.Equipment
import com.coffeelab.coffeenotes.viewmodel.EquipmentViewModel
import androidx.compose.material3.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentManagementScreen(
    navController: NavController,
    equipmentViewModel: EquipmentViewModel = viewModel()
) {
    val equipmentList by equipmentViewModel.allEquipment.collectAsState(initial = emptyList())
    val mutableList = remember { mutableStateListOf(*equipmentList.toTypedArray()) }
    var isReorderMode by remember { mutableStateOf(false) }

    // Keep mutableList in sync with equipmentList when not in reorder mode
    LaunchedEffect(equipmentList, isReorderMode) {
        if (!isReorderMode) {
            mutableList.clear()
            mutableList.addAll(equipmentList)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingEquipment by remember { mutableStateOf<Equipment?>(null) }
    var newEquipmentName by remember { mutableStateOf("") }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (toIndex < 0 || toIndex >= mutableList.size) return
        val item = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, item)
    }

    fun saveOrder() {
        isReorderMode = false
        equipmentViewModel.saveEquipmentOrder(mutableList.toList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isReorderMode) Text("器具排序") else Text("器具管理")
                },
                navigationIcon = {
                    if (isReorderMode) {
                        IconButton(onClick = { isReorderMode = false }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (!isReorderMode && equipmentList.isNotEmpty()) {
                        IconButton(onClick = { isReorderMode = true }) {
                            Icon(Icons.Default.SwapVert, contentDescription = "排序", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    if (!isReorderMode) {
                        IconButton(onClick = {
                            newEquipmentName = ""
                            showAddDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "添加器具", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    if (isReorderMode) {
                        IconButton(onClick = { saveOrder() }) {
                            Icon(Icons.Default.Check, contentDescription = "完成排序", tint = MaterialTheme.colorScheme.onPrimary)
                        }
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
        if (equipmentList.isEmpty() && !isReorderMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有器具\n点击右下角 + 添加",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isReorderMode) {
                    itemsIndexed(mutableList) { index, equipment ->
                        ReorderableEquipmentItem(
                            equipment = equipment,
                            onMoveUp = { moveItem(index, index - 1) },
                            onMoveDown = { moveItem(index, index + 1) },
                            isFirst = index == 0,
                            isLast = index == mutableList.size - 1
                        )
                    }
                } else {
                    itemsIndexed(equipmentList) { _, equipment ->
                        EquipmentItem(
                            equipment = equipment,
                            onEdit = {
                                editingEquipment = equipment
                                newEquipmentName = equipment.name
                                showEditDialog = true
                            },
                            onDelete = {
                                editingEquipment = equipment
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Equipment Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加器具") },
            text = {
                OutlinedTextField(
                    value = newEquipmentName,
                    onValueChange = { newEquipmentName = it },
                    label = { Text("器具名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newEquipmentName.isNotBlank()) {
                            equipmentViewModel.addEquipment(newEquipmentName.trim())
                            showAddDialog = false
                        }
                    },
                    enabled = newEquipmentName.isNotBlank()
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            }
        )
    }

    // Edit Equipment Dialog
    if (showEditDialog && editingEquipment != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("编辑器具") },
            text = {
                OutlinedTextField(
                    value = newEquipmentName,
                    onValueChange = { newEquipmentName = it },
                    label = { Text("器具名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newEquipmentName.isNotBlank()) {
                            editingEquipment?.let {
                                equipmentViewModel.updateEquipment(
                                    it.copy(name = newEquipmentName.trim())
                                )
                            }
                            showEditDialog = false
                        }
                    },
                    enabled = newEquipmentName.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("取消") }
            }
        )
    }

    // Delete Equipment Dialog
    if (showDeleteDialog && editingEquipment != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${editingEquipment?.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingEquipment?.let { equipmentViewModel.deleteEquipment(it) }
                        showDeleteDialog = false
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ReorderableEquipmentItem(
    equipment: Equipment,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "拖动排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = equipment.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "上移",
                    tint = if (!isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "下移",
                    tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun EquipmentItem(
    equipment: Equipment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = equipment.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
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
