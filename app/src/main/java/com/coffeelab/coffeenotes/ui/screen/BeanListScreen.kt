package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.ui.component.BeanCard
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BeanListScreen(
    navController: NavController,
    viewModel: BeanViewModel = viewModel()
) {
    val beans by viewModel.allBeans.collectAsState(initial = emptyList())
    val mutableBeans = remember { mutableStateListOf(*beans.toTypedArray()) }
    var isReorderMode by remember { mutableStateOf(false) }

    // Keep mutableBeans in sync with beans when not in reorder mode
    LaunchedEffect(beans, isReorderMode) {
        if (!isReorderMode) {
            mutableBeans.clear()
            mutableBeans.addAll(beans)
        }
    }

    // 多选状态
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedBeans by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    // 拖拽状态
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // 显示的豆子（筛选收藏）
    val displayedBeans = if (showFavoritesOnly) beans.filter { it.isFavorite } else beans

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

    // 移动豆子（排序模式）
    fun moveBean(fromIndex: Int, toIndex: Int) {
        if (toIndex < 0 || toIndex >= mutableBeans.size) return
        val item = mutableBeans.removeAt(fromIndex)
        mutableBeans.add(toIndex, item)
    }

    // 保存排序
    fun saveOrder() {
        isReorderMode = false
        viewModel.saveBeanOrder(mutableBeans.toList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when {
                        isSelectionMode -> Text("${selectedBeans.size} 条已选")
                        isReorderMode -> Text("拖动排序")
                        else -> Text("🫘 我的豆子")
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
                        else -> IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (!isSelectionMode && !isReorderMode) {
                        IconButton(onClick = { isReorderMode = true }) {
                            Icon(Icons.Default.SwapVert, contentDescription = "排序")
                        }
                        IconButton(onClick = { navController.navigate(Screen.BeanEdit.createRoute()) }) {
                            Icon(Icons.Default.Add, contentDescription = "添加豆子")
                        }
                    }
                    if (isReorderMode) {
                        IconButton(onClick = { saveOrder() }) {
                            Icon(Icons.Default.Check, contentDescription = "完成排序")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSelectionMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    titleContentColor = if (isSelectionMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = if (isSelectionMode || isReorderMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = if (isSelectionMode || isReorderMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
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
                    "还没有咖啡豆，点击 + 添加吧 🫘",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val listState = rememberLazyListState()
            val items = if (isReorderMode) mutableBeans else displayedBeans

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                state = listState,
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
                                selected = !showFavoritesOnly,
                                onClick = { showFavoritesOnly = false },
                                label = { Text("全部") }
                            )
                            FilterChip(
                                selected = showFavoritesOnly,
                                onClick = { showFavoritesOnly = true },
                                label = { Text("❤ 收藏") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer
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
                                    if (showFavoritesOnly) "还没有收藏的豆子 🫑" else "还没有咖啡豆，点击 + 添加吧 🫘",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                itemsIndexed(if (isReorderMode) mutableBeans else displayedBeans) { index, bean ->
                    if (isReorderMode) {
                        ReorderableBeanItem(
                            bean = bean,
                            index = index,
                            totalCount = mutableBeans.size,
                            onMoveUp = { if (index > 0) moveBean(index, index - 1) },
                            onMoveDown = { if (index < mutableBeans.size - 1) moveBean(index, index + 1) }
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
                            }
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
                Text("确定要删除这 ${selectedBeans.size} 条豆子吗？\n该豆子的所有冲煮记录也会被删除。")
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
}

@Composable
private fun ReorderableBeanItem(
    bean: CoffeeBean,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "拖动排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
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
            IconButton(
                onClick = onMoveUp,
                enabled = index > 0
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "上移",
                    tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = index < totalCount - 1
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "下移",
                    tint = if (index < totalCount - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}
