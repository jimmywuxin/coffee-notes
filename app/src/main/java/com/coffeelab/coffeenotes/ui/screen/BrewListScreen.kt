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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.ui.component.RecordCard
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
    val rawRecords by if (beanId > 0) {
        brewViewModel.loadRecordsForBean(beanId)
        brewViewModel.recordsForBean.collectAsState(initial = emptyList())
    } else {
        brewViewModel.allRecords.collectAsState(initial = emptyList())
    }
    val records = rawRecords

    // 分页加载
    var visibleCount by remember { mutableIntStateOf(30) }
    val PAGE_SIZE = 30
    val beans by beanViewModel.allBeans.collectAsState(initial = emptyList())

    // Week range filter (only for all records view)
    var selectedWeekRange by remember { mutableStateOf("全部") }
    val weekRanges = listOf("全部", "本周", "上周", "两周前", "更早")
    val filteredRecords = remember(selectedWeekRange, records) {
        if (beanId > 0) records else DateUtils.filterByWeekRange(records, selectedWeekRange)
    }

    // 多选状态
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
                        IconButton(onClick = { navController.navigate(Screen.Search.createRoute("records")) }) {
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
                                text = "已选 ${selectedRecords.size} 条",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips (only for all records view)
            if (beanId <= 0 && records.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    weekRanges.forEach { range ->
                        FilterChip(
                            selected = (selectedWeekRange == range),
                            onClick = {
                                selectedWeekRange = range
                                visibleCount = PAGE_SIZE
                            },
                            label = { Text(range) }
                        )
                    }
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "还没有冲煮记录\n点击 + 开始记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRecords.take(visibleCount), contentType = { "record" }) { record ->
                        val beanName = beans.find { it.id == record.beanId }?.let {
                            "${it.roaster} - ${it.name}"
                        } ?: "未知豆子"
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
                                    navController.navigate(
                                        Screen.BrewEdit.createRoute(record.id, record.beanId)
                                    )
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    enterSelectionMode(record)
                                }
                            }
                        )
                    }

                    // 加载更多
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

    // 批量删除确认弹窗
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = {
                Text("确定要删除这 ${selectedRecords.size} 条冲煮记录吗？删除后无法恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        filteredRecords.filter { selectedRecords.contains(it.id) }.forEach {
                            brewViewModel.deleteRecord(it)
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
