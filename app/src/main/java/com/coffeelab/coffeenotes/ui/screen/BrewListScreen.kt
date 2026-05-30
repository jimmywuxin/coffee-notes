package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.coffeelab.coffeenotes.ui.component.EmptyState
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.util.DateUtils
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel

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
    val searchQuery by brewViewModel.searchQuery.collectAsState()
    val searchResults by brewViewModel.searchResults.collectAsState(initial = emptyList())
    val isSearching by brewViewModel.isSearching.collectAsState(initial = false)
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
        brewViewModel.recordsForBean.collectAsState(initial = emptyList())
    } else {
        brewViewModel.allRecords.collectAsState(initial = emptyList())
    }
    val records = if (isSearching) searchResults else rawRecords

    // Paging
    var visibleCount by remember { mutableIntStateOf(30) }
    val PAGE_SIZE = 30
    val beans by beanViewModel.allBeans.collectAsState(initial = emptyList())

    // Week range filter (only for all records view, hidden when searching)
    var selectedWeekRange by remember { mutableStateOf("全部") }
    var selectedRatingFilter by remember { mutableStateOf("全部") }
    val weekRanges = listOf("全部", "本周", "上周", "更早")
    val ratingFilters = listOf("全部", "三星以上", "四星以上", "五星")

    val filteredRecords = remember(selectedWeekRange, selectedRatingFilter, records) {
        if (beanId > 0 || isSearching) {
            records
        } else {
            var result = DateUtils.filterByWeekRange(records, selectedWeekRange)
            result = when (selectedRatingFilter) {
                "三星以上" -> result.filter { it.overallRating >= 3 }
                "四星以上" -> result.filter { it.overallRating >= 4 }
                "五星" -> result.filter { it.overallRating >= 5 }
                else -> result
            }
            result
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

            // Filter dropdowns (only for all records view, not bean-specific and not when searching)
            if (beanId <= 0 && rawRecords.isNotEmpty() && !isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var weekExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = weekExpanded,
                        onExpandedChange = { weekExpanded = it },
                        modifier = Modifier.width(140.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedWeekRange,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("时间段") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = weekExpanded,
                            onDismissRequest = { weekExpanded = false }
                        ) {
                            weekRanges.forEach { range ->
                                DropdownMenuItem(
                                    text = { Text(range) },
                                    onClick = {
                                        selectedWeekRange = range
                                        visibleCount = PAGE_SIZE
                                        weekExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var ratingExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = ratingExpanded,
                        onExpandedChange = { ratingExpanded = it },
                        modifier = Modifier.width(140.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedRatingFilter,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("星级") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ratingExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = ratingExpanded,
                            onDismissRequest = { ratingExpanded = false }
                        ) {
                            ratingFilters.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter) },
                                    onClick = {
                                        selectedRatingFilter = filter
                                        ratingExpanded = false
                                        visibleCount = PAGE_SIZE
                                    }
                                )
                            }
                        }
                    }
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
                    items(filteredRecords.take(visibleCount), contentType = { "record" }) { record ->
                        val beanName = beans.find { it.id == record.beanId }?.let {
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
