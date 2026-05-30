package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.Converters
import com.coffeelab.coffeenotes.data.entity.BrewMethod
import com.coffeelab.coffeenotes.data.entity.BrewMethodStep
import com.coffeelab.coffeenotes.viewmodel.BrewMethodViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrewMethodEditScreen(
    navController: NavController,
    methodId: Long,
    viewModel: BrewMethodViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val isEditing = methodId > 0

    var name by remember { mutableStateOf("") }
    var isPreset by remember { mutableStateOf(false) }
    var steps by remember { mutableStateOf(listOf<BrewMethodStep>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    // 冲煮参数默认值
    var coffeeWeightText by remember { mutableStateOf("") }
    var coffeeWeight by remember { mutableStateOf<Double?>(null) }
    var waterTempText by remember { mutableStateOf("") }
    var waterTemp by remember { mutableStateOf<Int?>(null) }
    var coffeeWaterRatioText by remember { mutableStateOf("") }
    var coffeeWaterRatio by remember { mutableStateOf<Double?>(null) }
    var showCustomRatio by remember { mutableStateOf(false) }

    // Load existing method
    LaunchedEffect(methodId) {
        if (isEditing) {
            val method = viewModel.getMethod(methodId)
            method?.let {
                name = it.name
                isPreset = it.isPreset
                steps = Converters.parseSteps(it.steps)
                coffeeWeight = it.coffeeWeight
                coffeeWeightText = it.coffeeWeight?.toString() ?: ""
                waterTemp = it.waterTemp
                waterTempText = it.waterTemp?.toString() ?: ""
                coffeeWaterRatio = it.coffeeWaterRatio
                coffeeWaterRatioText = it.coffeeWaterRatio?.let { r ->
                    val s = r.toString()
                    if (s.endsWith(".0")) s.dropLast(2) else s
                } ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccountTree, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(if (isEditing) "编辑手法" else "新建手法") } },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("手法名称 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 冲煮参数默认值
            Text("冲煮参数（选填，作为新建记录的默认值）", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = coffeeWeightText,
                    onValueChange = { newVal ->
                        coffeeWeightText = newVal
                        coffeeWeight = newVal.toDoubleOrNull()
                    },
                    label = { Text("粉量 (g)") },
                    placeholder = { Text("如 15") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = waterTempText,
                    onValueChange = { newVal ->
                        waterTempText = newVal
                        waterTemp = newVal.toIntOrNull()
                    },
                    label = { Text("水温 (℃)") },
                    placeholder = { Text("如 93") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Text("粉水比", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                val ratioOptions = listOf("2", "15", "16", "17")
                ratioOptions.forEach { ratio ->
                    FilterChip(
                        selected = coffeeWaterRatioText == ratio,
                        onClick = {
                            coffeeWaterRatio = ratio.toDoubleOrNull()
                            coffeeWaterRatioText = ratio
                        },
                        label = { Text("1:$ratio") }
                    )
                }
                // 自定义粉水比
                val isCustomRatio = coffeeWaterRatioText.isNotEmpty() && !ratioOptions.contains(coffeeWaterRatioText)
                FilterChip(
                    selected = showCustomRatio || isCustomRatio,
                    onClick = {
                        coffeeWaterRatioText = ""
                        showCustomRatio = true
                    },
                    label = { Text("自定义") }
                )
                if (showCustomRatio || isCustomRatio) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("1:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            BasicTextField(
                                value = coffeeWaterRatioText,
                                onValueChange = {
                                    coffeeWaterRatioText = it
                                    coffeeWaterRatio = it.toDoubleOrNull()
                                    if (it.isNotEmpty()) showCustomRatio = true
                                },
                                textStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(60.dp)
                            )
                        }
                    }
                }
            }

            // Steps header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("步骤", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val newStep = steps.lastOrNull()
                                ?.copy(waterAmount = null, durationSeconds = 30, description = null)
                                ?: BrewMethodStep(waterAmount = null, durationSeconds = 30)
                            steps = steps + newStep
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("新增步骤")
                    }
                }
            }

            // Step list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(steps) { index, step ->
                    StepCard(
                        stepIndex = index,
                        step = step,
                        onStepChange = { newStep ->
                            steps = steps.toMutableList().also { it[index] = newStep }
                        },
                        onDelete = {
                            steps = steps.toMutableList().also { it.removeAt(index) }
                        },
                        canDelete = steps.size > 1
                    )
                }

                if (steps.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "点击上方「新增步骤」添加第一步",
                                modifier = Modifier.padding(20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        val method = BrewMethod(
                            id = if (isEditing) methodId else 0,
                            name = name,
                            isPreset = isPreset,
                            steps = Converters.serializeSteps(steps),
                            coffeeWeight = coffeeWeight,
                            coffeeWaterRatio = coffeeWaterRatio,
                            waterTemp = waterTemp,
                            createdAt = if (isEditing) (viewModel.getMethod(methodId)?.createdAt ?: System.currentTimeMillis()) else System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        if (isEditing) {
                            viewModel.updateMethod(method)
                        } else {
                            viewModel.saveMethod(method)
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = name.isNotBlank()
            ) {
                Text("保存手法")
            }
        }
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${name}」吗？此操作无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.getMethod(methodId)?.let { viewModel.deleteMethod(it) }
                            showDeleteDialog = false
                            navController.popBackStack()
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
private fun StepCard(
    stepIndex: Int,
    step: BrewMethodStep,
    onStepChange: (BrewMethodStep) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    // 本地文本状态，独立于 step.waterAmount，避免删不干净
    var waterAmountText by remember(stepIndex) {
        mutableStateOf(step.waterAmount?.let {
            if (it == 0f) "" else it.toString()
        } ?: "")
    }
    var durationText by remember(stepIndex) {
        mutableStateOf(if (step.durationSeconds == 0) "" else step.durationSeconds.toString())
    }
    var descriptionText by remember(stepIndex) {
        mutableStateOf(step.description ?: "")
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "步骤 ${stepIndex + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 注水量 & 时间
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = waterAmountText,
                    onValueChange = { newVal ->
                        waterAmountText = newVal
                        val amount = newVal.toFloatOrNull()
                        // 空字符串 → null（至总水量），0 → null，其他数值保留
                        val finalAmount = when {
                            newVal.isEmpty() -> null
                            amount == 0f -> null
                            else -> amount
                        }
                        onStepChange(step.copy(waterAmount = finalAmount))
                    },
                    label = { Text("注水量 (ml)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("空=至总水量") }
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { newVal ->
                        durationText = newVal
                        val secs = newVal.toIntOrNull() ?: 0
                        onStepChange(step.copy(durationSeconds = secs))
                    },
                    label = { Text("时间 (s)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // 至总水量提示
            if (waterAmountText.isEmpty()) {
                Text(
                    "此次注水至总水量",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // 步骤描述
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { newVal ->
                    descriptionText = newVal
                    onStepChange(step.copy(description = newVal.ifBlank { null }))
                },
                label = { Text("水流 / 注水方式（选填）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("如：大水流、中心注入、画圈注水") }
            )
        }
    }
}
