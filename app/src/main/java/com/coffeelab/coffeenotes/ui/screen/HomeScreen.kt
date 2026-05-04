package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.util.DateUtils
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    beanViewModel: BeanViewModel = viewModel(),
    brewViewModel: BrewViewModel = viewModel()
) {
    val beans by beanViewModel.allBeans.collectAsState(initial = emptyList())
    val recentRecords by brewViewModel.allRecords.collectAsState(initial = emptyList())
    var selectedWeekRange by remember { mutableStateOf("全部") }
    val weekRanges = listOf("全部", "本周", "上周", "两周前", "更早")

    val filteredRecords = remember(selectedWeekRange, recentRecords) {
        DateUtils.filterByWeekRange(recentRecords, selectedWeekRange)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("☕ 咖啡笔记") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Action Row: 冲煮记录 | 我的豆子 | 搜索
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Coffee,
                        label = "冲煮记录",
                        onClick = { navController.navigate(Screen.BrewList.createRoute()) }
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Grain,
                        label = "我的豆子",
                        onClick = { navController.navigate(Screen.BeanList.route) }
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Search,
                        label = "搜索",
                        onClick = { navController.navigate(Screen.Search.route) }
                    )
                }
            }

            // Recent Brews Section
            if (recentRecords.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "最近冲煮",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                        // Week filter chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            weekRanges.forEach { range ->
                                FilterChip(
                                    selected = (selectedWeekRange == range),
                                    onClick = { selectedWeekRange = range },
                                    label = { Text(range) }
                                )
                            }
                        }
                    }
                }

                if (filteredRecords.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "暂无${selectedWeekRange}冲煮记录",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredRecords.take(50)) { record ->
                        val beanName = beans.find { it.id == record.beanId }?.let { bean ->
                            "${bean.roaster} - ${bean.name}"
                        } ?: "未知豆子"

                        RecordCard(
                            record = record,
                            beanName = beanName,
                            onClick = {
                                navController.navigate(Screen.BrewEdit.createRoute(record.id, record.beanId))
                            }
                        )
                    }
                }
            } else {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "还没有冲煮记录\n点击上方「冲煮记录」开始第一杯吧 ☕",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun RecordCard(
    record: BrewRecord,
    beanName: String,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = beanName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${record.equipment} · ${DateUtils.formatDateTime(record.dateTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${record.coffeeWeight}g · 1:${String.format("%.1f", record.coffeeWaterRatio)} · ${record.waterTemp}℃",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (record.overallRating > 0) {
                Text(
                    text = "★".repeat(record.overallRating),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
