package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.ui.component.rememberDragReorderState
import com.coffeelab.coffeenotes.data.Converters
import com.coffeelab.coffeenotes.data.entity.BrewMethod
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BrewMethodViewModel

/** 步骤摘要：durationSeconds 是累计秒数（步骤结束时刻），不是单段时长 */
private fun stepSummary(stepsJson: String?): String =
    Converters.parseSteps(stepsJson).withIndex().joinToString(" → ") { (i, step) ->
        val water = step.waterAmount?.let { "${it}ml" } ?: "至总水量"
        "${i + 1}: ${water}/${step.durationSeconds}s"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewMethodListScreen(
    navController: NavController,
    viewModel: BrewMethodViewModel = viewModel()
) {
    val methods by viewModel.allMethods.collectAsStateWithLifecycle(initialValue = emptyList())
    val mutableList = remember { mutableStateListOf(*methods.toTypedArray()) }
    var isReorderMode by remember { mutableStateOf(false) }
    // 拖拽状态 — 使用 LazyListState 精确追踪位置
    val lazyListState = rememberLazyListState()
    // 拖拽状态机已抽到 rememberDragReorderState（原先此文件与另三处各写一遍且已漂移）
    val dragState = rememberDragReorderState(lazyListState, mutableList) { ordered ->
        isReorderMode = false
        viewModel.saveMethodOrder(ordered)
    }

    // Keep mutableList in sync with methods when not in reorder mode
    LaunchedEffect(methods, isReorderMode) {
        if (!isReorderMode) {
            mutableList.clear()
            mutableList.addAll(methods)
        }
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
                    val isDraggingThisItem = dragState.draggingItemIndex == index
                    DraggableMethodItem(
                        method = method,
                        isDragging = isDraggingThisItem,
                        dragOffset = if (isDraggingThisItem) dragState.smoothDragOffset else 0f,
                        onDragStart = { dragState.onDragStart(index) },
                        onDrag = { dragState.onDrag(it.y) },
                        onDragEnd = { dragState.onDragEnd() }
                    )
                }
            } else {
                if (methods.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "还没有冲煮手法，点击右上角 + 新建",
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
                Text(
                    stepSummary(method.steps),
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
                Text(
                    stepSummary(method.steps),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
