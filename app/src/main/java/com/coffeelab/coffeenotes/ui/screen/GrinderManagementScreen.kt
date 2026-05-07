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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Check
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
import com.coffeelab.coffeenotes.data.entity.Grinder
import com.coffeelab.coffeenotes.viewmodel.GrinderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrinderManagementScreen(
    navController: NavController,
    grinderViewModel: GrinderViewModel = viewModel()
) {
    val grinderList by grinderViewModel.allGrinders.collectAsState(initial = emptyList())
    val mutableList = remember { mutableStateListOf(*grinderList.toTypedArray()) }
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

    LaunchedEffect(grinderList, isReorderMode) {
        if (!isReorderMode) {
            mutableList.clear()
            mutableList.addAll(grinderList)
            draggingItemIndex = -1
            accumulatedDrag = 0f
        }
    }

    fun saveOrder() {
        isReorderMode = false
        draggingItemIndex = -1
        accumulatedDrag = 0f
        grinderViewModel.saveGrinderOrder(mutableList.toList())
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingGrinder by remember { mutableStateOf<Grinder?>(null) }
    var newGrinderName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isReorderMode) Text("拖动排序") else Text("磨豆机管理")
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
                    if (!isReorderMode && grinderList.isNotEmpty()) {
                        IconButton(onClick = { isReorderMode = true }) {
                            Icon(Icons.Default.SwapVert, contentDescription = "排序", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    if (!isReorderMode) {
                        IconButton(onClick = {
                            newGrinderName = ""
                            showAddDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "添加磨豆机", tint = MaterialTheme.colorScheme.onPrimary)
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
        if (grinderList.isEmpty() && !isReorderMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有磨豆机\n点击右下角 + 添加",
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
                    itemsIndexed(mutableList, key = { _, item -> item.id }) { index, grinder ->
                        val isDraggingThisItem = isReorderMode && draggingItemIndex == index
                        DraggableGrinderItem(
                            grinder = grinder,
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
                                grinderViewModel.saveGrinderOrder(mutableList.toList())
                                isReorderMode = false
                            }
                        )
                    }
                } else {
                    itemsIndexed(grinderList, key = { _, item -> item.id }) { _, grinder ->
                        GrinderItem(
                            grinder = grinder,
                            onEdit = {
                                editingGrinder = grinder
                                newGrinderName = grinder.name
                                showEditDialog = true
                            },
                            onDelete = {
                                editingGrinder = grinder
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Grinder Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加磨豆机") },
            text = {
                OutlinedTextField(
                    value = newGrinderName,
                    onValueChange = { newGrinderName = it },
                    label = { Text("磨豆机名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGrinderName.isNotBlank()) {
                            grinderViewModel.addGrinder(newGrinderName.trim())
                            showAddDialog = false
                        }
                    },
                    enabled = newGrinderName.isNotBlank()
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            }
        )
    }

    // Edit Grinder Dialog
    if (showEditDialog && editingGrinder != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("编辑磨豆机") },
            text = {
                OutlinedTextField(
                    value = newGrinderName,
                    onValueChange = { newGrinderName = it },
                    label = { Text("磨豆机名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGrinderName.isNotBlank()) {
                            editingGrinder?.let {
                                grinderViewModel.updateGrinder(
                                    it.copy(name = newGrinderName.trim())
                                )
                            }
                            showEditDialog = false
                        }
                    },
                    enabled = newGrinderName.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("取消") }
            }
        )
    }

    // Delete Grinder Dialog
    if (showDeleteDialog && editingGrinder != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${editingGrinder?.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingGrinder?.let { grinderViewModel.deleteGrinder(it) }
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
fun GrinderItem(
    grinder: Grinder,
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
                text = grinder.name,
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

@Composable
private fun DraggableGrinderItem(
    grinder: Grinder,
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
                text = grinder.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
