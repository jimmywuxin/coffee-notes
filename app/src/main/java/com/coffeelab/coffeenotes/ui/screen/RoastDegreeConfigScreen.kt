package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.ui.component.DraggableManagementItem
import com.coffeelab.coffeenotes.ui.component.EmptyState
import com.coffeelab.coffeenotes.viewmodel.RoastDegreeConfigItem
import com.coffeelab.coffeenotes.viewmodel.RoastDegreeConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoastDegreeConfigScreen(
    navController: NavController,
    viewModel: RoastDegreeConfigViewModel = viewModel()
) {
    val items by viewModel.configs.collectAsStateWithLifecycle(initialValue = emptyList())
    val mutableList = remember { mutableStateListOf<RoastDegreeConfigItem>() }
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

    // Add inline
    var isAdding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // Delete
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingItem by remember { mutableStateOf<RoastDegreeConfigItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    if (isReorderMode) Text("烘焙度排序")
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("烘焙度配置")
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
        if (items.isEmpty() && !isReorderMode) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(emoji = "🔥", message = "暂无烘焙度", hint = "点击 + 添加")
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add inline row
                if (isAdding && !isReorderMode) {
                    item {
                        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    placeholder = { Text("烘焙度名称") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                IconButton(onClick = {
                                    if (newName.isNotBlank()) {
                                        viewModel.addDegree(newName.trim())
                                        isAdding = false
                                    }
                                }) {
                                    Icon(Icons.Default.Check, "确认", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { isAdding = false }) {
                                    Icon(Icons.Default.Close, "取消", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (isReorderMode) {
                    itemsIndexed(mutableList, key = { _, item -> item.degree.id }) { index, item ->
                        val isDragging = draggingIndex == index
                        DraggableManagementItem(
                            name = item.degree.name,
                            isDragging = isDragging,
                            dragOffset = if (isDragging) smoothDragOffset else 0f,
                            onDragStart = { draggingIndex = index; accumulatedDrag = 0f },
                            onDrag = { dragAmount ->
                                accumulatedDrag += dragAmount.y
                                val info = lazyListState.layoutInfo
                                val myInfo = info.visibleItemsInfo.firstOrNull { it.index == draggingIndex } ?: return@DraggableManagementItem
                                val currentCenterY = myInfo.offset + myInfo.size / 2 + accumulatedDrag
                                var targetIndex = draggingIndex
                                info.visibleItemsInfo
                                    .filter { it.index != draggingIndex }
                                    .forEach { other ->
                                        val itemCenter = other.offset + other.size / 2
                                        if (accumulatedDrag > 0 && other.index > draggingIndex && currentCenterY > itemCenter) {
                                            targetIndex = maxOf(targetIndex, other.index)
                                        } else if (accumulatedDrag < 0 && other.index < draggingIndex && currentCenterY < itemCenter) {
                                            targetIndex = minOf(targetIndex, other.index)
                                        }
                                    }
                                targetIndex = targetIndex.coerceIn(0, mutableList.lastIndex)
                                if (targetIndex != draggingIndex) {
                                    val movedItem = mutableList.removeAt(draggingIndex)
                                    mutableList.add(targetIndex, movedItem)
                                    draggingIndex = targetIndex
                                    accumulatedDrag = 0f
                                }
                            },
                            onDragEnd = {
                                draggingIndex = -1; accumulatedDrag = 0f
                                viewModel.saveOrder(mutableList.map { it.degree })
                                isReorderMode = false
                            }
                        )
                    }
                } else {
                    // Header
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("烘焙度", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("养豆(天)", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Text("赏味(天)", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(Modifier.width(40.dp))
                        }
                    }

                    itemsIndexed(items, key = { _, item -> item.degree.id }) { _, item ->
                        var editedName by remember(item.degree.id) { mutableStateOf(item.degree.name) }
                        var restDaysStr by remember(item.restDays) { mutableStateOf(item.restDays.toString()) }
                        var peakDaysStr by remember(item.peakFlavorDays) { mutableStateOf(item.peakFlavorDays.toString()) }

                        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Name
                                OutlinedTextField(
                                    value = editedName,
                                    onValueChange = { editedName = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).onFocusChanged { focus ->
                                        if (!focus.isFocused && editedName.isNotBlank() && editedName != item.degree.name) {
                                            viewModel.renameDegree(item.degree, editedName.trim())
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge
                                )

                                // Rest days
                                OutlinedTextField(
                                    value = restDaysStr,
                                    onValueChange = { v ->
                                        restDaysStr = v.filter { it.isDigit() }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(72.dp).onFocusChanged { focus ->
                                        if (!focus.isFocused) {
                                            val days = restDaysStr.toIntOrNull() ?: item.restDays
                                            if (days != item.restDays) {
                                                viewModel.updateRestDays(item.degree.id, days)
                                                restDaysStr = days.toString()
                                            }
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
                                )

                                // Peak flavor days
                                OutlinedTextField(
                                    value = peakDaysStr,
                                    onValueChange = { v ->
                                        peakDaysStr = v.filter { it.isDigit() }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(72.dp).onFocusChanged { focus ->
                                        if (!focus.isFocused) {
                                            val days = peakDaysStr.toIntOrNull() ?: item.peakFlavorDays
                                            if (days != item.peakFlavorDays) {
                                                viewModel.updatePeakFlavorDays(item.degree.id, days)
                                                peakDaysStr = days.toString()
                                            }
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
                                )

                                // Delete
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
            text = { Text("确定要删除「${deletingItem!!.degree.name}」吗？相关的养豆/赏味配置也会被删除。") },
            confirmButton = { TextButton(onClick = { viewModel.deleteDegree(deletingItem!!.degree); showDeleteDialog = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }
}
