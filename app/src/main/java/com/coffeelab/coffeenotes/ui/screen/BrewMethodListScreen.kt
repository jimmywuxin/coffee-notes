package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.coffeelab.coffeenotes.data.Converters
import com.coffeelab.coffeenotes.data.entity.BrewMethod
import com.coffeelab.coffeenotes.ui.component.EmptyState
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BrewMethodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewMethodListScreen(
    navController: NavController,
    viewModel: BrewMethodViewModel = viewModel()
) {
    val methods by viewModel.allMethods.collectAsState(initial = emptyList())
    val mutableList = remember { mutableStateListOf(*methods.toTypedArray()) }
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

    // Keep mutableList in sync with methods when not in reorder mode
    LaunchedEffect(methods, isReorderMode) {
        if (!isReorderMode) {
            mutableList.clear()
            mutableList.addAll(methods)
            draggingItemIndex = -1
            accumulatedDrag = 0f
        }
    }

    fun saveOrder() {
        isReorderMode = false
        draggingItemIndex = -1
        accumulatedDrag = 0f
        viewModel.saveMethodOrder(mutableList.toList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    if (isReorderMode) Text("冲煮手法排序") else Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountTree, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("冲煮手法")
                    }
                },
                actions = {
                    if (!isReorderMode && methods.isNotEmpty()) {
                        IconButton(onClick = { isReorderMode = true }) {
                            Icon(Icons.Default.SwapVert, contentDescription = "排序", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    if (!isReorderMode) {
                        IconButton(onClick = { navController.navigate(Screen.BrewMethodEdit.createRoute()) }) {
                            Icon(Icons.Default.Add, contentDescription = "新建手法", tint = MaterialTheme.colorScheme.onPrimary)
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
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isReorderMode) {
                itemsIndexed(mutableList, key = { _, item -> item.id }) { index, method ->
                    val isDraggingThisItem = isReorderMode && draggingItemIndex == index
                    DraggableMethodItem(
                        method = method,
                        isDragging = isDraggingThisItem,
                        dragOffset = if (isDraggingThisItem) smoothDragOffset else 0f,
                        onDragStart = {
                            draggingItemIndex = index
                            accumulatedDrag = 0f
                            // 使用 layoutInfo 获取 item 在列表中的真实像素位置
                            val layoutInfo = lazyListState.layoutInfo
                            val myInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                            dragStartItemOffset = myInfo?.offset?.toFloat() ?: 0f
                        },
                        onDrag = { dragAmount ->
                            accumulatedDrag += dragAmount.y
                            // 当前 item 中心在列表坐标系中的位置（含拖动偏移）
                            val layoutInfo = lazyListState.layoutInfo
                            val myInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }
                            if (myInfo != null) {
                                val currentCenterY = myInfo.offset + myInfo.size / 2 + accumulatedDrag
                                // 与相邻 item 中心比较，确定目标位置
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
                                    // 重置拖动偏移，避免 item 闪跳
                                    accumulatedDrag = 0f
                                    // 更新起始偏移为 item 的新位置
                                    val newLayoutInfo = lazyListState.layoutInfo
                                    val newInfo = newLayoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                                    dragStartItemOffset = newInfo?.offset?.toFloat() ?: 0f
                                }
                            }
                        },
                        onDragEnd = {
                            draggingItemIndex = -1
                            accumulatedDrag = 0f
                            viewModel.saveMethodOrder(mutableList.toList())
                            isReorderMode = false
                        }
                    )
                }
            } else {
                // 新建手法引导卡片（常驻顶部，统一新建入口视觉位置）
                item {
                    Surface(
                        onClick = { navController.navigate(Screen.BrewMethodEdit.createRoute()) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("新建手法", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (methods.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "还没有冲煮手法，点击上方卡片新建",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(methods, key = { _, item -> item.id }) { _, method ->
                        MethodCard(
                            method = method,
                            onClick = {
                                navController.navigate(Screen.BrewMethodEdit.createRoute(method.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableMethodItem(
    method: BrewMethod,
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
                    onDragStart = { offset -> onDragStart() },
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
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "拖动排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(method.name, style = MaterialTheme.typography.titleMedium)
                val parsedSteps = Converters.parseSteps(method.steps)
                val stepSummary = parsedSteps.withIndex().joinToString(" → ") { (i, step) ->
                    val water = step.waterAmount?.let { "${it}ml" } ?: "至总水量"
                    "${i + 1}: ${water}/${step.durationSeconds}s"
                }
                Text(
                    stepSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MethodCard(
    method: BrewMethod,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(method.name, style = MaterialTheme.typography.titleMedium)
                val parsedSteps = Converters.parseSteps(method.steps)
                val stepSummary = parsedSteps.withIndex().joinToString(" → ") { (i, step) ->
                    val water = step.waterAmount?.let { "${it}ml" } ?: "至总水量"
                    "${i + 1}: ${water}/${step.durationSeconds}s"
                }
                Text(
                    stepSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
