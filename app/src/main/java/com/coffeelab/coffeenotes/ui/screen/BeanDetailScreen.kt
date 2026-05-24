package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.RoastDegree
import com.coffeelab.coffeenotes.data.entity.ProcessMethod
import com.coffeelab.coffeenotes.data.entity.RestPeriodConfig
import com.coffeelab.coffeenotes.data.entity.PeakFlavorConfig
import com.coffeelab.coffeenotes.data.entity.PurchaseRecord
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.ui.component.RecordCard
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.util.DateUtils
import com.coffeelab.coffeenotes.util.ImageUtils
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
    val context = LocalContext.current
    var purchaseRecords by remember { mutableStateOf<List<PurchaseRecord>>(emptyList()) }
    LaunchedEffect(beanId) {
        beanViewModel.loadBean(beanId)
        beanViewModel.loadTags(beanId)
        beanViewModel.loadImpressionTags(beanId)
        brewViewModel.loadRecordsForBean(beanId)
        // 加载购买记录
        val records = AppDatabase.getInstance(context).purchaseRecordDao().getByBeanIdOnce(beanId)
        purchaseRecords = records
    }

    val bean by beanViewModel.selectedBean.collectAsState(initial = null)
    val tags by beanViewModel.tags.collectAsState(initial = emptyList())
    val impressionTags by beanViewModel.impressionTags.collectAsState(initial = emptyList())
    val records by brewViewModel.recordsForBean.collectAsState(initial = emptyList())
    val roastDegrees by beanViewModel.allRoastDegrees.collectAsState(initial = emptyList())
    val processMethods by beanViewModel.allProcessMethods.collectAsState(initial = emptyList())

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
    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(bean?.name ?: "豆子详情") },
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

                // 基础信息
                item {
                    Surface(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("基础信息", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            // 豆名（突出）
                            Text(
                                text = b.name.ifEmpty { "未命名" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            // 烘焙商（小字弱化）
                            if (b.roaster.isNotEmpty()) {
                                Text(
                                    text = b.roaster,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // 产地 · 产区 · 庄园
                            if (b.origin.isNotEmpty() || b.region.isNotEmpty() || b.estate.isNotEmpty()) {
                                Text(
                                    text = listOfNotNull(b.origin.ifEmpty { null }, b.region.ifEmpty { null }, b.estate.ifEmpty { null }).joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            // 品种 · 处理法 · 烘焙度
                            val processName = processMethods.find { it.name == b.process }?.name ?: b.process
                            val roastName = roastDegrees.find { it.name == b.roastLevel }?.name ?: b.roastLevel
                            if (b.variety.isNotEmpty() || processName.isNotEmpty() || roastName.isNotEmpty()) {
                                Text(
                                    text = listOfNotNull(b.variety.ifEmpty { null }, processName.ifEmpty { null }, roastName.ifEmpty { null }).joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            // 养豆至 · 赏味至
                            if (b.roastDate != null && (b.restDays != null || b.peakFlavorDays != null)) {
                                val DAY_MS = 86400 * 1000L
                                val restEnd = b.restDays?.let { b.roastDate + it.toLong() * DAY_MS }
                                val peakEnd = b.peakFlavorDays?.let { (restEnd ?: b.roastDate) + it.toLong() * DAY_MS }
                                if (restEnd != null || peakEnd != null) {
                                    Text(
                                        text = listOfNotNull(
                                            restEnd?.let { "养豆至 ${DateUtils.formatDate(it)}" },
                                            peakEnd?.let { "赏味至 ${DateUtils.formatDate(it)}" }
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
                // 备注
                if (b.notes.isNotEmpty()) {
                    item {
                        Surface(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("备注", style = MaterialTheme.typography.labelLarge)
                                Text(b.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Bean Photos Grid (above extraction suggestion)
                if (b.localPhotoPaths.isNotEmpty()) {
                    item {
                        Text(
                            "豆子照片",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val rows = b.localPhotoPaths.chunked(3)
                            rows.forEach { rowPhotos ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowPhotos.forEach { photoPath ->
                                        val photoFile = ImageUtils.getBeanPhotoFile(context, photoPath)
                                        AsyncImage(
                                            model = File(photoFile.absolutePath),
                                            contentDescription = "豆子照片",
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .fillMaxWidth()
                                                .clickable { selectedPhotoPath = photoPath },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    repeat(3 - rowPhotos.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // 印象标签
                if (impressionTags.isNotEmpty()) {
                    item {
                        Text(
                            "印象标签",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            impressionTags.forEach { tag ->
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 风味标签（上移）
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


                // 购买记录入口
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("购买记录", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = {
                            navController.navigate(Screen.PurchaseRecordManagement.createRoute(beanId, bean?.name ?: ""))
                        }) {
                            Text("管理")
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (purchaseRecords.isNotEmpty()) {
                        val totalWeight = purchaseRecords.sumOf { it.weightGrams }
                        Text(
                            "${purchaseRecords.size} 条记录，共 ${totalWeight}g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }
                }



                // 官方萃取建议
                if (b.dose != null || b.brewRatio != null ||
                    b.waterAmount != null || b.brewTime != null || b.waterTemp != null) {
                    item {
                        Surface(modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            shape = MaterialTheme.shapes.medium) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("官方萃取建议", style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                // 粉量 + 注水量
                                if (b.dose != null || b.waterAmount != null) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        if (b.dose != null) InfoRowCompact("粉量", "${b.dose}g", Modifier.weight(1f))
                                        else Spacer(Modifier.weight(1f))
                                        if (b.waterAmount != null) InfoRowCompact("注水量", "${b.waterAmount}ml", Modifier.weight(1f))
                                        else Spacer(Modifier.weight(1f))
                                    }
                                }
                                // 粉水比 + 水温
                                if (b.brewRatio?.isNotEmpty() == true || b.waterTemp != null) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        if (b.brewRatio?.isNotEmpty() == true) {
                                            val ratioDisplay = if (b.brewRatio!!.startsWith("1:") || b.brewRatio!!.startsWith("1：")) b.brewRatio else "1:${b.brewRatio}"
                                            InfoRowCompact("粉水比", ratioDisplay, Modifier.weight(1f))
                                        } else Spacer(Modifier.weight(1f))
                                        if (b.waterTemp != null) InfoRowCompact("水温", "${b.waterTemp}°C", Modifier.weight(1f))
                                        else Spacer(Modifier.weight(1f))
                                    }
                                }
                                // 注水时长 + 萃取时长
                                if (b.pouringDurationSeconds != null || b.brewTime != null) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        if (b.pouringDurationSeconds != null) InfoRowCompact("注水时长", "${b.pouringDurationSeconds}s", Modifier.weight(1f))
                                        else Spacer(Modifier.weight(1f))
                                        if (b.brewTime != null) InfoRowCompact("萃取时长", "${b.brewTime}s", Modifier.weight(1f))
                                        else Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
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

    // Full-screen photo viewer
    if (selectedPhotoPath != null) {
        FullScreenPhotoDialog(
            photoPath = selectedPhotoPath!!,
            context = context,
            onDismiss = { selectedPhotoPath = null }
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
fun InfoRowCompact(label: String, value: String, modifier: Modifier = Modifier) {
    if (value.isNotEmpty()) {
        Row(modifier = modifier.padding(vertical = 2.dp)) {
            Text(
                text = "$label：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


// Full-screen photo viewer dialog
@Composable
fun FullScreenPhotoDialog(
    photoPath: String,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    val photoFile = ImageUtils.getBeanPhotoFile(context, photoPath)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = File(photoFile.absolutePath),
                contentDescription = "照片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        }
    }
}
