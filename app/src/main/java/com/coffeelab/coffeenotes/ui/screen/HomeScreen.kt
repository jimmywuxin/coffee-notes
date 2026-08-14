package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.ui.component.BeanCard
import com.coffeelab.coffeenotes.ui.component.RecordCard
import com.coffeelab.coffeenotes.ui.component.EmptyState
import com.coffeelab.coffeenotes.ui.component.RandomBeanPickerDialog
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewMethodViewModel
import com.coffeelab.coffeenotes.viewmodel.HomeViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    beanViewModel: BeanViewModel = viewModel(),
    brewViewModel: BrewViewModel = viewModel(),
    methodViewModel: BrewMethodViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val beans by beanViewModel.allBeans.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentRecords by brewViewModel.allRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val allMethods by methodViewModel.allMethods.collectAsStateWithLifecycle(initialValue = emptyList())
    val nearingBeans by beanViewModel.beansNearingPeakFlavorEnd.collectAsStateWithLifecycle(initialValue = emptyList())

    // ===== Search state =====
    var isSearchMode by remember { mutableStateOf(false) }
    val searchQuery by homeViewModel.searchQuery.collectAsStateWithLifecycle(initialValue = homeViewModel.searchQuery.value)
    val isSearching by homeViewModel.isSearching.collectAsStateWithLifecycle(initialValue = false)
    val mixedResults by homeViewModel.mixedResults.collectAsStateWithLifecycle(initialValue = emptyList())
    val focusRequester = remember { FocusRequester() }
    var showRandomPicker by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        } else {
            homeViewModel.clearSearch()
        }
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

    // Stats (computed from recentRecords)
    val todayCount = remember(recentRecords) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        recentRecords.count { it.dateTime >= todayStart }
    }

    val streakDays = remember(recentRecords) {
        if (recentRecords.isEmpty()) return@remember 0
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
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

    val totalCount = recentRecords.size

    val avgPerWeek = remember(recentRecords) {
        if (recentRecords.isEmpty()) return@remember 0.0
        val now = System.currentTimeMillis()
        val fourWeeksAgo = now - 28L * 24 * 60 * 60 * 1000
        val recent4w = recentRecords.count { it.dateTime >= fourWeeksAgo }
        (recent4w / 4.0 * 10).toInt() / 10.0
    }

    val mostUsedMethod = remember(recentRecords, allMethods) {
        if (recentRecords.isEmpty()) return@remember null
        val methodCounts = recentRecords
            .mapNotNull { record -> record.methodId?.let { id -> allMethods.find { it.id == id } } }
            .groupingBy { it.name }.eachCount()
        if (methodCounts.isEmpty()) null else {
            val (name, count) = methodCounts.maxByOrNull { it.value }!!
            name to count
        }
    }

    val lastBean = remember(recentRecords, beans) {
        val lastRecord = recentRecords.firstOrNull()
        if (lastRecord != null) beans.find { it.id == lastRecord.beanId } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    if (isSearchMode) {
                        Text("搜索")
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("咖啡笔记")
                        }
                    }
                },
                actions = {
                    if (isSearchMode) {
                        TextButton(onClick = {
                            isSearchMode = false
                            homeViewModel.clearSearch()
                        }) {
                            Text("取消", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        IconButton(onClick = { isSearchMode = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
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
                    onValueChange = { homeViewModel.setSearchQuery(it) },
                    placeholder = { Text("搜索豆子、冲煮记录", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { homeViewModel.clearSearch() }) {
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

            if (isSearchMode) {
                // ===== Search Results =====
                if (searchQuery.isBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "输入关键词开始搜索",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (mixedResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "没有找到匹配的结果",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(mixedResults, key = { item ->
                            when (item) {
                                is CoffeeBean -> "bean_${item.id}"
                                is BrewRecord -> "record_${item.id}"
                                else -> "unknown_${item.hashCode()}"
                            }
                        }, contentType = { item ->
                            when (item) {
                                is CoffeeBean -> "bean"
                                is BrewRecord -> "record"
                                else -> "unknown"
                            }
                        }) { item ->
                            when (item) {
                                is CoffeeBean -> {
                                    BeanCard(
                                        bean = item,
                                        onClick = { navController.navigate(Screen.BeanDetail.createRoute(item.id)) },
                                        impressionTags = emptyList()
                                    )
                                }
                                is BrewRecord -> {
                                    val bn = item.beanRoaster.let {
                                        if (it.isNotEmpty()) "$it - ${item.beanName}" else "未知豆子"
                                    }
                                    RecordCard(
                                        record = item,
                                        beanName = bn,
                                        onClick = {
                                            navController.navigate(Screen.BrewEdit.createRoute(item.id, item.beanId))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ===== Normal home content =====
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Greeting
                    item {
                        Text(
                            text = "$greeting，今天 $todayCount 杯 ☕",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Stats cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatMiniCard(
                                modifier = Modifier.weight(1f),
                                label = "连续冲煮",
                                value = "$streakDays",
                                unit = "天",
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatMiniCard(
                                modifier = Modifier.weight(1f),
                                label = "总冲煮",
                                value = "$totalCount",
                                unit = "次",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            StatMiniCard(
                                modifier = Modifier.weight(1f),
                                label = "周均",
                                value = if (avgPerWeek == avgPerWeek.toInt().toDouble()) "${avgPerWeek.toInt()}" else "$avgPerWeek",
                                unit = "杯",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Most used method
                    if (mostUsedMethod != null) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("最爱手法：", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${mostUsedMethod.first} (${mostUsedMethod.second}次)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Last bean
                    if (lastBean != null) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "最近在喝",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(Modifier.height(4.dp))
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
                            }
                        }
                    }

                    // Peak flavor countdown
                    if (nearingBeans.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(8.dp))
                                        Text("赏味期倒计时", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    nearingBeans.forEach { (bean, daysLeft) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.BeanDetail.createRoute(bean.id)) }.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${bean.roaster} - ${bean.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.30f),
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    text = if (daysLeft <= 0) "今日结束" else "剩余${daysLeft}天",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick start + 随机选豆
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { navController.navigate(Screen.BrewEdit.createRoute()) },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Coffee, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开始冲煮", style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedButton(
                                onClick = { showRandomPicker = true },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("随机选豆", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }

                    if (recentRecords.isEmpty()) {
                        item {
                            EmptyState(
                                emoji = "☕",
                                message = "还没有冲煮记录",
                                hint = "点击上方按钮开始第一杯吧"
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // 随机选豆弹窗（只对在喝的豆子）
    if (showRandomPicker) {
        RandomBeanPickerDialog(
            activeBeans = beans,
            onDismiss = { showRandomPicker = false },
            onPick = { navController.navigate(Screen.BeanDetail.createRoute(it.id)) }
        )
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
