package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import com.coffeelab.coffeenotes.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    beanId: Long,
    viewModel: BeanViewModel = viewModel(),
    brewViewModel: BrewViewModel = viewModel()
) {
    val beans by viewModel.allBeans.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 统计") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (beanId > 0) "豆子统计" else "总览",
                style = MaterialTheme.typography.headlineMedium
            )

            // General stats
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📦 咖啡豆总数：${beans.size} 款", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "🏷 烘焙商：${beans.map { it.roaster }.filter { it.isNotEmpty() }.distinct().size} 家",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "🌍 产地：${beans.map { it.origin }.filter { it.isNotEmpty() }.distinct().size} 个",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // If a specific bean is selected
            if (beanId > 0) {
                val bean = beans.find { it.id == beanId }
                if (bean != null) {
                    Surface(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "${bean.roaster} - ${bean.name}",
                                style = MaterialTheme.typography.titleLarge
                            )

                            val brewCount by brewViewModel.getBrewCountForBean(beanId).collectAsState(initial = 0)
                            Text("☕ 冲煮次数：$brewCount 次")

                            // Best record
                            val bestRecord = remember { mutableStateOf<com.coffeelab.coffeenotes.data.entity.BrewRecord?>(null) }
                            LaunchedEffect(beanId) {
                                bestRecord.value = brewViewModel.getBestRecordForBean(beanId)
                            }
                            bestRecord.value?.let { best ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("🏆 最佳冲煮：", style = MaterialTheme.typography.titleMedium)
                                Text("  器具：${best.equipment}")
                                Text("  粉量：${best.coffeeWeight}g · 1:${String.format("%.1f", best.coffeeWaterRatio)}")
                                Text("  水温：${best.waterTemp}℃")
                                Text("  总评分：${"★".repeat(best.overallRating)}")
                            }
                        }
                    }
                }
            }

            // Recent brews overview
            val allRecords by brewViewModel.allRecords.collectAsState(initial = emptyList())
            if (allRecords.isNotEmpty()) {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("冲煮记录总览", style = MaterialTheme.typography.titleMedium)
                        Text("总冲煮次数：${allRecords.size}")

                        // Rating distribution
                        val ratingGroups = allRecords.groupBy { it.overallRating }
                        Text("评分分布：", style = MaterialTheme.typography.bodyMedium)
                        for (rating in 5 downTo 1) {
                            val count = ratingGroups[rating]?.size ?: 0
                            if (count > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("${"★".repeat(rating)}", color = MaterialTheme.colorScheme.secondary)
                                    LinearProgressIndicator(
                                        progress = { count.toFloat() / allRecords.size },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(12.dp),
                                    )
                                    Text("$count", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
