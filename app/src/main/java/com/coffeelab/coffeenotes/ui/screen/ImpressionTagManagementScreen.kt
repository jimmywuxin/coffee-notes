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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.ImpressionTag
import com.coffeelab.coffeenotes.ui.component.EmptyState
import com.coffeelab.coffeenotes.viewmodel.ImpressionTagViewModel
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpressionTagManagementScreen(
    navController: NavController,
    viewModel: ImpressionTagViewModel = viewModel()
) {
    val items by viewModel.allImpressionTags.collectAsState(initial = emptyList())
    val mutableList = remember { mutableStateListOf<ImpressionTag>() }
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
            mutableList.clear()
            mutableList.addAll(items)
            draggingIndex = -1
            accumulatedDrag = 0f
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ImpressionTag?>(null) }
    var nameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isReorderMode) Text("印象标签排序")
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Label, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("印象标签管理")
                    }
                },
                actions = {
                    if (!isReorderMode && items.isNotEmpty()) {
                        IconButton(onClick = { isReorderMode = true }) {
                            Icon(Icons.Default.SwapVert, "排序", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    if (!isReorderMode) {
                        IconButton(onClick = { nameInput = ""; showAddDialog = true }) {
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
        if (items.isEmpty() && !isReorderMode) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    emoji = "💧",
                    message = "暂无印象标签",
                    hint = "点击右下角 + 添加"
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isReorderMode) {
                    itemsIndexed(mutableList, key = { _, item -> item.id }) { index, item ->
                        val isDragging = draggingIndex == index
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    if (isDragging) {
                                        shadowElevation = 12f; alpha = 0.95f; scaleX = 1.03f; scaleY = 1.03f
                                        translationY = smoothDragOffset
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggingIndex = index; accumulatedDrag = 0f },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            accumulatedDrag += dragAmount.y
                                            val layoutInfo = lazyListState.layoutInfo
                                            val myInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                            if (myInfo != null) {
                                                val centerY = myInfo.offset + myInfo.size / 2 + accumulatedDrag
                                                var target = index
                                                layoutInfo.visibleItemsInfo.filter { it.index != index }.forEach { info ->
                                                    val itemCenter = info.offset + info.size / 2
                                                    if (accumulatedDrag > 0 && info.index > index && centerY > itemCenter) target = max(target, info.index)
                                                    else if (accumulatedDrag < 0 && info.index < index && centerY < itemCenter) target = min(target, info.index)
                                                }
                                                target = target.coerceIn(0, mutableList.lastIndex)
                                                if (target != index) {
                                                    mutableList.removeAt(index); mutableList.add(target, item)
                                                    draggingIndex = target; accumulatedDrag = 0f
                                                }
                                            }
                                        },
                                        onDragEnd = { draggingIndex = -1; accumulatedDrag = 0f; viewModel.saveOrder(mutableList.toList()); isReorderMode = false },
                                        onDragCancel = { draggingIndex = -1; accumulatedDrag = 0f }
                                    )
                                },
                            color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DragHandle, "拖动", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                } else {
                    itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                Row {
                                    IconButton(onClick = { editingItem = item; nameInput = item.name; showEditDialog = true }) {
                                        Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { editingItem = item; showDeleteDialog = true }) {
                                        Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加印象标签") },
            text = { OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { if (nameInput.isNotBlank()) { viewModel.addTag(nameInput.trim()); showAddDialog = false } }, enabled = nameInput.isNotBlank()) { Text("添加") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }

    if (showEditDialog && editingItem != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("编辑印象标签") },
            text = { OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { if (nameInput.isNotBlank()) { viewModel.updateTag(editingItem!!.copy(name = nameInput.trim())); showEditDialog = false } }, enabled = nameInput.isNotBlank()) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("取消") } }
        )
    }

    if (showDeleteDialog && editingItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${editingItem!!.name}」吗？") },
            confirmButton = { TextButton(onClick = { viewModel.deleteTag(editingItem!!); showDeleteDialog = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }
}
