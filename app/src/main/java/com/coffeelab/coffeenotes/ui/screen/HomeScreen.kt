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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.ui.component.RecordCard
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.util.DateUtils
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import java.util.*

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

    // Time-based greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 6 -> "夜深了"
            hour < 9 -> "早上好"
            hour < 12 -> "上午好"
            hour < 18 -> "下午好"
            else -> "晚上好"
        }
    }

    // Today's brew count
    val todayCount = remember(recentRecords) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        recentRecords.count { it.dateTime >= todayStart }
    }

    // This week count
    val thisWeekCount = remember(recentRecords) {
        DateUtils.filterByWeekRange(recentRecords, "本周").size
    }

    // Most recently used bean
    val lastBean = remember(recentRecords, beans) {
        val lastRecord = recentRecords.firstOrNull()
        if (lastRecord != null) beans.find { it.id == lastRecord.beanId } else null
    }

    // Streak days (consecutive days with at least one brew)
    val streakDays = remember(recentRecords) {
        if (recentRecords.isEmpty()) return@remember 0
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        var streak = 0
        while (true) {
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L
            val hasBrew = recentRecords.any { it.dateTime in dayStart until dayEnd }
            if (!hasBrew && streak > 0) break
            if (hasBrew) streak++
            cal.add(Calendar.DAY_OF_MONTH, -1)
            if (streak > 365) break // safety
        }
        streak
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("☕ 咖啡笔记") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Search.createRoute("all")) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
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
            // ===== Greeting =====
            item {
                Text(
                    text = "$greeting ☀️",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
                if (recentRecords.isEmpty()) {
                    Text(
                        text = "开始记录你的第一杯咖啡吧",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ===== Quick Stats Row =====
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "今日",
                        value = "$todayCount",
                        unit = "杯",
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "本周",
                        value = "$thisWeekCount",
                        unit = "杯",
                        color = MaterialTheme.colorScheme.secondary
                    )
                    StatMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "连续",
                        value = "$streakDays",
                        unit = "天",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // ===== Last Bean Card =====
            if (lastBean != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate(Screen.BeanDetail.createRoute(lastBean!!.id))
                            },
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Grain,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "最近在喝",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "${lastBean!!.roaster} - ${lastBean!!.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (lastBean!!.origin.isNotEmpty()) {
                                    Text(
                                        text = lastBean!!.origin,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // ===== Quick Start Button =====
            item {
                Button(
                    onClick = { navController.navigate(Screen.BrewEdit.createRoute()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Coffee, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始冲煮", style = MaterialTheme.typography.titleMedium)
                }
            }

            // ===== Recent Brews Section =====
            if (recentRecords.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "最近冲煮",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
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
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
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
                    items(filteredRecords.take(50), contentType = { "record" }) { record ->
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("☕", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "还没有冲煮记录",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "点击上方按钮开始第一杯吧",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun StatMiniCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.7f)
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}
