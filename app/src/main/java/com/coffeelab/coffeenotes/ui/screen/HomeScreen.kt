package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.coffeelab.coffeenotes.data.entity.BrewMethod
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.util.DateUtils
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewMethodViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    beanViewModel: BeanViewModel = viewModel(),
    brewViewModel: BrewViewModel = viewModel(),
    methodViewModel: BrewMethodViewModel = viewModel()
) {
    val beans by beanViewModel.allBeans.collectAsState(initial = emptyList())
    val recentRecords by brewViewModel.allRecords.collectAsState(initial = emptyList())
    val allMethods by methodViewModel.allMethods.collectAsState(initial = emptyList())
    val nearingBeans by beanViewModel.beansNearingPeakFlavorEnd.collectAsState(initial = emptyList())

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
            if (streak > 365) break
        }
        streak
    }

    // Total brew count
    val totalCount = recentRecords.size

    // Average brews per week (last 4 weeks)
    val avgPerWeek = remember(recentRecords) {
        if (recentRecords.isEmpty()) return@remember 0.0
        val now = System.currentTimeMillis()
        val fourWeeksAgo = now - 28L * 24 * 60 * 60 * 1000
        val recent4w = recentRecords.count { it.dateTime >= fourWeeksAgo }
        (recent4w / 4.0 * 10).toInt() / 10.0
    }

    // Most used brew method
    val mostUsedMethod = remember(recentRecords, allMethods) {
        if (recentRecords.isEmpty()) return@remember null
        val methodCounts = recentRecords
            .mapNotNull { record -> record.methodId?.let { id -> allMethods.find { it.id == id } } }
            .groupingBy { it.name }
            .eachCount()
        if (methodCounts.isEmpty()) null else {
            val (name, count) = methodCounts.maxByOrNull { it.value }!!
            name to count
        }
    }

    // Most recently used bean
    val lastBean = remember(recentRecords, beans) {
        val lastRecord = recentRecords.firstOrNull()
        if (lastRecord != null) beans.find { it.id == lastRecord.beanId } else null
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
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "$greeting ☀️",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (recentRecords.isNotEmpty()) {
                        Text(
                            text = "今天第 $todayCount 杯" + if (streakDays > 0) "，连续 $streakDays 天 ☕" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "开始记录你的第一杯咖啡吧",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ===== Stats Row =====
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "总杯数",
                        value = "$totalCount",
                        unit = "杯",
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "均/周",
                        value = if (avgPerWeek > 0) String.format("%.1f", avgPerWeek) else "-",
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

            // ===== Most Used Method =====
            if (mostUsedMethod != null) {
                item {
                    val (methodName, count) = mostUsedMethod
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "最爱用的方式",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = methodName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "$count 次",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
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

            // 赏味期倒计时
            if (nearingBeans.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(8.dp))
                                Text("赏味期倒计时", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            nearingBeans.forEach { (bean, daysLeft) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.BeanDetail.createRoute(bean.id)) }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = bean.roaster + " - " + bean.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = if (daysLeft <= 0) "今日结束" else "剩余" + daysLeft + "天",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                }
                            }
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

            // ===== Empty State =====
            if (recentRecords.isEmpty()) {
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
