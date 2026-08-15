package com.coffeelab.coffeenotes.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 单名称字段管理屏通用框架：排序 / 拖拽 / 新增 / 内联改名 / 删除弹窗 全部内置。
 *
 * 适用于「名称 + sortOrder」型的纯名称管理页（器具 / 磨豆机 / 印象标签 / 处理法）。
 * 烘焙度（三字段）、冲煮手法（带 steps）结构不同，不适用本组件。
 *
 * @param items 数据流当前值（每次变化会同步到本地排序列表）
 * @param onRename 内联改名（失焦且值变化时触发）
 * @param onSaveOrder 拖拽排序结束回调（新顺序全量）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> SingleNameManagementScreen(
    title: String,
    sortTitle: String,
    addPlaceholder: String,
    emptyEmoji: String,
    emptyMessage: String,
    icon: ImageVector,
    items: List<T>,
    getId: (T) -> Long,
    getName: (T) -> String,
    onAdd: (String) -> Unit,
    onRename: (T, String) -> Unit,
    onDelete: (T) -> Unit,
    onSaveOrder: (List<T>) -> Unit
) {
    val mutableList = remember { items.toMutableStateList() }
    var isReorderMode by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val smoothDragOffset by animateFloatAsState(
        targetValue = if (draggingItemIndex >= 0) accumulatedDrag else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
    )

    LaunchedEffect(items, isReorderMode) {
        if (!isReorderMode) {
            mutableList.clear(); mutableList.addAll(items)
            draggingItemIndex = -1; accumulatedDrag = 0f
        }
    }

    var isAdding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingItem by remember { mutableStateOf<T?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    if (isReorderMode) Text(sortTitle) else Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(title)
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
                EmptyState(emoji = emptyEmoji, message = emptyMessage, hint = "点击 + 添加")
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
                                    placeholder = { Text(addPlaceholder) }, singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )
                                IconButton(onClick = {
                                    if (newName.isNotBlank()) { onAdd(newName.trim()); isAdding = false }
                                }) { Icon(Icons.Default.Check, "确认", tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { isAdding = false }) { Icon(Icons.Default.Close, "取消", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }

                if (isReorderMode) {
                    itemsIndexed(mutableList, key = { _, item -> getId(item) }) { index, item ->
                        val isDragging = draggingItemIndex == index
                        DraggableManagementItem(
                            name = getName(item), isDragging = isDragging,
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
                                onSaveOrder(mutableList.toList())
                                isReorderMode = false
                            }
                        )
                    }
                } else {
                    itemsIndexed(items, key = { _, item -> getId(item) }) { _, item ->
                        var editedName by remember(getId(item)) { mutableStateOf(getName(item)) }
                        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = editedName, onValueChange = { editedName = it },
                                    singleLine = true, modifier = Modifier.weight(1f).onFocusChanged { focus ->
                                        if (!focus.isFocused && editedName.isNotBlank() && editedName != getName(item)) {
                                            onRename(item, editedName.trim())
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
            text = { Text("确定要删除「${getName(deletingItem!!)}」吗？") },
            confirmButton = { TextButton(onClick = { onDelete(deletingItem!!); showDeleteDialog = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }
}
