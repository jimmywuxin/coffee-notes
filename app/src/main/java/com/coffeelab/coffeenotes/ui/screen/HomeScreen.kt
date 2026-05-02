package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.data.entity.BrewRecipe
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.util.DateUtils
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import com.coffeelab.coffeenotes.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    beanViewModel: BeanViewModel = viewModel(),
    brewViewModel: BrewViewModel = viewModel()
) {
    val beans by beanViewModel.allBeans.collectAsState(initial = emptyList())
    val recentRecords by brewViewModel.allRecords.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("☕ 咖啡笔记") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        label = "添加豆子",
                        onClick = { navController.navigate(Screen.BeanEdit.createRoute()) }
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Search,
                        label = "搜索",
                        onClick = { navController.navigate(Screen.Search.route) }
                    )
                }
            }

            // Navigation Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NavCard(
                            modifier = Modifier.weight(1f),
                            emoji = "🫘",
                            label = "我的豆子 (${beans.size})",
                            onClick = { navController.navigate(Screen.BeanList.route) }
                        )
                        NavCard(
                            modifier = Modifier.weight(1f),
                            emoji = "📋",
                            label = "冲煮配方",
                            onClick = { navController.navigate(Screen.RecipeList.route) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NavCard(
                            modifier = Modifier.weight(1f),
                            emoji = "📊",
                            label = "统计",
                            onClick = { navController.navigate(Screen.Stats.createRoute()) }
                        )
                        NavCard(
                            modifier = Modifier.weight(1f),
                            emoji = "📥",
                            label = "导入",
                            onClick = { navController.navigate(Screen.Import.route) }
                        )
                    }
                }
            }

            // Recent Records Section
            if (recentRecords.isNotEmpty()) {
                item {
                    Text(
                        text = "最近冲煮",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(recentRecords.take(50)) { record ->
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
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
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
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
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
fun NavCard(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    text = "${record.coffeeWeight}g / ${record.waterWeight}ml · ${record.waterTemp}℃",
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
