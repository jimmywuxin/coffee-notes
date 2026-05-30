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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.AppDatabase
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
    val showArchivedOnly by viewModel.showArchivedOnly.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()

    // ===== Search state =====
    var isSearchMode by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState(initial = emptyList())
    val isSearching by viewModel.isSearching.collectAsState(initial = false)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        } else {
            viewModel.clearSearch()
        }
    }

    // Load impression tags for all displayed beans
    val context = LocalContext.current
    var beanImpressionTagsMap by remember { mutableStateOf<Map<Long, List<String>>>(emptyMap()) }
    LaunchedEffect(beans) {
        val dao = AppDatabase.getInstance(context).impressionTagDao()
        val map = mutableMapOf<Long, List<String>>()
        for (bean in beans) {
            val tags = dao.getTagsForBeanOnce(bean.id)
            if (tags.isNotEmpty()) {
                map[bean.id] = tags.map { it.name }
            }
        }
        beanImpressionTagsMap = map
    }

    // Drag state
    val lazyListState = rememberLazyListState()
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var dragStartItemOffset by remember { mutableFloatStateOf(0f) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val smoothDragOffset by animateFloatAsState(
        targetValue = accumulatedDrag,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(beans, isReorderMode) {
        if (!isReorderMode) {
            mutableBeans.clear()
            mutableBeans.addAll(beans)
            draggingItemIndex = -1
            accumulatedDrag = 0f
        }
    }

    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedBeans by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUnarchiveDialog by remember { mutableStateOf<CoffeeBean?>(null) }

    // Displayed beans
    val displayedBeans = when {
        isSearching -> searchResults
        showArchivedOnly -> archivedBeans
        showFavoritesOnly -> beans.filter { it.isFavorite }
        else -> beans
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    when {
                        isSelectionMode -> Text("${selectedBeans.size} 条已选")
                        isReorderMode -> Text("拖动排序")
                        else -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Grain, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("我的豆子")
                        }
                    }
                },
                navigationIcon = {
                    when {
                        isSelectionMode -> IconButton(onClick = { isSelectionMode = false; selectedBeans = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                        isReorderMode -> IconButton(onClick = { isReorderMode = false }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                        else -> {}
                    }
                },
                actions = {
                    if (!isSelectionMode && !isReorderMode) {
                        IconButton(onClick = { isSearchMode = !isSearchMode }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        IconButton(onClick = { isReorderMode = true }) {
                            Icon(Icons.Default.SwapVert, contentDescription = "排序")
                        }
                        IconButton(onClick = { navController.navigate(Screen.BeanEdit.createRoute()) }) {
                            Icon(Icons.Default.Add, contentDescription = "添加豆子")
                        }
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                if (selectedBeans.size == beans.size) {
                                    selectedBeans = emptySet()
                                    isSelectionMode = false
                                } else {
                                    selectedBeans = beans.map { it.id }.toSet()
                                }
                            }) {
                                Icon(Icons.Default.SelectAll, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("全选")
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("已选 ${selectedBeans.size} 条", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ===== Search bar =====
            AnimatedVisibility(
                visible = isSearchMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("搜索烘焙商、豆名、产地、处理法...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Default.Close, contentDescription = "清除")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            if (beans.isEmpty() && !isReorderMode && !isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有咖啡豆\n点击 + 开始记录", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (!isReorderMode) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !showFavoritesOnly && !showArchivedOnly && !isSearching,
                                    onClick = { viewModel.setShowFavoritesOnly(false); viewModel.setShowArchivedOnly(false) },
                                    label = { Text("全部") }
                                )
                                FilterChip(
                                    selected = showFavoritesOnly && !isSearching,
                                    onClick = { viewModel.setShowFavoritesOnly(true); viewModel.setShowArchivedOnly(false) },
                                    label = { Text("❤ 收藏") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)
                                )
                                FilterChip(
                                    selected = showArchivedOnly && !isSearching,
                                    onClick = {
                                        viewModel.setShowArchivedOnly(!showArchivedOnly)
                                        if (!showArchivedOnly) viewModel.setShowFavoritesOnly(false)
                                    },
                                    label = { Text("📁 归档") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer)
                                )
                            }
                        }

                        if (displayedBeans.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        if (isSearching) "没有找到匹配的豆子"
                                        else if (showArchivedOnly) "还没有归档的豆子"
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
                                    dragStartItemOffset = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.offset?.toFloat() ?: 0f
                                },
                                onDrag = { dragAmount ->
                                    accumulatedDrag += dragAmount.y
                                    val myInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }
                                    if (myInfo != null) {
                                        val currentCenterY = myInfo.offset + myInfo.size / 2 + accumulatedDrag
                                        val targetIndex = lazyListState.layoutInfo.visibleItemsInfo
                                            .minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - currentCenterY) }?.index
                                        if (targetIndex != null && targetIndex != draggingItemIndex) {
                                            val moved = mutableBeans.removeAt(draggingItemIndex)
                                            mutableBeans.add(targetIndex, moved)
                                            draggingItemIndex = targetIndex
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingItemIndex = -1
                                    accumulatedDrag = 0f
                                    isReorderMode = false
                                    viewModel.saveBeanOrder(mutableBeans.toList())
                                }
                            )
                        } else {
                            BeanCard(
                                bean = bean,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedBeans.contains(bean.id),
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedBeans = if (selectedBeans.contains(bean.id)) selectedBeans - bean.id else selectedBeans + bean.id
                                        if (selectedBeans.isEmpty()) isSelectionMode = false
                                    } else {
                                        navController.navigate(Screen.BeanDetail.createRoute(bean.id))
                                    }
                                },
                                onLongClick = { isSelectionMode = true; selectedBeans = setOf(bean.id) },
                                onUnarchiveClick = if (showArchivedOnly) {{ showUnarchiveDialog = bean }} else null,
                                impressionTags = beanImpressionTagsMap[bean.id] ?: emptyList()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这 ${selectedBeans.size} 条豆子吗？\n所有冲煮记录也会被删除，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    beans.filter { selectedBeans.contains(it.id) }.forEach { viewModel.deleteBean(it) }
                    showDeleteDialog = false
                    isSelectionMode = false
                    selectedBeans = emptySet()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }

    showUnarchiveDialog?.let { bean ->
        AlertDialog(
            onDismissRequest = { showUnarchiveDialog = null },
            title = { Text("取消归档") },
            text = { Text("将「${bean.name}」恢复到豆子列表？") },
            confirmButton = {
                TextButton(onClick = { viewModel.unarchiveBean(bean); showUnarchiveDialog = null }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showUnarchiveDialog = null }) { Text("取消") } }
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
    val animatedElevation by animateFloatAsState(targetValue = if (isDragging) 12f else 0f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f), label = "elev")
    val animatedScale by animateFloatAsState(targetValue = if (isDragging) 1.03f else 1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f), label = "scale")
    val animatedDragOffset by animateFloatAsState(targetValue = if (isDragging) dragOffset else 0f, animationSpec = spring(dampingRatio = 0.4f, stiffness = 150f), label = "drag")
    val animatedBgColor by animateColorAsState(targetValue = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, animationSpec = spring(dampingRatio = 0.5f, stiffness = 150f), label = "bg")

    Surface(
        modifier = modifier.fillMaxWidth().graphicsLayer {
            alpha = if (isDragging) 0.95f else 1f
            shadowElevation = animatedElevation
            translationY = animatedDragOffset
            scaleX = animatedScale
            scaleY = animatedScale
        }.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { onDragStart() },
                onDrag = { change, dragAmount -> change.consume(); onDrag(dragAmount) },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragEnd() }
            )
        },
        color = animatedBgColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DragHandle, contentDescription = "拖动排序", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = bean.name.ifEmpty { "(未命名)" }, style = MaterialTheme.typography.bodyLarge)
                Text(text = "${bean.roaster} ${if (bean.origin.isNotEmpty()) "· ${bean.origin}" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
