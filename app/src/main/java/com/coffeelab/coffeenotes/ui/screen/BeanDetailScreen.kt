package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.ui.component.RecordCard
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.util.DateUtils
import com.coffeelab.coffeenotes.ui.component.RadarChart
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeanDetailScreen(
    navController: NavController,
    beanId: Long,
    beanViewModel: BeanViewModel = viewModel(),
    brewViewModel: BrewViewModel = viewModel()
) {
    LaunchedEffect(beanId) {
        beanViewModel.loadBean(beanId)
        beanViewModel.loadTags(beanId)
        brewViewModel.loadRecordsForBean(beanId)
    }

    val bean by beanViewModel.selectedBean.collectAsState(initial = null)
    val tags by beanViewModel.tags.collectAsState(initial = emptyList())
    val records by brewViewModel.recordsForBean.collectAsState(initial = emptyList())

    // 计算雷达图数据（取有评分记录的维度平均分）
    val radarValues = remember(records) {
        val ratedRecords = records.filter {
            it.acidity > 0 || it.sweetness > 0 || it.bitterness > 0 || it.mouthfeel > 0 || it.aftertaste > 0
        }
        if (ratedRecords.isEmpty()) {
            null
        } else {
            val sum = ratedRecords.fold(floatArrayOf(0f, 0f, 0f, 0f, 0f)) { acc, r ->
                acc[0] += r.acidity.toFloat()
                acc[1] += r.sweetness.toFloat()
                acc[2] += r.bitterness.toFloat()
                acc[3] += r.mouthfeel.toFloat()
                acc[4] += r.aftertaste.toFloat()
                acc
            }
            val count = ratedRecords.size.toFloat()
            listOf(sum[0] / count, sum[1] / count, sum[2] / count, sum[3] / count, sum[4] / count)
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(bean?.name ?: "豆子详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (bean != null) {
                        IconButton(onClick = { showArchiveDialog = true }) {
                            Icon(
                                if (bean!!.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (bean!!.isArchived) "取消归档" else "归档"
                            )
                        }
                        IconButton(onClick = {
                            navController.navigate(Screen.BeanEdit.createRoute(beanId))
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (bean != null) {
                val b = bean!!

                // Image
                if (b.imageUri.isNotEmpty()) {
                    item {
                        AsyncImage(
                            model = File(b.imageUri),
                            contentDescription = "豆袋照片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Bean Info
                item {
                    Surface(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("烘焙商", b.roaster)
                            InfoRow("豆名", b.name)
                            InfoRow("产地", b.origin)
                            InfoRow("庄园", b.estate)
                            InfoRow("品种", b.variety)
                            InfoRow("处理法", b.process)
                            InfoRow("烘焙度", b.roastLevel)
                            if (b.roastDate != null) {
                                InfoRow("烘焙日期", DateUtils.formatDate(b.roastDate))
                            }
                            if (b.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("备注", style = MaterialTheme.typography.labelLarge)
                                Text(b.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // 官方萃取建议卡片
                if (b.dose != null || b.brewRatio != null ||
                    b.waterAmount != null || b.brewTime != null || b.waterTemp != null) {
                    item {
                        Surface(modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondaryContainer) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalCafe, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("官方萃取建议", style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (b.dose != null) {
                                    ExtractionInfoRow("粉量", "${b.dose}g")
                                }
                                if (b.brewRatio?.isNotEmpty() == true) {
                                    ExtractionInfoRow("粉水比", b.brewRatio)
                                }
                                if (b.waterAmount != null) {
                                    ExtractionInfoRow("注水量", "${b.waterAmount}ml")
                                }
                                if (b.brewTime != null) {
                                    ExtractionInfoRow("萃取时间", "${b.brewTime}s")
                                }
                                if (b.waterTemp != null) {
                                    ExtractionInfoRow("水温", "${b.waterTemp}°C")
                                }
                            }
                        }
                    }
                }

                // Flavor Tags
                if (tags.isNotEmpty()) {
                    item {
                        Text(
                            "风味标签",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            tags.forEach { tag ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }

                // Radar Chart
                if (radarValues != null) {
                    item {
                        Text(
                            "风味雷达图",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        RadarChart(
                            values = radarValues,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Action buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                navController.navigate(Screen.BrewEdit.createRoute(beanId = beanId))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Coffee, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("冲煮")
                        }
                        OutlinedButton(
                            onClick = {
                                navController.navigate(Screen.Stats.createRoute(beanId))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("统计")
                        }
                    }
                }

                // Brew Records for this bean
                if (records.isNotEmpty()) {
                    item {
                        Text(
                            "冲煮记录 (${records.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(records) { record ->
                        RecordCard(
                            record = record,
                            beanName = bean?.name ?: "",
                            isSelectionMode = false,
                            isSelected = false,
                            onClick = {
                                navController.navigate(
                                    Screen.BrewEdit.createRoute(record.id, record.beanId)
                                )
                            }
                        )
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    // Archive Confirmation Dialog
    if (showArchiveDialog && bean != null) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text(if (bean!!.isArchived) "取消归档" else "归档") },
            text = {
                Text(
                    if (bean!!.isArchived)
                        "将「${bean?.name}」恢复到豆子列表？"
                    else
                        "将「${bean?.name}」归档？\n归档后不在主列表显示，但数据和冲煮记录都保留。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        bean?.let {
                            val isArchived = it.isArchived
                            if (isArchived) beanViewModel.unarchiveBean(it)
                            else beanViewModel.archiveBean(it)
                            showArchiveDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isArchived) "已取消归档" else "已归档",
                                    duration = SnackbarDuration.Indefinite
                                )
                            }
                            coroutineScope.launch {
                                delay(1000)
                                navController.popBackStack()
                            }
                        }
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) { Text("取消") }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && bean != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${bean?.name}」吗？\n该豆子的所有冲煮记录也会被删除，且无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        bean?.let {
                            val beanName = it.name
                            beanViewModel.deleteBean(it)
                            showDeleteDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "已删除「${beanName}」",
                                    duration = SnackbarDuration.Indefinite
                                )
                            }
                            coroutineScope.launch {
                                delay(1000)
                                navController.popBackStack()
                            }
                        }
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    if (value.isNotEmpty()) {
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(
                text = "$label：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ExtractionInfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
