package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.core.*
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.ProcessMethod
import com.coffeelab.coffeenotes.ui.component.EmptyState
import com.coffeelab.coffeenotes.viewmodel.ProcessMethodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessMethodManagementScreen(
    navController: NavController,
    viewModel: ProcessMethodViewModel = viewModel()
) {
    val items by viewModel.allProcessMethods.collectAsState(initial = emptyList())
    val mutableList = remember { mutableStateListOf<ProcessMethod>() }
    var isReorderMode by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val smoothDragOffset by animateFloatAsState(
        targetValue = if (draggingIndex >= 0) accumulatedDrag else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
    )

    LaunchedEffect(items, isReorderMode) {
        if (!isReorderMode) {
            mutableList.clear(); mutableList.addAll(items)
            draggingIndex = -1; accumulatedDrag = 0f
        }
    }

    var isAdding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingItem by remember { mutableStateOf<ProcessMethod?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isReorderMode) Text("处理法排序")
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("处理法管理")
                    }
                },
                actions = {
                    if (!isReorderMode && items.isNotEmpty()) {
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
        if (items.isEmpty() && !isReorderMode && !isAdding) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(emoji = "💧", message = "暂无处理法", hint = "点击 + 添加")
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isAdding && !isReorderMode) {
                    item {
                        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newName, onValueChange = { newName = it },
                                    placeholder = { Text("处理法名称") }, singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )
                                IconButton(onClick = {
                                    if (newName.isNotBlank()) { viewModel.addProcessMethod(newName.trim()); isAdding = false }
                                }) { Icon(Icons.Default.Check, "确认", tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { isAdding = false }) {
                                    Icon(Icons.Default.Close, "取消", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (isReorderMode) {
                    itemsIndexed(mutableList, key = { _, item -> item.id }) { index, item ->
                        val isDragging = draggingIndex == index
                        Surface(
                            modifier = Modifier.fillMaxWidth().graphicsLayer {
                                if (isDragging) { shadowElevation = 12f; alpha = 0.95f; scaleX = 1.03f; scaleY = 1.03f; translationY = smoothDragOffset }
                            }.pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggingIndex = index; accumulatedDrag = 0f },
                                    onDrag = { _, dragAmount ->
                                        accumulatedDrag += dragAmount.y
                                        val info = lazyListState.layoutInfo
                                        val myInfo = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return@detectDragGesturesAfterLongPress
                                        val centerY = myInfo.offset + myInfo.size / 2 + accumulatedDrag
                                        val target = info.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - centerY) }?.index ?: index
                                        if (target != index) { mutableList.removeAt(index); mutableList.add(target, item); draggingIndex = target; accumulatedDrag = 0f }
                                    },
                                    onDragEnd = { draggingIndex = -1; accumulatedDrag = 0f; viewModel.saveOrder(mutableList.toList()); isReorderMode = false },
                                    onDragCancel = { draggingIndex = -1; accumulatedDrag = 0f }
                                )
                            },
                            color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DragHandle, "拖动", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                } else {
                    itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                        var editedName by remember(item.id) { mutableStateOf(item.name) }
                        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = editedName, onValueChange = { editedName = it },
                                    singleLine = true, modifier = Modifier.weight(1f).onFocusChanged { focus ->
                                        if (!focus.isFocused && editedName.isNotBlank() && editedName != item.name) {
                                            viewModel.renameProcessMethod(item, editedName.trim())
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
            confirmButton = { TextButton(onClick = { viewModel.deleteProcessMethod(deletingItem!!); showDeleteDialog = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }
}
