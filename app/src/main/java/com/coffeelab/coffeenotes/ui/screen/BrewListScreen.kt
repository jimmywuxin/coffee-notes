package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.ui.component.RecordCard
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel

private const val FILTER_ALL = -1L
private const val FILTER_NO_EQUIPMENT = 0L // 器具 id 从 1 起，0 = 未设置

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrewListScreen(
    navController: NavController,
    beanId: Long,
    brewViewModel: BrewViewModel = viewModel(),
    beanViewModel: BeanViewModel = viewModel()
) {
    // ===== Search state =====
    var isSearchMode by remember { mutableStateOf(false) }
    val searchQuery by brewViewModel.searchQuery.collectAsStateWithLifecycle(initialValue = brewViewModel.searchQuery.value)
    val searchResults by brewViewModel.searchResults.collectAsStateWithLifecycle(initialValue = emptyList())
    val isSearching by brewViewModel.isSearching.collectAsStateWithLifecycle(initialValue = false)
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        } else {
            brewViewModel.clearSearch()
        }
    }

    val rawRecords by if (beanId > 0) {
        brewViewModel.loadRecordsForBean(beanId)
        brewViewModel.recordsForBean.collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        brewViewModel.allRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    }
    val records = if (isSearching) searchResults else rawRecords

    // Paging
    var visibleCount by remember { mutableIntStateOf(30) }
    val PAGE_SIZE = 30
    val beans by beanViewModel.allBeans.collectAsStateWithLifecycle(initialValue = emptyList())
    // 预构建 id→bean 映射，避免每条记录线性查找 beans（O(n×m) → O(n)）
    val beansById = remember(beans) { beans.associateBy { it.id } }

    // 豆种（按豆名，不显示烘焙商）/ 器具 / 星级筛选（仅全部记录视图，文字搜索时不叠加）
    // null = 全部
    var selectedBeanId by remember { mutableStateOf<Long?>(null) }
    var selectedEquipmentId by remember { mutableLongStateOf(FILTER_ALL) }
    var selectedRatingFilter by remember { mutableStateOf("全部") }
    val ratingFilters = listOf("全部", "三星以上", "四星以上", "五星")

    // 豆名优先取档案，档案已删则用记录里的快照
    val beanNamesById = remember(records, beansById) {
        records.associate { record ->
            record.beanId to (beansById[record.beanId]?.name?.takeIf { it.isNotBlank() }
                ?: record.beanName.ifEmpty { "未知豆子" })
        }
    }
    val beanOptions = remember(records) {
        buildList<Long?> { add(null); addAll(records.map { it.beanId }.distinct()) }
    }
    val equipmentOptions = remember(records) {
        records.associate { record ->
            val id = if (record.equipmentName.isNullOrBlank()) FILTER_NO_EQUIPMENT else record.equipmentId ?: FILTER_NO_EQUIPMENT
            id to (record.equipmentName?.takeIf { it.isNotBlank() } ?: "未设置")
        }.toList()
    }
    val equipmentFilterOptions = remember(equipmentOptions) {
        buildList<Pair<Long, String>> { add(FILTER_ALL to "全部"); addAll(equipmentOptions) }
    }

    val filteredRecords = remember(records, selectedBeanId, selectedEquipmentId, selectedRatingFilter) {
        if (beanId > 0 || isSearching) {
            records
        } else {
            var result = records
            if (selectedBeanId != null) result = result.filter { it.beanId == selectedBeanId }
            if (selectedEquipmentId != FILTER_ALL) {
                result = result.filter { record ->
                    if (selectedEquipmentId == FILTER_NO_EQUIPMENT) {
                        record.equipmentName.isNullOrBlank()
                    } else {
                        record.equipmentId == selectedEquipmentId
                    }
                }
            }
            when (selectedRatingFilter) {
                "三星以上" -> result.filter { it.overallRating >= 3 }
                "四星以上" -> result.filter { it.overallRating >= 4 }
                "五星" -> result.filter { it.overallRating >= 5 }
                else -> result
            }
        }
    }

    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedRecords by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun enterSelectionMode(record: BrewRecord) {
        isSelectionMode = true
        selectedRecords = setOf(record.id)
    }

    fun toggleSelection(recordId: Long) {
        selectedRecords = if (selectedRecords.contains(recordId)) {
            selectedRecords - recordId
        } else {
            selectedRecords + recordId
        }
        if (selectedRecords.isEmpty()) {
            isSelectionMode = false
        }
    }

    fun toggleSelectAll() {
        if (selectedRecords.size == filteredRecords.size) {
            selectedRecords = emptySet()
            isSelectionMode = false
        } else {
            selectedRecords = filteredRecords.map { it.id }.toSet()
        }
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedRecords = emptySet()
    }

    fun deleteSelected() {
        showDeleteDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    if (isSelectionMode) {
                        Text("${selectedRecords.size} 条已选")
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Coffee, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("冲煮记录")
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    }
                },
                actions = {
                    if (!isSelectionMode) {
                        IconButton(onClick = { isSearchMode = !isSearchMode }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        IconButton(onClick = { navController.navigate(Screen.BrewEdit.createRoute(beanId = beanId)) }) {
                            Icon(Icons.Default.Add, contentDescription = "记录冲煮")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSelectionMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    titleContentColor = if (isSelectionMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = if (isSelectionMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = if (isSelectionMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode && selectedRecords.isNotEmpty(),
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
                            TextButton(onClick = { toggleSelectAll() }) {
                                Icon(Icons.Default.SelectAll, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("全选")
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "已选 ${selectedRecords.size} 条",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = { deleteSelected() },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // ===== Search bar =====
            AnimatedVisibility(
                visible = isSearchMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { brewViewModel.setSearchQuery(it) },
                    placeholder = { Text("搜索豆子、器具、冲煮参数...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { brewViewModel.clearSearch() }) {
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

            // 筛选下拉（仅全部记录视图，单豆子视图和文字搜索时不显示）
            if (beanId <= 0 && rawRecords.isNotEmpty() && !isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterDropdown(
                        label = "豆种",
                        selected = selectedBeanId,
                        options = beanOptions,
                        labelOf = { id -> if (id == null) "全部" else beanNamesById[id] ?: "未知豆子" },
                        onSelect = { selectedBeanId = it; visibleCount = PAGE_SIZE },
                        modifier = Modifier.weight(1f)
                    )
                    FilterDropdown(
                        label = "器具",
                        selected = selectedEquipmentId,
                        options = equipmentFilterOptions.map { it.first },
                        labelOf = { id -> equipmentFilterOptions.find { it.first == id }?.second ?: "全部" },
                        onSelect = { selectedEquipmentId = it; visibleCount = PAGE_SIZE },
                        modifier = Modifier.weight(1f)
                    )
                    FilterDropdown(
                        label = "星级",
                        selected = selectedRatingFilter,
                        options = ratingFilters,
                        labelOf = { it },
                        onSelect = { selectedRatingFilter = it; visibleCount = PAGE_SIZE },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isSearching) "没有找到匹配的记录" else "还没有冲煮记录\n点击 + 开始记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRecords.take(visibleCount), key = { it.id }, contentType = { "record" }) { record ->
                        val beanName = beansById[record.beanId]?.let {
                            "${it.roaster} - ${it.name}"
                        } ?: record.beanRoaster.let { if (it.isNotEmpty()) "$it - ${record.beanName}" else "未知豆子" }

                        val isSelected = selectedRecords.contains(record.id)

                        RecordCard(
                            record = record,
                            beanName = beanName,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    toggleSelection(record.id)
                                } else {
                                    navController.navigate(Screen.BrewEdit.createRoute(record.id, record.beanId))
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    enterSelectionMode(record)
                                }
                            }
                        )
                    }

                    if (filteredRecords.size > visibleCount) {
                        item(contentType = { "load_more" }) {
                            TextButton(
                                onClick = { visibleCount += PAGE_SIZE },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSelectionMode
                            ) {
                                Text("加载更多 (${filteredRecords.size - visibleCount} 条)")
                            }
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
            text = { Text("确定要删除这 ${selectedRecords.size} 条冲煮记录吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        filteredRecords.filter { selectedRecords.contains(it.id) }.forEach {
                            brewViewModel.deleteRecord(it)
                        }
                        showDeleteDialog = false
                        exitSelectionMode()
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterDropdown(
    label: String,
    selected: T,
    options: List<T>,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = labelOf(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        // 透明点击层：readOnly 的 TextField 会吞掉点击
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 200.dp)
        ) {
            // 限高 + 滚动：选项多时不会盖住下面的记录和筛选栏
            Column(
                modifier = Modifier
                    .heightIn(max = 256.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(labelOf(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
