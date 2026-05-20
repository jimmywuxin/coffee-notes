package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.ui.component.BeanCard
import com.coffeelab.coffeenotes.ui.component.EmptyState
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BeanListScreen(
    navController: NavController,
    viewModel: BeanViewModel = viewModel()
) {
    val beans by viewModel.activeBeans.collectAsState(initial = emptyList())
    val archivedBeans by viewModel.archivedBeans.collectAsState(initial = emptyList())
    val mutableBeans = remember { mutableStateListOf(*beans.toTypedArray()) }
    var isReorderMode by remember { mutableStateOf(false) }
    var showArchivedOnly by remember { mutableStateOf(false) }

    // 拖拽状态 — 使用 LazyListState 精确追踪位置
    val lazyListState = rememberLazyListState()
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var dragStartItemOffset by remember { mutableFloatStateOf(0f) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val smoothDragOffset by animateFloatAsState(
        targetValue = accumulatedDrag,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
    )
    val coroutineScope = rememberCoroutineScope()

    // Keep mutableBeans in sync with beans when not in reorder mode
    LaunchedEffect(beans, isReorderMode) {
        if (!isReorderMode) {
            mutableBeans.clear()
            mutableBeans.addAll(beans)
            draggingItemIndex = -1
            accumulatedDrag = 0f
        }
    }

    // 多选状态
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedBeans by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUnarchiveDialog by remember { mutableStateOf<CoffeeBean?>(null) }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    // 显示的豆子（三态筛选：全部 / 收藏 / 归档）
    val displayedBeans = when {
        showArchivedOnly -> archivedBeans
        showFavoritesOnly -> beans.filter { it.isFavorite }
        else -> beans
    }

    // 进入多选模式
    fun enterSelectionMode(bean: CoffeeBean) {
        isSelectionMode = true
        selectedBeans = setOf(bean.id)
    }

    // 切换选中状态
    fun toggleSelection(beanId: Long) {
        selectedBeans = if (selectedBeans.contains(beanId)) {
            selectedBeans - beanId
        } else {
            selectedBeans + beanId
        }
        if (selectedBeans.isEmpty()) {
            isSelectionMode = false
        }
    }

    // 全选/取消全选
    fun toggleSelectAll() {
        if (selectedBeans.size == beans.size) {
            selectedBeans = emptySet()
            isSelectionMode = false
        } else {
            selectedBeans = beans.map { it.id }.toSet()
        }
    }

    // 退出多选模式
    fun exitSelectionMode() {
        isSelectionMode = false
        selectedBeans = emptySet()
    }

    // 删除所选
    fun deleteSelected() {
        showDeleteDialog = true
    }

    // 保存排序
    fun saveOrder() {
        isReorderMode = false
        draggingItemIndex = -1
        accumulatedDrag = 0f
        viewModel.saveBeanOrder(mutableBeans.toList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when {
                        isSelectionMode -> Text("${selectedBeans.size} 条已选")
                        isReorderMode -> Text("拖动排序")
                        else -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Grain, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("我的豆子") }
                    }
                },
                navigationIcon = {
                    when {
                        isSelectionMode -> IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                        isReorderMode -> IconButton(onClick = {
                            isReorderMode = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                        else -> { /* 无返回按钮，主页由底部导航切换 */ }
                    }
                },
                actions = {
                    if (!isSelectionMode && !isReorderMode) {
                        IconButton(onClick = { navController.navigate(Screen.Search.createRoute("beans")) }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        IconButton(onClick = { isReorderMode = true }) {
                            Icon(Icons.Default.SwapVert, contentDescription = "排序")
                        }
                        IconButton(onClick = { navController.navigate(Screen.BeanEdit.createRoute()) }) {
                            Icon(Icons.Default.Add, contentDescription = "添加豆子")
                        }
                    }
                    if (isReorderMode) {
                        // 松手即完成，不再需要完成按钮
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = when {
                        isSelectionMode -> MaterialTheme.colorScheme.primaryContainer
                        isReorderMode -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    titleContentColor = when {
                        isSelectionMode -> MaterialTheme.colorScheme.onPrimaryContainer
                        isReorderMode -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onPrimary
                    },
                    navigationIconContentColor = when {
                        isSelectionMode -> MaterialTheme.colorScheme.onPrimaryContainer
                        isReorderMode -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onPrimary
                    },
                    actionIconContentColor = when {
                        isSelectionMode -> MaterialTheme.colorScheme.onPrimaryContainer
                        isReorderMode -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onPrimary
                    }
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode && selectedBeans.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { toggleSelectAll() }) {
                                Icon(Icons.Default.SelectAll, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("全选")
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "已选 ${selectedBeans.size} 条",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = { deleteSelected() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("删除")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (beans.isEmpty() && !isReorderMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有咖啡豆\n点击 + 开始记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (!isReorderMode) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !showFavoritesOnly && !showArchivedOnly,
                                onClick = {
                                    showFavoritesOnly = false
                                    showArchivedOnly = false
                                },
                                label = { Text("全部") }
                            )
                            FilterChip(
                                selected = showFavoritesOnly,
                                onClick = {
                                    showFavoritesOnly = true
                                    showArchivedOnly = false
                                },
                                label = { Text("❤ 收藏") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            )
                            FilterChip(
                                selected = showArchivedOnly,
                                onClick = {
                                    showArchivedOnly = !showArchivedOnly
                                    if (showArchivedOnly) showFavoritesOnly = false
                                },
                                label = { Text("📁 归档") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }

                    if (displayedBeans.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (showArchivedOnly) "还没有归档的豆子"
                                else if (showFavoritesOnly) "还没有收藏的豆子"
                                else "还没有咖啡豆\n点击 + 开始记录",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                itemsIndexed(
                    if (isReorderMode) mutableBeans else displayedBeans,
                    key = { index, bean -> if (isReorderMode) mutableBeans[index].id else bean.id }
                ) { index, bean ->
                    val isDraggingThisItem = isReorderMode && draggingItemIndex == index
                    if (isReorderMode) {
                        DraggableBeanItem(
                            bean = bean,
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
                                    targetIndex = targetIndex.coerceIn(0, mutableBeans.lastIndex)
                                    if (targetIndex != draggingItemIndex) {
                                        val item = mutableBeans.removeAt(draggingItemIndex)
                                        mutableBeans.add(targetIndex, item)
                                        draggingItemIndex = targetIndex
                                        accumulatedDrag = 0f
                                        val newLayoutInfo = lazyListState.layoutInfo
                                        val newInfo = newLayoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                                        dragStartItemOffset = newInfo?.offset?.toFloat() ?: 0f
                                    }

                                    // Auto-scroll when dragged item nears top/bottom edge
                                    val viewHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                                    val itemTop = myInfo.offset + accumulatedDrag
                                    val itemBottom = itemTop + myInfo.size
                                    val edgeThreshold = viewHeight * 0.15f
                                    if (itemTop < edgeThreshold) {
                                        val scrollTarget = (draggingItemIndex - 1).coerceAtLeast(0)
                                        coroutineScope.launch { lazyListState.animateScrollToItem(scrollTarget) }
                                    } else if (itemBottom > viewHeight - edgeThreshold) {
                                        val scrollTarget = (draggingItemIndex + 1).coerceAtMost(mutableBeans.lastIndex)
                                        coroutineScope.launch { lazyListState.animateScrollToItem(scrollTarget) }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingItemIndex = -1
                                accumulatedDrag = 0f
                                viewModel.saveBeanOrder(mutableBeans.toList())
                                isReorderMode = false
                            }
                        )
                    } else {
                        val isSelected = selectedBeans.contains(bean.id)
                        BeanCard(
                            bean = bean,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    toggleSelection(bean.id)
                                } else {
                                    navController.navigate(Screen.BeanDetail.createRoute(bean.id))
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    enterSelectionMode(bean)
                                }
                            },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(bean)
                            },
                            onUnarchiveClick = if (showArchivedOnly) {
                                { showUnarchiveDialog = bean }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // 批量删除确认弹窗
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = {
                Text("确定要删除这 ${selectedBeans.size} 条豆子吗？\n所有冲煮记录也会被删除，且无法恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        beans.filter { selectedBeans.contains(it.id) }.forEach {
                            viewModel.deleteBean(it)
                        }
                        showDeleteDialog = false
                        exitSelectionMode()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    // 取消归档确认弹窗
    showUnarchiveDialog?.let { bean ->
        AlertDialog(
            onDismissRequest = { showUnarchiveDialog = null },
            title = { Text("取消归档") },
            text = { Text("将「${bean.name}」恢复到豆子列表？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unarchiveBean(bean)
                        showUnarchiveDialog = null
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showUnarchiveDialog = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun DraggableBeanItem(
    bean: CoffeeBean,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "拖动排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bean.name.ifEmpty { "(未命名)" },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${bean.roaster} ${if (bean.origin.isNotEmpty()) "· ${bean.origin}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
