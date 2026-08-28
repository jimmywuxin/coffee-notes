package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DatePicker
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.BrewMethod
import com.coffeelab.coffeenotes.data.entity.Grinder
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.util.DateUtils
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import com.coffeelab.coffeenotes.data.entity.Equipment
import com.coffeelab.coffeenotes.viewmodel.BrewMethodViewModel
import com.coffeelab.coffeenotes.viewmodel.EquipmentViewModel
import com.coffeelab.coffeenotes.ui.component.StarRatingRow
import com.coffeelab.coffeenotes.ui.component.CompactDatePicker
import com.coffeelab.coffeenotes.viewmodel.GrinderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Extraction suggestion data class
private data class ExtractionSuggestion(
    val dose: Float?,
    val brewRatio: String?,
    val waterAmount: Float?,
    val brewTime: Int?,
    val waterTemp: Int?,
    val pouringDurationSeconds: Int?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrewEditScreen(
    navController: NavController,
    recordId: Long,
    beanId: Long,
    brewViewModel: BrewViewModel = viewModel(),
    beanViewModel: BeanViewModel = viewModel(),
    methodViewModel: BrewMethodViewModel = viewModel(),
    equipmentViewModel: EquipmentViewModel = viewModel(),
    grinderViewModel: GrinderViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val beans by beanViewModel.allBeans.collectAsStateWithLifecycle(initialValue = emptyList())
    val methods by methodViewModel.allMethods.collectAsStateWithLifecycle(initialValue = emptyList())

    val isEditing = recordId > 0
    var showDeleteDialog by remember { mutableStateOf(false) }

    // State
    var selectedBeanId by remember { mutableStateOf(beanId) }
    var selectedMethodId by remember { mutableStateOf(-1L) }
    var methodSelectedByUser by remember { mutableStateOf(false) } // 仅用户主动选择手法时自动填充
    var selectedEquipmentId by remember { mutableStateOf<Long?>(null) }
    // 冲煮时间（可修改，默认当前时间；补录历史记录用）
    var recordDateTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var coffeeWeight by remember { mutableStateOf("") }
    var coffeeWaterRatio by remember { mutableStateOf("") }
    var waterAmount by remember { mutableStateOf("") }
    var waterTemp by remember { mutableStateOf("") }
    var selectedGrinderId by remember { mutableStateOf<Long?>(null) }
    var grindSize by remember { mutableStateOf("") }
    var extractionTime by remember { mutableStateOf("") }
    var pouringDurationSeconds by remember { mutableStateOf("") }
    var flavorNotes by remember { mutableStateOf("") }
    var showCustomRatio by remember { mutableStateOf(false) }
    // 品鉴评分折叠区（编辑已有评分时自动展开）
    var ratingExpanded by remember { mutableStateOf(false) }

    // Rating states (1-5, 0 = not rated)
    var acidity by remember { mutableIntStateOf(0) }
    var sweetness by remember { mutableIntStateOf(0) }
    var bitterness by remember { mutableIntStateOf(0) }
    var mouthfeel by remember { mutableIntStateOf(0) }
    var aftertaste by remember { mutableIntStateOf(0) }
    var overall by remember { mutableIntStateOf(0) }

    // New: iced & bypass
    var isIced by remember { mutableStateOf(false) }
    var iceAmount by remember { mutableStateOf("100") }
    var bypassAmount by remember { mutableStateOf("") }

    // New: reverse ratio calculation
    var calculatedRatio by remember { mutableStateOf("") }

    // ===== Brew Timer State (two-phase) =====
    var timerExpanded by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var timerRunning by remember { mutableStateOf(false) }
    var pourPhaseSeconds by remember { mutableIntStateOf(0) }  // 注水阶段秒数（暂停后固定）
    var brewPhaseSeconds by remember { mutableIntStateOf(0) }  // 萃取阶段秒数（暂停后固定）
    var currentPhase by remember { mutableIntStateOf(0) }      // 0=注水, 1=萃取
    val haptic = LocalHapticFeedback.current

    // Timer coroutine
    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(1000L)
            timerSeconds++
        }
    }

    fun formatTimer(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }

    fun startTimer() {
        timerRunning = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun stopTimer() {
        timerRunning = false
        if (currentPhase == 0) {
            pourPhaseSeconds = timerSeconds
        } else {
            brewPhaseSeconds = timerSeconds
        }
    }

    fun nextPhase() {
        stopTimer()
        currentPhase = 1
        // 继续计时（不重置，累计计时）
        startTimer()
    }

    fun resetTimer() {
        timerRunning = false
        timerSeconds = 0
        pourPhaseSeconds = 0
        brewPhaseSeconds = 0
        currentPhase = 0
    }

    // Extraction suggestion from selected bean
    var selectedBeanExtraction by remember { mutableStateOf<ExtractionSuggestion?>(null) }

    // Equipment list
    val equipmentList by equipmentViewModel.allEquipment.collectAsStateWithLifecycle(initialValue = emptyList())

    // Grinder list
    val grinderList by grinderViewModel.allGrinders.collectAsStateWithLifecycle(initialValue = emptyList())

    // Auto-calculate waterAmount when coffeeWeight or coffeeWaterRatio changes
    LaunchedEffect(coffeeWeight, coffeeWaterRatio) {
        val weight = coffeeWeight.toDoubleOrNull() ?: 0.0
        val ratio = coffeeWaterRatio.toDoubleOrNull() ?: 0.0
        if (weight > 0 && ratio > 0) {
            val calculated = weight * ratio
            // Only auto-fill if user hasn't manually edited waterAmount
            // (we detect manual edit by checking if current value differs from last calculation)
            waterAmount = String.format("%.1f", calculated)
        }
    }

    // Load existing record
    LaunchedEffect(recordId) {
        if (isEditing) {
            val record = brewViewModel.getRecord(recordId)
            record?.let { r ->
                selectedBeanId = r.beanId
                recordDateTime = r.dateTime
                selectedMethodId = r.methodId ?: -1L
                selectedEquipmentId = r.equipmentId
                coffeeWeight = if (r.coffeeWeight > 0) r.coffeeWeight.toString() else ""
                coffeeWaterRatio = if (r.coffeeWaterRatio > 0) {
                    val s = r.coffeeWaterRatio.toString()
                    if (s.endsWith(".0")) s.dropLast(2) else s
                } else ""
                waterAmount = if (r.waterAmount > 0) r.waterAmount.toString() else ""
                waterTemp = if (r.waterTemp > 0) r.waterTemp.toString() else ""
                selectedGrinderId = r.grinderId
                grindSize = r.grindSize
                extractionTime = if (r.extractionTime > 0) r.extractionTime.toString() else ""
                pouringDurationSeconds = r.pouringDurationSeconds?.toString() ?: ""
                flavorNotes = r.flavorNotes
                acidity = r.acidity
                sweetness = r.sweetness
                bitterness = r.bitterness
                mouthfeel = r.mouthfeel
                aftertaste = r.aftertaste
                overall = r.overallRating
                isIced = r.isIced
                iceAmount = if (r.iceAmount > 0) r.iceAmount.toString() else "100"
                bypassAmount = if (r.bypassAmount > 0) r.bypassAmount.toString() else ""
                ratingExpanded = r.overallRating > 0
            }
        }
    }

    // Load extraction suggestion when selected bean changes
    LaunchedEffect(selectedBeanId, beans) {
        if (!isEditing) {
            val bean = beans.find { it.id == selectedBeanId }
            if (bean != null && (bean.dose != null || bean.brewRatio != null || bean.waterAmount != null || bean.brewTime != null || bean.waterTemp != null || bean.pouringDurationSeconds != null)) {
                selectedBeanExtraction = ExtractionSuggestion(
                    dose = bean.dose,
                    brewRatio = bean.brewRatio,
                    waterAmount = bean.waterAmount,
                    brewTime = bean.brewTime,
                    waterTemp = bean.waterTemp,
                    pouringDurationSeconds = bean.pouringDurationSeconds
                )
            } else {
                selectedBeanExtraction = null
            }
        }
    }

    // 当用户选择冲煮手法时（或加载已有记录时），自动填入手法参数
    LaunchedEffect(selectedMethodId, methods) {
        if (!isEditing && selectedMethodId > 0) {
            val method = methods.find { it.id == selectedMethodId }
            method?.let { m ->
                val steps = com.coffeelab.coffeenotes.data.Converters.parseSteps(m.steps)
                // 取最后一个有注水量的步骤的值
                val lastWaterAmount = steps.lastOrNull { it.waterAmount != null }?.waterAmount
                // 取最后一个有时长的步骤的值
                val lastDuration = steps.lastOrNull { it.durationSeconds > 0 }?.durationSeconds
                // 仅当字段为空时才填充（不覆盖用户已编辑的值）
                if (coffeeWeight.isEmpty() && m.coffeeWeight != null) {
                    coffeeWeight = m.coffeeWeight.toString()
                }
                if (coffeeWaterRatio.isEmpty() && m.coffeeWaterRatio != null) {
                    val r = m.coffeeWaterRatio
                    coffeeWaterRatio = if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
                }
                if (waterTemp.isEmpty() && m.waterTemp != null) {
                    waterTemp = m.waterTemp.toString()
                }
                if (waterAmount.isEmpty() && lastWaterAmount != null) {
                    waterAmount = lastWaterAmount.toString()
                }
                if (extractionTime.isEmpty() && lastDuration != null) {
                    extractionTime = lastDuration.toString()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Coffee, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(if (isEditing) "编辑冲煮记录" else "新增冲煮记录") } },
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
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
// 冲煮时间（可修改，补录历史记录用）
            Text("冲煮时间", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { showDateTimePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.EditCalendar,
                        contentDescription = "修改时间",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = DateUtils.formatDateTime(recordDateTime),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // 风味笔记置顶：先写感受，参数后补（brew-guide 风格）
            Text("风味笔记", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = flavorNotes,
                onValueChange = { flavorNotes = it },
                placeholder = { Text("记录一下这杯的感受…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            // Select Bean
            Text("选择咖啡豆", style = MaterialTheme.typography.titleMedium)
            if (beans.isEmpty()) {
                Text("还没有咖啡豆，请先添加豆子", color = MaterialTheme.colorScheme.error)
                Button(
                    onClick = { navController.navigate(Screen.BeanEdit.createRoute()) }
                ) { Text("添加豆子") }
            } else {
                var beanExpanded by remember { mutableStateOf(false) }
                val selectedBeanName = beans.find { it.id == selectedBeanId }?.let {
                    "${it.roaster} - ${it.name}"
                } ?: "请选择豆子"

                ExposedDropdownMenuBox(
                    expanded = beanExpanded,
                    onExpandedChange = { beanExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedBeanName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("咖啡豆") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = beanExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = beanExpanded,
                        onDismissRequest = { beanExpanded = false },
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        beans.forEach { bean ->
                            DropdownMenuItem(
                                text = {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "${bean.roaster} - ${bean.name}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                onClick = {
                                    selectedBeanId = bean.id
                                    beanExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Extraction suggestion card (display only, for reference)
            selectedBeanExtraction?.let { suggestion ->
                val hasAny = suggestion.dose != null || suggestion.brewRatio != null ||
                    suggestion.waterAmount != null || suggestion.brewTime != null || suggestion.waterTemp != null || suggestion.pouringDurationSeconds != null
                if (hasAny) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "萃取参考",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                suggestion.dose?.let { doseVal ->
                                    Column {
                                        Text("粉量", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                        Text("${doseVal}g", style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                suggestion.brewRatio?.let { ratioVal ->
                                    Column {
                                        Text("比例", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                        val displayRatio = if (ratioVal.startsWith("1:") || ratioVal.startsWith("1：")) ratioVal else "1:$ratioVal"
                                        Text(displayRatio, style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                suggestion.waterAmount?.let { waterVal ->
                                    Column {
                                        Text("注水量", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                        Text("${waterVal}ml", style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                suggestion.pouringDurationSeconds?.let { pourVal ->
                                    Column {
                                        Text("注水时长", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                        Text("${pourVal}秒", style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                suggestion.brewTime?.let { timeVal ->
                                    val mins = timeVal / 60
                                    val secs = timeVal % 60
                                    val timeStr = if (mins > 0) "${mins}分${secs}秒" else "${secs}秒"
                                    Column {
                                        Text("时间", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                        Text(timeStr, style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                suggestion.waterTemp?.let { tempVal ->
                                    Column {
                                        Text("水温", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                        Text("${tempVal}°C", style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Select Method
            Text("选择冲煮手法", style = MaterialTheme.typography.titleMedium)
            var methodExpanded by remember { mutableStateOf(false) }
            val selectedMethodName = methods.find { it.id == selectedMethodId }?.name ?: "请选择冲煮手法（可选）"
            ExposedDropdownMenuBox(
                expanded = methodExpanded,
                onExpandedChange = { methodExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedMethodName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("冲煮手法") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false },
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                    DropdownMenuItem(
                        text = { Text("不选择") },
                        onClick = {
                            selectedMethodId = -1L
                            methodExpanded = false
                        }
                    )
                    methods.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.name) },
                            onClick = {
                                selectedMethodId = method.id
                                methodSelectedByUser = true
                                methodExpanded = false
                            }
                        )
                    }
                }
            }

            // Method detail card (shown when selected)
            val selectedMethod = methods.find { it.id == selectedMethodId }
            if (selectedMethod != null) {
                val steps = com.coffeelab.coffeenotes.data.Converters.parseSteps(selectedMethod.steps)
                if (steps.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                selectedMethod.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(2.dp))
                            steps.forEachIndexed { index, step ->
                                val waterStr = step.waterAmount?.let { "${it}ml" } ?: "至总水量"
                                val descStr = step.description?.let { " · $it" } ?: ""
                                Text(
                                    "步骤${index + 1}：$waterStr · ${step.durationSeconds}秒$descStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Equipment
            Text("器具", style = MaterialTheme.typography.titleMedium)
            var equipmentExpanded by remember { mutableStateOf(false) }
            val selectedEqName = equipmentList.find { it.id == selectedEquipmentId }?.name ?: ""
            ExposedDropdownMenuBox(
                expanded = equipmentExpanded,
                onExpandedChange = { equipmentExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedEqName.ifEmpty { "请选择器具（可选）" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("器具") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipmentExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                        expanded = equipmentExpanded,
                        onDismissRequest = { equipmentExpanded = false },
                        modifier = Modifier.offset(y = 4.dp).heightIn(max = 240.dp)
                    ) {
                    DropdownMenuItem(
                        text = { Text("不选择") },
                        onClick = {
                            selectedEquipmentId = null
                            equipmentExpanded = false
                        }
                    )
                    equipmentList.forEach { eq ->
                        DropdownMenuItem(
                            text = { Text(eq.name) },
                            onClick = {
                                selectedEquipmentId = eq.id
                                equipmentExpanded = false
                            }
                        )
                    }
                }
            }

            // Brew Parameters
            Text("冲煮参数", style = MaterialTheme.typography.titleMedium)
            // ratioDisplay 存分母数值字符串（如 "15"、"4.5"），显示时加 "1:" 前缀
            val ratioOptions = listOf("2", "15", "16", "17")
            Text("粉水比", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ratioOptions.forEach { ratio ->
                    FilterChip(
                        selected = coffeeWaterRatio == ratio,
                        onClick = {
                            coffeeWaterRatio = ratio
                            showCustomRatio = false
                        },
                        label = { Text("1:$ratio") }
                    )
                }
                // 自定义粉水比
                val isCustomRatio = coffeeWaterRatio.isNotEmpty() && !ratioOptions.contains(coffeeWaterRatio)
                FilterChip(
                    selected = showCustomRatio || isCustomRatio,
                    onClick = {
                        coffeeWaterRatio = ""
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
                                value = coffeeWaterRatio,
                                onValueChange = {
                                    coffeeWaterRatio = it
                                    if (it.isNotEmpty()) showCustomRatio = true
                                },
                                textStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(56.dp)
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = coffeeWeight,
                    onValueChange = { coffeeWeight = it },
                    label = { Text("粉量 (g)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = waterAmount,
                    onValueChange = { waterAmount = it },
                    label = { Text("注水量 (ml)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            // Reverse calculation: derive ratio from actual coffeeWeight + waterAmount
            val weight = coffeeWeight.toDoubleOrNull() ?: 0.0
            val amount = waterAmount.toDoubleOrNull() ?: 0.0
            val derivedRatio = if (weight > 0 && amount > 0) String.format("%.1f", amount / weight) else ""
            if (derivedRatio.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "实际粉水比 1:${derivedRatio}（粉${coffeeWeight}g + 水${waterAmount}ml）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            coffeeWaterRatio = derivedRatio
                            showCustomRatio = true
                        }
                    ) {
                        Text("应用到粉水比")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pouringDurationSeconds,
                    onValueChange = { pouringDurationSeconds = it },
                    label = { Text("注水时长 (s)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = extractionTime,
                    onValueChange = { extractionTime = it },
                    label = { Text("萃取时长 (s)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            // ===== Brew Timer (two-phase, collapsible) =====
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                onClick = { timerExpanded = !timerExpanded }
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (currentPhase == 0) "☕ 注水中" else "⏳ 萃取中",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatTimer(timerSeconds),
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (timerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Icon(
                                if (timerExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = timerExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            // Phase labels
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "注水：${formatTimer(pourPhaseSeconds)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    "萃取：${formatTimer(brewPhaseSeconds)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Phase indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f).height(4.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = if (currentPhase == 0) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.outlineVariant
                                ) {}
                                Surface(
                                    modifier = Modifier.weight(1f).height(4.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = if (currentPhase == 1) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.outlineVariant
                                ) {}
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Control buttons
                            if (!timerRunning) {
                                // Stopped: show Start + Next Phase + Reset
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { startTimer() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("开始")
                                    }

                                    if (currentPhase == 0 && pourPhaseSeconds > 0) {
                                        OutlinedButton(
                                            onClick = { nextPhase() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("下一阶段")
                                        }
                                    }

                                    if (timerSeconds > 0) {
                                        OutlinedButton(
                                            onClick = { resetTimer() },
                                            modifier = Modifier.weight(0.6f)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            } else {
                                // Running: show Pause
                                Button(
                                    onClick = { stopTimer() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("暂停")
                                }
                            }

                            // Apply buttons (always visible)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { pouringDurationSeconds = pourPhaseSeconds.toString() },
                                    enabled = pourPhaseSeconds > 0,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("→ 填入注水时长")
                                }
                                TextButton(
                                    onClick = { extractionTime = brewPhaseSeconds.toString() },
                                    enabled = brewPhaseSeconds > 0,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("→ 填入萃取时长")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(0.dp))

            OutlinedTextField(
                value = waterTemp,
                onValueChange = { waterTemp = it },
                label = { Text("水温 (℃)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Grinder + Grind Size
            Text("研磨", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Grinder dropdown
                var grinderExpanded by remember { mutableStateOf(false) }
                val selectedGrName = grinderList.find { it.id == selectedGrinderId }?.name ?: ""
                ExposedDropdownMenuBox(
                    expanded = grinderExpanded,
                    onExpandedChange = { grinderExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedGrName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("磨豆机") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = grinderExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = grinderExpanded,
                        onDismissRequest = { grinderExpanded = false },
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("不选择") },
                            onClick = {
                                selectedGrinderId = null
                                grinderExpanded = false
                            }
                        )
                        grinderList.forEach { gr ->
                            DropdownMenuItem(
                                text = { Text(gr.name) },
                                onClick = {
                                    selectedGrinderId = gr.id
                                    grinderExpanded = false
                                }
                            )
                        }
                    }
                }
                // Grind size number input
                OutlinedTextField(
                    value = grindSize,
                    onValueChange = { grindSize = it },
                    label = { Text("格数") },
                    modifier = Modifier.weight(0.6f),
                    singleLine = true,
                    placeholder = { Text("如 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Ice & Bypass
            Text("冰饮 & Bypass", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("加冰", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = isIced,
                    onCheckedChange = { isIced = it }
                )
                if (isIced) {
                    OutlinedTextField(
                        value = iceAmount,
                        onValueChange = { iceAmount = it },
                        label = { Text("冰量 (g)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
            OutlinedTextField(
                value = bypassAmount,
                onValueChange = { bypassAmount = it },
                label = { Text("Bypass 水量 (ml)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Tasting Scores（折叠区：收起时只显示摘要，展开后打分）
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                onClick = { ratingExpanded = !ratingExpanded }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "品鉴评分",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = buildString {
                                val parts = mutableListOf<String>()
                                if (acidity > 0) parts.add("酸$acidity")
                                if (sweetness > 0) parts.add("甜$sweetness")
                                if (bitterness > 0) parts.add("苦$bitterness")
                                if (mouthfeel > 0) parts.add("口$mouthfeel")
                                if (aftertaste > 0) parts.add("回$aftertaste")
                                if (overall > 0) parts.add("总评★$overall")
                                append(if (parts.isEmpty()) "未评分" else parts.joinToString(" · "))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            if (ratingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = ratingExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            StarRatingRow(label = "酸度", rating = acidity, onRatingChange = { acidity = it })
                            StarRatingRow(label = "甜感", rating = sweetness, onRatingChange = { sweetness = it })
                            StarRatingRow(label = "苦味", rating = bitterness, onRatingChange = { bitterness = it })
                            StarRatingRow(label = "口感", rating = mouthfeel, onRatingChange = { mouthfeel = it })
                            StarRatingRow(label = "回甘", rating = aftertaste, onRatingChange = { aftertaste = it })
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            StarRatingRow(label = "总评 ⭐", rating = overall, onRatingChange = { overall = it }, large = true)
                        }
                    }
                }
            }

            // Save
            Button(
                onClick = {
                    scope.launch {
                        val record = BrewRecord(
                            id = if (isEditing) recordId else 0,
                            beanId = selectedBeanId,
                            methodId = if (selectedMethodId > 0) selectedMethodId else null,
                            dateTime = recordDateTime,
                            equipmentId = selectedEquipmentId,
                            coffeeWeight = coffeeWeight.toDoubleOrNull() ?: 0.0,
                            coffeeWaterRatio = coffeeWaterRatio.toDoubleOrNull() ?: 0.0,
                            waterAmount = waterAmount.toDoubleOrNull() ?: 0.0,
                            waterTemp = waterTemp.toDoubleOrNull() ?: 0.0,
                            grinderId = selectedGrinderId,
                            grindSize = grindSize,
                            extractionTime = extractionTime.toIntOrNull() ?: 0,
                            pouringDurationSeconds = pouringDurationSeconds.toIntOrNull(),
                            acidity = acidity,
                            sweetness = sweetness,
                            bitterness = bitterness,
                            mouthfeel = mouthfeel,
                            aftertaste = aftertaste,
                            overallRating = overall,
                            flavorNotes = flavorNotes,
                            isIced = isIced,
                            iceAmount = iceAmount.toIntOrNull() ?: 0,
                            bypassAmount = bypassAmount.toIntOrNull() ?: 0
                        )
                        if (isEditing) {
                            brewViewModel.updateRecord(record)
                        } else {
                            brewViewModel.saveRecord(record)
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("保存记录")
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条冲煮记录吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val record = brewViewModel.getRecord(recordId)
                            record?.let { brewViewModel.deleteRecord(it) }
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

    // 冲煮时间选择器：同一弹窗内分两步（日期 → 时间），点日期只高亮不跳页
    if (showDateTimePicker) {
        val zone = java.time.ZoneId.systemDefault()
        val initialLocalDate = java.time.Instant.ofEpochMilli(recordDateTime)
            .atZone(zone).toLocalDate()
        val initialLocalTime = java.time.Instant.ofEpochMilli(recordDateTime)
            .atZone(zone).toLocalTime()
        // 第一步选日期（点击仅高亮，不跳页），第二步定时间；
        // 弹窗放开宽度限制（usePlatformDefaultWidth=false），否则 M3 拨盘在 AlertDialog 里被压变形
        var tempDate by remember(showDateTimePicker) {
            mutableStateOf<java.time.LocalDate>(initialLocalDate)
        }
        var showTimeStep by remember(showDateTimePicker) { mutableStateOf(false) }
        val timePickerState = rememberTimePickerState(
            initialHour = initialLocalTime.hour,
            initialMinute = initialLocalTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showDateTimePicker = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            title = { Text(if (showTimeStep) "选择时间" else "选择日期") },
            text = {
                if (!showTimeStep) {
                    CompactDatePicker(
                        initialDate = initialLocalDate,
                        // 只记下选中日期用于「下一步」提交，不立刻跳转
                        onDateSelected = { tempDate = it }
                    )
                } else {
                    // 拨盘按 M3 规范垫在圆角色块容器里，避免裸漂在弹窗底色上
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TimePicker(state = timePickerState)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!showTimeStep) {
                        // 第一步：锁定所选日期，进入时间选择
                        showTimeStep = true
                    } else {
                        // 第二步：组合日期+时间，写回 recordDateTime
                        val picked = java.time.LocalDateTime.of(
                            tempDate,
                            java.time.LocalTime.of(timePickerState.hour, timePickerState.minute)
                        )
                        recordDateTime = picked.atZone(zone).toInstant().toEpochMilli()
                        showDateTimePicker = false
                    }
                }) { Text(if (showTimeStep) "确定" else "下一步") }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (!showTimeStep) {
                        showDateTimePicker = false
                    } else {
                        showTimeStep = false
                    }
                }) { Text(if (showTimeStep) "上一步" else "取消") }
            }
        )
    }
}

