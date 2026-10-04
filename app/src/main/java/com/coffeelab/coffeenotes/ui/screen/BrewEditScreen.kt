package com.coffeelab.coffeenotes.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.Converters
import com.coffeelab.coffeenotes.data.entity.BedShape
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.ui.component.CompactDatePicker
import com.coffeelab.coffeenotes.ui.component.StarRatingRow
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.util.DateUtils
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewMethodViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import com.coffeelab.coffeenotes.viewmodel.EquipmentViewModel
import com.coffeelab.coffeenotes.viewmodel.GrinderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val beans by beanViewModel.allBeans.collectAsStateWithLifecycle(initialValue = emptyList())
    val methods by methodViewModel.allMethods.collectAsStateWithLifecycle(initialValue = emptyList())
    val equipmentList by equipmentViewModel.allEquipment.collectAsStateWithLifecycle(initialValue = emptyList())
    val grinderList by grinderViewModel.allGrinders.collectAsStateWithLifecycle(initialValue = emptyList())

    // 预构建查找表，避免每次重组线性扫描（同 BrewListScreen.beansById 的做法）
    val beansById = remember(beans) { beans.associateBy { it.id } }
    val methodsById = remember(methods) { methods.associateBy { it.id } }
    val equipmentById = remember(equipmentList) { equipmentList.associateBy { it.id } }
    val grindersById = remember(grinderList) { grinderList.associateBy { it.id } }
    // 归档豆子排到最后，新增记录时默认看到「在喝」的豆子
    val beanOptions = remember(beans) { beans.sortedBy { it.isArchived } }

    val isEditing = recordId > 0

    // ===== 表单状态：全部 rememberSaveable，横竖屏切换 / 进程回收后不丢 =====
    var selectedBeanId by rememberSaveable { mutableStateOf(beanId) }
    var selectedMethodId by rememberSaveable { mutableStateOf(-1L) }
    var methodSelectedByUser by rememberSaveable { mutableStateOf(false) }
    var selectedEquipmentId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedGrinderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recordDateTime by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var dateTimeTouched by rememberSaveable { mutableStateOf(false) }
    var coffeeWeight by rememberSaveable { mutableStateOf("") }
    var coffeeWaterRatio by rememberSaveable { mutableStateOf("") }
    var waterAmount by rememberSaveable { mutableStateOf("") }
    // 用户是否自己填过注水量：填过就不再被「粉量 × 比例」覆盖
    var waterAmountTouched by rememberSaveable { mutableStateOf(false) }
    var waterTemp by rememberSaveable { mutableStateOf("") }
    var grindSize by rememberSaveable { mutableStateOf("") }
    var extractionTime by rememberSaveable { mutableStateOf("") }
    var pouringDurationSeconds by rememberSaveable { mutableStateOf("") }
    var flavorNotes by rememberSaveable { mutableStateOf("") }
    var bedShape by rememberSaveable { mutableStateOf("") }
    var showCustomRatio by rememberSaveable { mutableStateOf(false) }
    var isIced by rememberSaveable { mutableStateOf(false) }
    var iceAmount by rememberSaveable { mutableStateOf("100") }
    var bypassAmount by rememberSaveable { mutableStateOf("") }
    var acidity by rememberSaveable { mutableIntStateOf(0) }
    var sweetness by rememberSaveable { mutableIntStateOf(0) }
    var bitterness by rememberSaveable { mutableIntStateOf(0) }
    var mouthfeel by rememberSaveable { mutableIntStateOf(0) }
    var aftertaste by rememberSaveable { mutableIntStateOf(0) }
    var overall by rememberSaveable { mutableIntStateOf(0) }
    var ratingExpanded by rememberSaveable { mutableStateOf(false) }
    var paramsExpanded by rememberSaveable { mutableStateOf(true) }
    var timerExpanded by rememberSaveable { mutableStateOf(false) }

    // 下拉展开态
    var beanExpanded by rememberSaveable { mutableStateOf(false) }
    var methodExpanded by rememberSaveable { mutableStateOf(false) }
    var equipmentExpanded by rememberSaveable { mutableStateOf(false) }
    var grinderExpanded by rememberSaveable { mutableStateOf(false) }

    // 弹窗 / 加载态
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(isEditing) }
    var loadFailed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    // 载入时的「未改动」基线，用于返回时判断是否需要二次确认
    var baseline by remember { mutableStateOf("") }
    // 编辑时保留原始创建时间，别被 updatedAt 一起覆盖
    var loadedCreatedAt by remember { mutableStateOf(0L) }

    var selectedBeanExtraction by remember { mutableStateOf<ExtractionSuggestion?>(null) }
    var lastRecord by remember { mutableStateOf<BrewRecord?>(null) }

    // ===== Brew Timer State (two-phase) =====
    var timerSeconds by rememberSaveable { mutableIntStateOf(0) }
    var pourPhaseSeconds by rememberSaveable { mutableIntStateOf(0) }  // 注水段时长
    var brewPhaseSeconds by rememberSaveable { mutableIntStateOf(0) }  // 萃取段时长（不含注水）
    var currentPhase by rememberSaveable { mutableIntStateOf(0) }      // 0=注水, 1=萃取
    var timerRunning by remember { mutableStateOf(false) }
    var timerBaseElapsed by remember { mutableLongStateOf(0L) }
    var timerBaseSeconds by remember { mutableIntStateOf(0) }

    // 表单快照：只包含会写库的字段，用于脏检查
    fun formSnapshot(): String = listOf(
        selectedBeanId, selectedMethodId, selectedEquipmentId, selectedGrinderId,
        recordDateTime, coffeeWeight, coffeeWaterRatio, waterAmount, waterTemp, grindSize,
        extractionTime, pouringDurationSeconds, flavorNotes, bedShape, isIced, iceAmount,
        bypassAmount, acidity, sweetness, bitterness, mouthfeel, aftertaste, overall
    ).joinToString("|")

    val dirty by remember {
        derivedStateOf { !isLoading && !loadFailed && formSnapshot() != baseline }
    }

    // 正在计时时保持屏幕常亮（否则冲煮中途息屏，看不见计时）
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(timerRunning, activity) {
        if (timerRunning) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // ===== 计时器：以 elapsedRealtime 为基准，不再逐秒累加（避免漂移、后台也准） =====
    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            timerSeconds = timerBaseSeconds +
                ((SystemClock.elapsedRealtime() - timerBaseElapsed) / 1000L).toInt()
            delay(200L)
        }
    }

    fun formatTimer(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    fun syncTimer() {
        if (timerRunning) {
            timerSeconds = timerBaseSeconds +
                ((SystemClock.elapsedRealtime() - timerBaseElapsed) / 1000L).toInt()
        }
    }

    fun startTimer() {
        timerBaseSeconds = timerSeconds
        timerBaseElapsed = SystemClock.elapsedRealtime()
        timerRunning = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun stopTimer() {
        syncTimer()
        timerRunning = false
        if (currentPhase == 0) {
            pourPhaseSeconds = timerSeconds
            // 暂停即回填，但只在用户没自己填过的时候（不覆盖手输值）
            if (pouringDurationSeconds.isEmpty() && timerSeconds > 0) {
                pouringDurationSeconds = timerSeconds.toString()
            }
        } else {
            brewPhaseSeconds = (timerSeconds - pourPhaseSeconds).coerceAtLeast(0)
            // 「萃取时长」口径 = 总时长（含注水）
            if (extractionTime.isEmpty() && timerSeconds > 0) {
                extractionTime = timerSeconds.toString()
            }
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun nextPhase() {
        if (currentPhase != 0) return
        stopTimer()
        currentPhase = 1
        startTimer()
    }

    fun resetTimer() {
        timerRunning = false
        timerSeconds = 0
        pourPhaseSeconds = 0
        brewPhaseSeconds = 0
        currentPhase = 0
    }

    // 粉量 / 粉水比变化时反算注水量——仅在用户没自己填过时
    LaunchedEffect(coffeeWeight, coffeeWaterRatio, waterAmountTouched) {
        if (waterAmountTouched) return@LaunchedEffect
        val weight = coffeeWeight.toDoubleOrNull() ?: 0.0
        val ratio = coffeeWaterRatio.toDoubleOrNull() ?: 0.0
        waterAmount = if (weight > 0 && ratio > 0) formatNum(weight * ratio) else ""
    }

    // ===== 载入已有记录 =====
    LaunchedEffect(recordId) {
        if (isEditing) {
            val r = brewViewModel.getRecord(recordId)
            if (r == null) {
                // 记录已被删除（如另一处删掉后再返回）：不要用空表单覆盖，直接拦下
                loadFailed = true
                isLoading = false
                return@LaunchedEffect
            }
            selectedBeanId = r.beanId
            recordDateTime = r.dateTime
            dateTimeTouched = true
            selectedMethodId = r.methodId ?: -1L
            selectedEquipmentId = r.equipmentId
            selectedGrinderId = r.grinderId
            coffeeWeight = if (r.coffeeWeight > 0) formatNum(r.coffeeWeight) else ""
            coffeeWaterRatio = if (r.coffeeWaterRatio > 0) formatNum(r.coffeeWaterRatio) else ""
            waterAmount = if (r.waterAmount > 0) formatNum(r.waterAmount) else ""
            // 库里存的注水量是实测值，不能被「粉量 × 比例」重算掉
            waterAmountTouched = r.waterAmount > 0
            waterTemp = if (r.waterTemp > 0) formatNum(r.waterTemp) else ""
            grindSize = r.grindSize
            extractionTime = if (r.extractionTime > 0) r.extractionTime.toString() else ""
            pouringDurationSeconds = r.pouringDurationSeconds?.toString() ?: ""
            flavorNotes = r.flavorNotes
            bedShape = r.bedShape
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
            loadedCreatedAt = r.createdAt
        }
        isLoading = false
        baseline = formSnapshot()
    }

    // ===== 萃取参考（新增/编辑都展示，便于和现有记录对照） =====
    LaunchedEffect(selectedBeanId, beansById) {
        val bean = beansById[selectedBeanId]
        selectedBeanExtraction = if (bean != null && (
                bean.dose != null || bean.brewRatio != null || bean.waterAmount != null ||
                    bean.brewTime != null || bean.waterTemp != null || bean.pouringDurationSeconds != null
                )
        ) {
            ExtractionSuggestion(
                dose = bean.dose,
                brewRatio = bean.brewRatio,
                waterAmount = bean.waterAmount,
                brewTime = bean.brewTime,
                waterTemp = bean.waterTemp,
                pouringDurationSeconds = bean.pouringDurationSeconds
            )
        } else {
            null
        }
    }

    // ===== 同豆子最近一杯（沿用上一杯） =====
    LaunchedEffect(selectedBeanId, recordId) {
        lastRecord = if (selectedBeanId > 0) {
            brewViewModel.getLastRecordForBean(selectedBeanId, if (isEditing) recordId else 0L)
        } else {
            null
        }
    }

    // ===== 选中手法时带入参数（编辑模式下仅在用户主动改选时补全空字段） =====
    LaunchedEffect(selectedMethodId, methods, methodSelectedByUser) {
        if (selectedMethodId <= 0) return@LaunchedEffect
        if (isEditing && !methodSelectedByUser) return@LaunchedEffect
        val m = methodsById[selectedMethodId] ?: return@LaunchedEffect
        val steps = Converters.parseSteps(m.steps)
        val lastWaterAmount = steps.lastOrNull { it.waterAmount != null }?.waterAmount
        // 「萃取时长」按总时长口径：各步骤时长之和
        val totalDuration = steps.sumOf { it.durationSeconds }
        if (coffeeWeight.isEmpty() && m.coffeeWeight != null) coffeeWeight = formatNum(m.coffeeWeight)
        if (coffeeWaterRatio.isEmpty() && m.coffeeWaterRatio != null) coffeeWaterRatio = formatNum(m.coffeeWaterRatio)
        if (waterTemp.isEmpty() && m.waterTemp != null) waterTemp = m.waterTemp.toString()
        if (waterAmount.isEmpty() && lastWaterAmount != null) {
            waterAmount = formatNum(lastWaterAmount.toDouble())
            waterAmountTouched = true
        }
        if (extractionTime.isEmpty() && totalDuration > 0) extractionTime = totalDuration.toString()
    }

    // ===== 带入操作 =====
    fun applySuggestion() {
        val s = selectedBeanExtraction ?: return
        if (coffeeWeight.isEmpty() && s.dose != null) coffeeWeight = formatNum(s.dose.toDouble())
        if (coffeeWaterRatio.isEmpty() && s.brewRatio != null) coffeeWaterRatio = normalizeRatio(s.brewRatio)
        if (waterAmount.isEmpty() && s.waterAmount != null) {
            waterAmount = formatNum(s.waterAmount.toDouble())
            waterAmountTouched = true
        }
        if (waterTemp.isEmpty() && s.waterTemp != null) waterTemp = s.waterTemp.toString()
        if (extractionTime.isEmpty() && s.brewTime != null) extractionTime = s.brewTime.toString()
        if (pouringDurationSeconds.isEmpty() && s.pouringDurationSeconds != null) {
            pouringDurationSeconds = s.pouringDurationSeconds.toString()
        }
    }

    fun applyLastRecord() {
        val r = lastRecord ?: return
        if (coffeeWeight.isEmpty() && r.coffeeWeight > 0) coffeeWeight = formatNum(r.coffeeWeight)
        if (coffeeWaterRatio.isEmpty() && r.coffeeWaterRatio > 0) coffeeWaterRatio = formatNum(r.coffeeWaterRatio)
        if (waterAmount.isEmpty() && r.waterAmount > 0) {
            waterAmount = formatNum(r.waterAmount)
            waterAmountTouched = true
        }
        if (waterTemp.isEmpty() && r.waterTemp > 0) waterTemp = formatNum(r.waterTemp)
        if (grindSize.isEmpty()) grindSize = r.grindSize
        if (extractionTime.isEmpty() && r.extractionTime > 0) extractionTime = r.extractionTime.toString()
        if (pouringDurationSeconds.isEmpty() && (r.pouringDurationSeconds ?: 0) > 0) {
            pouringDurationSeconds = r.pouringDurationSeconds.toString()
        }
        if (selectedEquipmentId == null) selectedEquipmentId = r.equipmentId
        if (selectedGrinderId == null) selectedGrinderId = r.grinderId
        val lastMethodId = r.methodId ?: 0L
        if (selectedMethodId <= 0 && lastMethodId > 0) selectedMethodId = lastMethodId
    }

    val canSave = !isLoading && !loadFailed && !saving && selectedBeanId > 0

    fun save() {
        if (!canSave) return
        saving = true
        // 新增记录：用户没动过时间就按「此刻」存，而不是进页面那一刻
        if (!isEditing && !dateTimeTouched) recordDateTime = System.currentTimeMillis()
        val finalDateTime = recordDateTime
        val passedIce = iceAmount.toIntOrNull() ?: 0
        scope.launch {
            val now = System.currentTimeMillis()
            val record = BrewRecord(
                id = if (isEditing) recordId else 0,
                beanId = selectedBeanId,
                methodId = if (selectedMethodId > 0) selectedMethodId else null,
                dateTime = finalDateTime,
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
                bedShape = bedShape,
                isIced = isIced,
                // 没开加冰就别把冰量写进库（否则关掉开关仍留一条脏数据）
                iceAmount = if (isIced) passedIce else 0,
                bypassAmount = bypassAmount.toIntOrNull() ?: 0,
                createdAt = if (isEditing && loadedCreatedAt > 0) loadedCreatedAt else now,
                updatedAt = now
            )
            // 写库失败（如豆子被并发删除撞外键）时把 saving 放回去，否则按钮永久卡死
            val ok = runCatching {
                if (isEditing) brewViewModel.updateRecord(record) else brewViewModel.saveRecord(record)
            }.isSuccess
            if (ok) navController.popBackStack() else saving = false
        }
    }

    fun attemptLeave() {
        // 正在写库时不允许离开：这个 scope 随页面一起销毁，会把写入协程一起取消
        if (saving) return
        if (dirty) showDiscardDialog = true else navController.popBackStack()
    }

    BackHandler(enabled = dirty || saving) {
        if (!saving && dirty) showDiscardDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = { attemptLeave() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Coffee, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isEditing) "编辑冲煮记录" else "新增冲煮记录")
                    }
                },
                actions = {
                    if (isEditing && !loadFailed) {
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
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = { save() },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(48.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (selectedBeanId <= 0) {
                            // 没选豆子就保存会撞 beanId 外键，先在按钮上说清原因
                            "请先选择咖啡豆"
                        } else if (isEditing) {
                            "保存修改"
                        } else {
                            "保存记录"
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (loadFailed) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "这条冲煮记录已不存在",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "可能已在别处被删除，为避免覆盖数据，这里不再允许保存。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { navController.popBackStack() }) { Text("返回") }
                    }
                }
            }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                Button(onClick = { navController.navigate(Screen.BeanEdit.createRoute()) }) {
                    Text("添加豆子")
                }
            } else {
                val selectedBeanName = beansById[selectedBeanId]?.let { "${it.roaster} - ${it.name}" }
                    ?: "请选择豆子"
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
                        beanOptions.forEach { bean ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${bean.roaster} - ${bean.name}" +
                                            if (bean.isArchived) "（已归档）" else "",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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

            // 沿用上一杯（同豆子最近一条）
            lastRecord?.let { r ->
                val summary = buildString {
                    append(DateUtils.formatDateTime(r.dateTime))
                    if (r.coffeeWeight > 0) append(" · ${formatNum(r.coffeeWeight)}g")
                    if (r.coffeeWaterRatio > 0) append(" · 1:${formatNum(r.coffeeWaterRatio)}")
                    if (r.waterTemp > 0) append(" · ${formatNum(r.waterTemp)}℃")
                    if (r.grindSize.isNotBlank()) append(" · ${r.grindSize}")
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "上一杯",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = { applyLastRecord() }) { Text("带入") }
                    }
                }
            }

            // Extraction suggestion card（新增/编辑都可一键应用，只填空字段）
            selectedBeanExtraction?.let { suggestion ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "萃取参考",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { applySuggestion() }) { Text("一键应用") }
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            suggestion.dose?.let { doseVal ->
                                Column {
                                    Text(
                                        "粉量", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        "${formatNum(doseVal.toDouble())}g",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            suggestion.brewRatio?.let { ratioVal ->
                                Column {
                                    Text(
                                        "比例", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        "1:${normalizeRatio(ratioVal)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            suggestion.waterAmount?.let { waterVal ->
                                Column {
                                    Text(
                                        "注水量", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        "${formatNum(waterVal.toDouble())}ml",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            suggestion.pouringDurationSeconds?.let { pourVal ->
                                Column {
                                    Text(
                                        "注水时长", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        "${pourVal}秒",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            suggestion.brewTime?.let { timeVal ->
                                Column {
                                    Text(
                                        "总时长", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        formatDuration(timeVal),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            suggestion.waterTemp?.let { tempVal ->
                                Column {
                                    Text(
                                        "水温", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        "${tempVal}°C",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Select Method
            Text("选择冲煮手法", style = MaterialTheme.typography.titleMedium)
            val selectedMethodName = methodsById[selectedMethodId]?.name ?: "请选择冲煮手法（可选）"
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
                            methodSelectedByUser = true
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
            val selectedMethod = methodsById[selectedMethodId]
            val selectedMethodSteps = remember(selectedMethod) {
                selectedMethod?.let { Converters.parseSteps(it.steps) }
            }
            if (selectedMethod != null && !selectedMethodSteps.isNullOrEmpty()) {
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
                        selectedMethodSteps.forEachIndexed { index, step ->
                            val waterStr = step.waterAmount?.let { "${formatNum(it.toDouble())}ml" } ?: "至总水量"
                            val descStr = step.description?.let { " · $it" } ?: ""
                            Text(
                                "步骤${index + 1}：$waterStr · ${step.durationSeconds}秒$descStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "合计 ${formatDuration(selectedMethodSteps.sumOf { it.durationSeconds })}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Equipment
            Text("器具", style = MaterialTheme.typography.titleMedium)
            val selectedEqName = equipmentById[selectedEquipmentId]?.name ?: ""
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
                    modifier = Modifier.heightIn(max = 240.dp)
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

            // ===== 冲煮参数（可折叠，收起时显示摘要） =====
            val paramsSummary = buildList {
                if (coffeeWeight.isNotBlank()) add("${coffeeWeight}g")
                if (coffeeWaterRatio.isNotBlank()) add("1:$coffeeWaterRatio")
                if (waterAmount.isNotBlank()) add("${waterAmount}ml")
                if (waterTemp.isNotBlank()) add("${waterTemp}℃")
                if (pouringDurationSeconds.isNotBlank()) add("注水${pouringDurationSeconds}s")
                if (extractionTime.isNotBlank()) add("总时长${extractionTime}s")
            }.joinToString(" · ").ifEmpty { "未填写" }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                onClick = { paramsExpanded = !paramsExpanded }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("冲煮参数", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        paramsSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (paramsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = paramsExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // ratioDisplay 存分母数值字符串（如 "15"、"4.5"），显示时加 "1:" 前缀
                    val ratioOptions = listOf("2", "15", "16", "17")
                    Text("粉水比", style = MaterialTheme.typography.bodyMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                        // 自定义粉水比：只切到输入态，不清掉已经输入的值
                        val isCustomRatio = coffeeWaterRatio.isNotEmpty() && !ratioOptions.contains(coffeeWaterRatio)
                        FilterChip(
                            selected = showCustomRatio || isCustomRatio,
                            onClick = { showCustomRatio = true },
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
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        "1:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    BasicTextField(
                                        value = coffeeWaterRatio,
                                        onValueChange = {
                                            coffeeWaterRatio = it
                                            if (it.isNotEmpty()) showCustomRatio = true
                                        },
                                        textStyle = MaterialTheme.typography.labelLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
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
                            onValueChange = {
                                waterAmount = it
                                waterAmountTouched = true
                            },
                            label = { Text("注水量 (ml)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    // Reverse calculation: derive ratio from actual coffeeWeight + waterAmount
                    val weight = coffeeWeight.toDoubleOrNull() ?: 0.0
                    val amount = waterAmount.toDoubleOrNull() ?: 0.0
                    val derivedRatio = if (weight > 0 && amount > 0) formatNum(amount / weight) else ""
                    if (derivedRatio.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "实际粉水比 1:$derivedRatio（粉${coffeeWeight}g + 水${waterAmount}ml）",
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
                            label = { Text("萃取时长 (s，含注水)") },
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
                                    Spacer(Modifier.width(8.dp))
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
                                        color = if (timerRunning) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
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

                                    Spacer(Modifier.height(8.dp))

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

                                    Spacer(Modifier.height(12.dp))

                                    if (!timerRunning) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { startTimer() },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(if (timerSeconds > 0) "继续" else "开始")
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
                                                    Icon(
                                                        Icons.Default.Refresh,
                                                        contentDescription = "重置",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { stopTimer() },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Pause,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("暂停")
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TextButton(
                                            onClick = { pouringDurationSeconds = pourPhaseSeconds.toString() },
                                            enabled = pourPhaseSeconds > 0,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("→ 注水时长")
                                        }
                                        TextButton(
                                            onClick = { extractionTime = timerSeconds.toString() },
                                            enabled = timerSeconds > 0,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("→ 总时长")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = waterTemp,
                        onValueChange = { waterTemp = it },
                        label = { Text("水温 (℃)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }

            // Grinder + Grind Size
            Text("研磨", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val selectedGrName = grindersById[selectedGrinderId]?.name ?: ""
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

            // 粉坑形状（前街咖啡六种粉床状态，选填）
            Text("粉坑形状", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BedShape.entries.take(3).forEach { shape ->
                    BedShapeCard(
                        shape = shape,
                        selected = bedShape == shape.key,
                        onClick = { bedShape = if (bedShape == shape.key) "" else shape.key },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BedShape.entries.drop(3).forEach { shape ->
                    BedShapeCard(
                        shape = shape,
                        selected = bedShape == shape.key,
                        onClick = { bedShape = if (bedShape == shape.key) "" else shape.key },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = BedShape.fromKey(bedShape)?.hint ?: "选填：冲煮结束后粉坑的状态，辅助诊断研磨与注水",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = bedShape.isEmpty(),
                    onClick = { bedShape = "" },
                    label = { Text("不记录") }
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
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "品鉴评分",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
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
                            StarRatingRow(
                                label = "总评 ⭐",
                                rating = overall,
                                onRatingChange = { overall = it },
                                large = true
                            )
                        }
                    }
                }
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

    // Discard Confirmation Dialog（返回时未保存）
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃修改？") },
            text = { Text("这次的改动还没有保存，返回后会丢失。") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    navController.popBackStack()
                }) { Text("放弃", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("继续编辑") }
            }
        )
    }

    // 冲煮时间选择器：同一弹窗内分两步（日期 → 时间），点日期只高亮不跳页
    if (showDateTimePicker) {
        val zone = java.time.ZoneId.systemDefault()
        val initialLocalDate = java.time.Instant.ofEpochMilli(recordDateTime).atZone(zone).toLocalDate()
        val initialLocalTime = java.time.Instant.ofEpochMilli(recordDateTime).atZone(zone).toLocalTime()
        var tempDate by remember(showDateTimePicker) { mutableStateOf(initialLocalDate) }
        var showTimeStep by remember(showDateTimePicker) { mutableStateOf(false) }
        val timePickerState = rememberTimePickerState(
            initialHour = initialLocalTime.hour,
            initialMinute = initialLocalTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showDateTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            title = { Text(if (showTimeStep) "选择时间" else "选择日期") },
            text = {
                if (!showTimeStep) {
                    CompactDatePicker(
                        initialDate = initialLocalDate,
                        onDateSelected = { tempDate = it }
                    )
                } else {
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
                        showTimeStep = true
                    } else {
                        val picked = java.time.LocalDateTime.of(
                            tempDate,
                            java.time.LocalTime.of(timePickerState.hour, timePickerState.minute)
                        )
                        recordDateTime = picked.atZone(zone).toInstant().toEpochMilli()
                        dateTimeTouched = true
                        showDateTimePicker = false
                    }
                }) { Text(if (showTimeStep) "确定" else "下一步") }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (!showTimeStep) showDateTimePicker = false else showTimeStep = false
                }) { Text(if (showTimeStep) "上一步" else "取消") }
            }
        )
    }
}

/** 数值展示：整数不带小数点，否则保留一位（240.0 → 240，242.5 → 242.5） */
private fun formatNum(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return ""
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
}

/** 把 "1:15" / "15" / "1：15" 统一成 "15" */
private fun normalizeRatio(raw: String): String {
    val cleaned = raw.trim().removePrefix("1:").removePrefix("1：").trim()
    val parsed = cleaned.toDoubleOrNull() ?: return raw
    return formatNum(parsed)
}

/** 秒 → "3分20秒" */
private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}分${s}秒" else "${s}秒"
}

/** 从 Compose 的 Context 链里找到宿主 Activity（保持屏幕常亮用） */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** 粉坑形状选择小卡：迷你粉床截面示意 + 名称 */
@Composable
private fun BedShapeCard(
    shape: BedShape,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        },
        tonalElevation = if (selected) 0.dp else 1.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
        ) {
            BedShapeIcon(
                shape = shape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = shape.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/** 粉坑形状迷你示意图（Canvas 绘制滤杯截面 + 粉床轮廓） */
@Composable
private fun BedShapeIcon(shape: BedShape, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val wallColor = Color(0xFFB8B4AF)   // 雾灰：滤杯壁
        val bedColor = Color(0xFFD4A574)    // 焦糖：粉床
        val darkColor = Color(0xFFA68B5B)   // 原木深色：粉墙/盐粒
        val waterColor = Color(0xFFA8B5A0)  // 茶绿：积水
        val stroke = 1.5.dp.toPx()

        drawLine(wallColor, Offset(w * 0.10f, h * 0.10f), Offset(w * 0.36f, h * 0.92f), stroke, StrokeCap.Round)
        drawLine(wallColor, Offset(w * 0.90f, h * 0.10f), Offset(w * 0.64f, h * 0.92f), stroke, StrokeCap.Round)

        fun flatBed(topY: Float): Path = Path().apply {
            moveTo(w * 0.16f, topY)
            lineTo(w * 0.84f, topY)
            lineTo(w * 0.64f, h * 0.90f)
            lineTo(w * 0.36f, h * 0.90f)
            close()
        }

        when (shape) {
            BedShape.DEEP_EVEN -> drawPath(Path().apply {
                moveTo(w * 0.16f, h * 0.26f)
                cubicTo(w * 0.36f, h * 1.05f, w * 0.64f, h * 1.05f, w * 0.84f, h * 0.26f)
                lineTo(w * 0.64f, h * 0.90f)
                lineTo(w * 0.36f, h * 0.90f)
                close()
            }, bedColor)
            BedShape.DEEP_OFFSET -> drawPath(Path().apply {
                moveTo(w * 0.16f, h * 0.26f)
                cubicTo(w * 0.36f, h * 0.70f, w * 0.62f, h * 1.05f, w * 0.84f, h * 0.34f)
                lineTo(w * 0.64f, h * 0.90f)
                lineTo(w * 0.36f, h * 0.90f)
                close()
            }, bedColor)
            BedShape.FLAT_WITH_WALL -> {
                drawPath(flatBed(h * 0.42f), bedColor)
                drawPath(Path().apply {
                    moveTo(w * 0.105f, h * 0.16f)
                    lineTo(w * 0.17f, h * 0.20f)
                    lineTo(w * 0.17f, h * 0.42f)
                    lineTo(w * 0.105f, h * 0.38f)
                    close()
                }, darkColor)
                drawPath(Path().apply {
                    moveTo(w * 0.895f, h * 0.16f)
                    lineTo(w * 0.83f, h * 0.20f)
                    lineTo(w * 0.83f, h * 0.42f)
                    lineTo(w * 0.895f, h * 0.38f)
                    close()
                }, darkColor)
            }
            BedShape.FLAT_NO_WALL -> drawPath(flatBed(h * 0.42f), bedColor)
            BedShape.MUD -> {
                drawPath(flatBed(h * 0.58f), bedColor)
                drawOval(waterColor, topLeft = Offset(w * 0.52f, h * 0.55f), size = Size(w * 0.30f, h * 0.14f))
            }
            BedShape.SALT -> {
                drawPath(flatBed(h * 0.42f), bedColor)
                listOf(
                    Offset(w * 0.30f, h * 0.55f),
                    Offset(w * 0.46f, h * 0.68f),
                    Offset(w * 0.60f, h * 0.55f),
                    Offset(w * 0.70f, h * 0.72f)
                ).forEach { c -> drawCircle(darkColor, radius = 1.6.dp.toPx(), center = c) }
            }
        }
    }
}
