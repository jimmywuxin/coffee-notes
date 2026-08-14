package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.Equipment
import com.coffeelab.coffeenotes.ui.component.DraggableManagementItem
import com.coffeelab.coffeenotes.ui.component.EmptyState
import com.coffeelab.coffeenotes.viewmodel.EquipmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentManagementScreen(
    navController: NavController,
    equipmentViewModel: EquipmentViewModel = viewModel()
) {
    val equipmentList by equipmentViewModel.allEquipment.collectAsStateWithLifecycle(initialValue = emptyList())
    val mutableList = remember { mutableStateListOf(*equipmentList.toTypedArray()) }
    var isReorderMode by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val smoothDragOffset by animateFloatAsState(
        targetValue = if (draggingItemIndex >= 0) accumulatedDrag else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
    )

    LaunchedEffect(equipmentList, isReorderMode) {
        if (!isReorderMode) {
            mutableList.clear(); mutableList.addAll(equipmentList)
            draggingItemIndex = -1; accumulatedDrag = 0f
        }
    }

    var isAdding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingItem by remember { mutableStateOf<Equipment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    if (isReorderMode) Text("器具排序") else Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalCafe, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("器具管理")
                    }
                },
                actions = {
                    if (!isReorderMode && equipmentList.isNotEmpty()) {
                        IconButton(onClick = { isReorderMode = true }) {
                            Icon(Icons.Default.SwapVert, "排序", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    if (!isReorderMode) {
                        IconButton(onClick = { isAdding = true; newName = "" }) {
                            Icon(Icons.Default.Add, "添加", tint = MaterialTheme.colorScheme.onPrimary)
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
        if (equipmentList.isEmpty() && !isReorderMode && !isAdding) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(emoji = "🔧", message = "还没有器具", hint = "点击 + 添加")
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isAdding && !isReorderMode) {
                    item {
                        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newName, onValueChange = { newName = it },
                                    placeholder = { Text("器具名称") }, singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )
                                IconButton(onClick = {
                                    if (newName.isNotBlank()) { equipmentViewModel.addEquipment(newName.trim()); isAdding = false }
                                }) { Icon(Icons.Default.Check, "确认", tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { isAdding = false }) { Icon(Icons.Default.Close, "取消", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }

                if (isReorderMode) {
                    itemsIndexed(mutableList, key = { _, item -> item.id }) { index, item ->
                        val isDragging = draggingItemIndex == index
                        DraggableManagementItem(
                            name = item.name, isDragging = isDragging,
                            dragOffset = if (isDragging) smoothDragOffset else 0f,
                            onDragStart = { draggingItemIndex = index; accumulatedDrag = 0f },
                            onDrag = { dragAmount ->
                                accumulatedDrag += dragAmount.y
                                val info = lazyListState.layoutInfo
                                val myInfo = info.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }
                                if (myInfo != null) {
                                    val currentCenterY = myInfo.offset + myInfo.size / 2 + accumulatedDrag
                                    var targetIndex = draggingItemIndex
                                    info.visibleItemsInfo
                                        .filter { it.index != draggingItemIndex }
                                        .forEach { other ->
                                            val itemCenter = other.offset + other.size / 2
                                            if (accumulatedDrag > 0 && other.index > draggingItemIndex && currentCenterY > itemCenter) {
                                                targetIndex = maxOf(targetIndex, other.index)
                                            } else if (accumulatedDrag < 0 && other.index < draggingItemIndex && currentCenterY < itemCenter) {
                                                targetIndex = minOf(targetIndex, other.index)
                                            }
                                        }
                                    targetIndex = targetIndex.coerceIn(0, mutableList.lastIndex)
                                    if (targetIndex != draggingItemIndex) {
                                        val movedItem = mutableList.removeAt(draggingItemIndex)
                                        mutableList.add(targetIndex, movedItem)
                                        draggingItemIndex = targetIndex
                                        accumulatedDrag = 0f
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingItemIndex = -1; accumulatedDrag = 0f
                                equipmentViewModel.saveEquipmentOrder(mutableList.toList())
                                isReorderMode = false
                            }
                        )
                    }
                } else {
                    itemsIndexed(equipmentList, key = { _, item -> item.id }) { _, item ->
                        var editedName by remember(item.id) { mutableStateOf(item.name) }
                        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = editedName, onValueChange = { editedName = it },
                                    singleLine = true, modifier = Modifier.weight(1f).onFocusChanged { focus ->
                                        if (!focus.isFocused && editedName.isNotBlank() && editedName != item.name) {
                                            equipmentViewModel.updateEquipment(item.copy(name = editedName.trim()))
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(onClick = { deletingItem = item; showDeleteDialog = true }) {
                                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && deletingItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${deletingItem!!.name}」吗？") },
            confirmButton = { TextButton(onClick = { equipmentViewModel.deleteEquipment(deletingItem!!); showDeleteDialog = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }
}
