package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.Equipment
import com.coffeelab.coffeenotes.viewmodel.EquipmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentManagementScreen(
    navController: NavController,
    equipmentViewModel: EquipmentViewModel = viewModel()
) {
    val equipmentList by equipmentViewModel.allEquipment.collectAsState(initial = emptyList())
    val mutableList = remember { mutableStateListOf(*equipmentList.toTypedArray()) }
    var isReorderMode by remember { mutableStateOf(false) }

    // 拖拽状态 — 使用 LazyListState 精确追踪位置
    val lazyListState = rememberLazyListState()
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var dragStartItemOffset by remember { mutableFloatStateOf(0f) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val smoothDragOffset by animateFloatAsState(
        targetValue = accumulatedDrag,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
    )

    // Keep mutableList in sync with equipmentList when not in reorder mode
    LaunchedEffect(equipmentList, isReorderMode) {
        if (!isReorderMode) {
            mutableList.clear()
            mutableList.addAll(equipmentList)
            draggingItemIndex = -1
            accumulatedDrag = 0f
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingEquipment by remember { mutableStateOf<Equipment?>(null) }
    var newEquipmentName by remember { mutableStateOf("") }

    fun saveOrder() {
        isReorderMode = false
        draggingItemIndex = -1
        accumulatedDrag = 0f
        equipmentViewModel.saveEquipmentOrder(mutableList.toList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isReorderMode) Text("器具排序") else Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocalCafe, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("器具管理") }
                },
                navigationIcon = {
                    if (isReorderMode) {
                        IconButton(onClick = { isReorderMode = false }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
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
                        // 松手即完成，不再需要完成按钮
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isReorderMode) {
                    itemsIndexed(mutableList, key = { _, item -> item.id }) { index, equipment ->
                        val isDraggingThisItem = isReorderMode && draggingItemIndex == index
                        DraggableEquipmentItem(
                            equipment = equipment,
                            isDragging = isDraggingThisItem,
                            dragOffset = if (isDraggingThisItem) smoothDragOffset else 0f,
                            onDragStart = {
                                draggingItemIndex = index
                                accumulatedDrag = 0f
                                val layoutInfo = lazyListState.layoutInfo
                                val myInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                dragStartItemOffset = myInfo?.offset?.toFloat() ?: 0f
                            },
                            onDrag = { dragAmount ->
                                accumulatedDrag += dragAmount.y
                                val layoutInfo = lazyListState.layoutInfo
                                val myInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }
                                if (myInfo != null) {
                                    val currentCenterY = myInfo.offset + myInfo.size / 2 + accumulatedDrag
                                    var targetIndex = draggingItemIndex
                                    layoutInfo.visibleItemsInfo
                                        .filter { it.index != draggingItemIndex }
                                        .forEach { info ->
                                            val itemCenter = info.offset + info.size / 2
                                            if (accumulatedDrag > 0 && info.index > draggingItemIndex && currentCenterY > itemCenter) {
                                                targetIndex = maxOf(targetIndex, info.index)
                                            } else if (accumulatedDrag < 0 && info.index < draggingItemIndex && currentCenterY < itemCenter) {
                                                targetIndex = minOf(targetIndex, info.index)
                                            }
                                        }
                                    targetIndex = targetIndex.coerceIn(0, mutableList.lastIndex)
                                    if (targetIndex != draggingItemIndex) {
                                        val item = mutableList.removeAt(draggingItemIndex)
                                        mutableList.add(targetIndex, item)
                                        draggingItemIndex = targetIndex
                                        accumulatedDrag = 0f
                                        val newLayoutInfo = lazyListState.layoutInfo
                                        val newInfo = newLayoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                                        dragStartItemOffset = newInfo?.offset?.toFloat() ?: 0f
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingItemIndex = -1
                                accumulatedDrag = 0f
                                equipmentViewModel.saveEquipmentOrder(mutableList.toList())
                                isReorderMode = false
                            }
                        )
                    }
                } else {
                    itemsIndexed(equipmentList, key = { _, item -> item.id }) { _, equipment ->
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
private fun DraggableEquipmentItem(
    equipment: Equipment,
    isDragging: Boolean,
    dragOffset: Float,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedElevation by animateFloatAsState(
        targetValue = if (isDragging) 12f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "elevation"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "scale"
    )
    val animatedDragOffset by animateFloatAsState(
        targetValue = if (isDragging) dragOffset else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 150f),
        label = "dragOffset"
    )
    val animatedBgColor by animateColorAsState(
        targetValue = if (isDragging) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 150f),
        label = "bgColor"
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (isDragging) 0.95f else 1f
                shadowElevation = animatedElevation
                translationY = animatedDragOffset
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            },
        color = animatedBgColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "拖动排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = equipment.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
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
