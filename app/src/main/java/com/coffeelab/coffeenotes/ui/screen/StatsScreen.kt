package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.dao.*
import com.coffeelab.coffeenotes.viewmodel.StatsViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    beanId: Long,
    viewModel: StatsViewModel = viewModel()
) {
    val isOverview = beanId <= 0
    val tabTitles = if (isOverview) listOf("趋势", "习惯", "评分", "风味") else listOf("趋势", "习惯", "评分")
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Analytics, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(if (beanId > 0) "豆子统计" else "统计总览") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview/Bean info - always show
                if (isOverview) {
                    OverviewSection(viewModel)
                } else {
                    BeanStatsSection(viewModel, beanId)
                }

                when (selectedTab) {
                    0 -> BrewTrendSection(viewModel, beanId)
                    1 -> {
                        BrewHabitsSection(viewModel, beanId)
                        if (isOverview) BeanSection(viewModel)
                    }
                    2 -> RatingSection(viewModel, beanId)
                    3 -> if (isOverview) FlavorSection(viewModel)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ===================== 总览卡片 =====================
@Composable
private fun OverviewSection(viewModel: StatsViewModel) {
    val beanCount by viewModel.beanCount.collectAsState()
    val roasterCount by viewModel.roasterCount.collectAsState()
    val originCount by viewModel.originCount.collectAsState()
    val totalBrews by viewModel.totalBrewCount.collectAsState()

    StatCard {
        Text("📦 总览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        StatRow("🫘 咖啡豆总数", "$beanCount 款")
        StatRow("🏷 烘焙商", "$roasterCount 家")
        StatRow("🌍 产地", "$originCount 个")
        StatRow("☕ 总冲煮次数", "$totalBrews 次")
    }
}

// ===================== 豆子详情统计 =====================
@Composable
private fun BeanStatsSection(viewModel: StatsViewModel, beanId: Long) {
    val beans by viewModel.allBeans.collectAsState(initial = emptyList())
    val bean = beans.find { it.id == beanId }

    if (bean != null) {
        StatCard {
            Text("${bean.roaster} - ${bean.name}", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            val brewCount by viewModel.getBrewCountForBean(beanId).collectAsState(initial = 0)
            StatRow("☕ 冲煮次数", "$brewCount 次")

            if (bean.origin.isNotEmpty()) StatRow("🌍 产地", bean.origin)
            if (bean.roastLevel.isNotEmpty()) StatRow("🔥 烘焙度", bean.roastLevel)

            val bestRecord = remember { mutableStateOf<com.coffeelab.coffeenotes.data.entity.BrewRecord?>(null) }
            LaunchedEffect(beanId) {
                bestRecord.value = viewModel.getBestRecordForBean(beanId)
            }
            bestRecord.value?.let { best ->
                Spacer(modifier = Modifier.height(8.dp))
                Text("🏆 最佳冲煮", style = MaterialTheme.typography.titleMedium)
                Text("  器具：${best.equipment}")
                Text("  粉量：${best.coffeeWeight}g · 1:${String.format("%.1f", best.coffeeWaterRatio)}")
                Text("  水温：${best.waterTemp}℃")
                Text("  总评分：${"★".repeat(best.overallRating)}")
            }
        }
    }
}

// ===================== ① 冲煮趋势 =====================
@Composable
private fun BrewTrendSection(viewModel: StatsViewModel, beanId: Long) {
    StatCard {
        Text("📈 冲煮趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (beanId > 0) {
            val monthlyCounts by viewModel.getMonthlyBrewCountsForBean(beanId).collectAsState(initial = emptyList())
            if (monthlyCounts.isNotEmpty()) {
                MonthlyTrendBar(monthlyCounts)
            } else {
                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val monthlyCounts by viewModel.monthlyBrewCounts.collectAsState(initial = emptyList())
            val thisWeek by viewModel.thisWeekCount.collectAsState(initial = 0)
            val lastWeek by viewModel.lastWeekCount.collectAsState(initial = 0)

            if (monthlyCounts.isNotEmpty()) {
                Text("📅 月度趋势（近12个月）")
                Spacer(modifier = Modifier.height(4.dp))
                MonthlyTrendBar(monthlyCounts)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("📅 本周 vs 上周")
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeekComparisonBox("本周", thisWeek, MaterialTheme.colorScheme.primary)
                WeekComparisonBox("上周", lastWeek, MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun MonthlyTrendBar(counts: List<Int>) {
    if (counts.isEmpty()) return
    val maxCount = counts.max().coerceAtLeast(1)
    val labels = generateMonthLabels(counts.size)
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    Column {
        // Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val w = size.width
            val h = size.height
            val padBottom = 24f
            val padTop = 12f
            val chartH = h - padBottom - padTop
            val stepX = if (counts.size > 1) w / (counts.size - 1) else w

            // Grid lines (3 horizontal)
            for (i in 0..2) {
                val y = padTop + chartH * i / 2
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            // Line path
            val path = Path()
            counts.forEachIndexed { i, c ->
                val x = if (counts.size > 1) i * stepX else w / 2
                val y = padTop + chartH * (1 - c.toFloat() / maxCount)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(width = 3f))

            // Points + value labels
            counts.forEachIndexed { i, c ->
                val x = if (counts.size > 1) i * stepX else w / 2
                val y = padTop + chartH * (1 - c.toFloat() / maxCount)
                drawCircle(lineColor, radius = 5f, center = Offset(x, y))
                drawCircle(Color.White, radius = 3f, center = Offset(x, y))
                // Value above point
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText("$c", x, y - 12f, paint)
                }
            }
        }

        // Month labels
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WeekComparisonBox(label: String, count: Int, color: Color) {
    Surface(
        modifier = Modifier.width(140.dp),
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "$count 次",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ===================== ② 冲煮习惯分析 =====================
@Composable
private fun BrewHabitsSection(viewModel: StatsViewModel, beanId: Long) {
    StatCard {
        Text("☕ 冲煮习惯分析", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        val isPerBean = beanId > 0

        // 器具排行
        val equipmentCnt by (if (isPerBean) viewModel.getEquipmentCountsForBean(beanId) else viewModel.equipmentCounts)
            .collectAsState(initial = emptyList())
        if (equipmentCnt.isNotEmpty()) {
            Text("🔧 器具使用排行")
            Spacer(modifier = Modifier.height(4.dp))
            val maxCnt = equipmentCnt.maxOfOrNull { it.cnt } ?: 1
            equipmentCnt.forEach { (name, cnt) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )
                    LinearProgressIndicator(
                        progress = { cnt.toFloat() / maxCnt },
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "$cnt",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 粉水比分布
        val ratioCnt by (if (isPerBean) viewModel.getRatioCountsForBean(beanId) else viewModel.ratioCounts)
            .collectAsState(initial = emptyList())
        if (ratioCnt.isNotEmpty()) {
            Text("💧 常用粉水比")
            Spacer(modifier = Modifier.height(4.dp))
            val maxRatio = ratioCnt.maxOfOrNull { it.cnt } ?: 1
            ratioCnt.forEach { (ratio, cnt) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "1:$ratio",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(56.dp)
                    )
                    LinearProgressIndicator(
                        progress = { cnt.toFloat() / maxRatio },
                        modifier = Modifier.weight(1f).height(14.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "$cnt",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 水温分布
        val tempCnt by (if (isPerBean) viewModel.getTempCountsForBean(beanId) else viewModel.tempCounts)
            .collectAsState(initial = emptyList())
        if (tempCnt.isNotEmpty()) {
            Text("🌡 常用水温区间")
            Spacer(modifier = Modifier.height(4.dp))
            val maxTemp = tempCnt.maxOfOrNull { it.cnt } ?: 1
            val tempLabels = mapOf(0 to "88°C以下", 1 to "88-91°C", 2 to "92-95°C")
            tempCnt.forEach { (bucket, cnt) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text(
                        text = tempLabels[bucket] ?: "${bucket}°C",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )
                    LinearProgressIndicator(
                        progress = { cnt.toFloat() / maxTemp },
                        modifier = Modifier.weight(1f).height(14.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "$cnt",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 冲煮时段分布
        val timeSlotCnt by (if (isPerBean) viewModel.getTimeSlotCountsForBean(beanId) else viewModel.timeSlotCounts)
            .collectAsState(initial = emptyList())
        if (timeSlotCnt.isNotEmpty()) {
            Text("🕐 冲煮时段")
            Spacer(modifier = Modifier.height(4.dp))
            val slotIcons = mapOf("早晨" to "🌅", "上午" to "☀️", "下午" to "🌤", "晚上" to "🌙", "深夜" to "🌃")
            val maxSlot = timeSlotCnt.maxOfOrNull { it.cnt } ?: 1
            timeSlotCnt.forEach { (slot, cnt) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "${slotIcons[slot] ?: ""} $slot",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(72.dp)
                    )
                    LinearProgressIndicator(
                        progress = { cnt.toFloat() / maxSlot },
                        modifier = Modifier.weight(1f).height(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "$cnt",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

// ===================== ③ 豆子统计 =====================
@Composable
private fun BeanSection(viewModel: StatsViewModel) {
    StatCard {
        Text("🫘 豆子统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // 冲煮排行 Top
        val topBeans by viewModel.topBrewedBeans.collectAsState(initial = emptyList())
        if (topBeans.isNotEmpty()) {
            Text("🥇 冲煮最多豆子 Top 5")
            Spacer(modifier = Modifier.height(4.dp))
            topBeans.take(5).forEachIndexed { index, bean ->
                Text(
                    text = "  ${index + 1}. ${bean.roaster} ${bean.name}  —  ${bean.brewCount}次",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 产地分布
        val origins by viewModel.beanCountByOrigin.collectAsState(initial = emptyList())
        if (origins.isNotEmpty()) {
            Text("🌍 产地分布")
            Spacer(modifier = Modifier.height(4.dp))
            origins.forEach { (origin, cnt) ->
                Text(
                    text = "  $origin  ——  $cnt 款",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 烘焙度分布
        val roasts by viewModel.beanCountByRoastLevel.collectAsState(initial = emptyList())
        if (roasts.isNotEmpty()) {
            Text("🔥 烘焙度分布")
            Spacer(modifier = Modifier.height(4.dp))
            roasts.forEach { (level, cnt) ->
                Text(
                    text = "  $level  ——  $cnt 款",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ===================== ④ 评分深度分析 =====================
@Composable
private fun RatingSection(viewModel: StatsViewModel, beanId: Long) {
    val isPerBean = beanId > 0
    StatCard {
        Text("⭐ 评分深度分析", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        val avgRating by (if (isPerBean) viewModel.getAvgRatingForBean(beanId).map { v -> v ?: 0.0 } else viewModel.overallAvgRating)
            .collectAsState(initial = 0.0)
        if (avgRating > 0) {
            Text(text = "📊 整体平均评分：${String.format("%.1f", avgRating)} / 5")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 评分分布
        val ratings by (if (isPerBean) viewModel.getRatingCountsForBean(beanId) else viewModel.ratingCounts)
            .collectAsState(initial = emptyList())
        val totalRated = ratings.sumOf { it.cnt }

        if (ratings.isNotEmpty() && totalRated > 0) {
            Text("评分分布")
            Spacer(modifier = Modifier.height(4.dp))
            for (rating in 5 downTo 1) {
                val count = ratings.find { it.overallRating == rating }?.cnt ?: 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "${"★".repeat(rating)}",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(80.dp)
                    )
                    LinearProgressIndicator(
                        progress = { count.toFloat() / totalRated },
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp),
                    )
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 各器具平均评分
        val equipRatings by viewModel.avgRatingByEquipment.collectAsState(initial = emptyList())
        if (equipRatings.isNotEmpty() && !isPerBean) {
            Text("🔧 各器具平均评分")
            Spacer(modifier = Modifier.height(4.dp))
            equipRatings.forEach { (equip, avg) ->
                Text(
                    text = "  $equip  ——  ${String.format("%.1f", avg)} / 5",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ===================== ⑤ 风味词统计 =====================
@Composable
private fun FlavorSection(viewModel: StatsViewModel) {
    StatCard {
        Text("🔥 风味词统计 Top 10", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        val topFlavors by viewModel.getTopFlavorTags(10).collectAsState(initial = emptyList())

        if (topFlavors.isNotEmpty()) {
            val maxCnt = topFlavors.maxOfOrNull { it.cnt } ?: 1
            topFlavors.forEachIndexed { index, (name, cnt) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )
                    LinearProgressIndicator(
                        progress = { cnt.toFloat() / maxCnt },
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "$cnt",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        } else {
            Text("暂无风味标签数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ===================== 通用组件 =====================

@Composable
private fun StatCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

/**
 * 根据月份数量生成标签列表
 */
private fun generateMonthLabels(count: Int): List<String> {
    val fmt = SimpleDateFormat("MM月", Locale.CHINESE)
    val cal = Calendar.getInstance()
    return (0 until count).map { i ->
        cal.timeInMillis = System.currentTimeMillis()
        cal.add(Calendar.MONTH, i - count + 1)
        fmt.format(cal.time)
    }
}
