package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.Converters
import com.coffeelab.coffeenotes.data.entity.BrewMethod
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BrewMethodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewMethodListScreen(
    navController: NavController,
    viewModel: BrewMethodViewModel = viewModel()
) {
    val methods by viewModel.allMethods.collectAsState(initial = emptyList())
    val mutableList = remember(methods) { methods.toMutableList() }
    var isReorderMode by remember { mutableStateOf(false) }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (toIndex < 0 || toIndex >= mutableList.size) return
        val item = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, item)
    }

    fun saveOrder() {
        isReorderMode = false
        viewModel.saveMethodOrder(mutableList.toList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isReorderMode) Text("冲煮手法排序") else Text("☕ 冲煮手法")
                },
                navigationIcon = {
                    if (isReorderMode) {
                        IconButton(onClick = { isReorderMode = false }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (!isReorderMode && methods.isNotEmpty()) {
                        IconButton(onClick = { isReorderMode = true }) {
                            Icon(Icons.Default.SwapVert, contentDescription = "排序", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    if (!isReorderMode) {
                        IconButton(onClick = { navController.navigate(Screen.BrewMethodEdit.createRoute()) }) {
                            Icon(Icons.Default.Add, contentDescription = "新建手法", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    if (isReorderMode) {
                        IconButton(onClick = { saveOrder() }) {
                            Icon(Icons.Default.Check, contentDescription = "完成排序", tint = MaterialTheme.colorScheme.onPrimary)
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
        if (methods.isEmpty() && !isReorderMode) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有冲煮手法\n点击 + 新建 🧪",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isReorderMode) {
                    itemsIndexed(mutableList) { index, method ->
                        ReorderableMethodItem(
                            method = method,
                            onMoveUp = { moveItem(index, index - 1) },
                            onMoveDown = { moveItem(index, index + 1) },
                            isFirst = index == 0,
                            isLast = index == mutableList.size - 1
                        )
                    }
                } else {
                    itemsIndexed(methods) { _, method ->
                        MethodCard(
                            method = method,
                            onClick = {
                                navController.navigate(Screen.BrewMethodEdit.createRoute(method.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableMethodItem(
    method: BrewMethod,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "拖动排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(method.name, style = MaterialTheme.typography.titleMedium)
                val parsedSteps = Converters.parseSteps(method.steps)
                val stepSummary = parsedSteps.withIndex().joinToString(" → ") { (i, step) ->
                    val water = step.waterAmount?.let { "${it}ml" } ?: "至总水量"
                    "${i + 1}: ${water}/${step.durationSeconds}s"
                }
                Text(
                    stepSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "上移",
                    tint = if (!isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "下移",
                    tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun MethodCard(
    method: BrewMethod,
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (method.isPreset) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "预置",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(method.name, style = MaterialTheme.typography.titleMedium)
                }
                val parsedSteps = Converters.parseSteps(method.steps)
                val stepSummary = parsedSteps.withIndex().joinToString(" → ") { (i, step) ->
                    val water = step.waterAmount?.let { "${it}ml" } ?: "至总水量"
                    "${i + 1}: ${water}/${step.durationSeconds}s"
                }
                Text(
                    stepSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
