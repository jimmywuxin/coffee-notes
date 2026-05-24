package com.coffeelab.coffeenotes.ui.screen



import android.graphics.Bitmap

import android.net.Uri

import android.os.Environment

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.runtime.snapshots.SnapshotStateList

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.NavController

import coil.compose.AsyncImage

import com.coffeelab.coffeenotes.data.entity.CoffeeBean

import com.coffeelab.coffeenotes.data.entity.RoastDegree

import com.coffeelab.coffeenotes.data.entity.ProcessMethod

import com.coffeelab.coffeenotes.data.entity.RestPeriodConfig

import com.coffeelab.coffeenotes.data.entity.PeakFlavorConfig

import com.coffeelab.coffeenotes.ui.navigation.Screen

import com.coffeelab.coffeenotes.util.BitmapLoader

import com.coffeelab.coffeenotes.util.DateUtils

import com.coffeelab.coffeenotes.util.ImageUtils

import com.coffeelab.coffeenotes.viewmodel.BeanViewModel

import kotlinx.coroutines.launch

import java.io.File



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

@Composable

fun BeanEditScreen(

    navController: NavController,

    beanId: Long,

    viewModel: BeanViewModel = viewModel()

) {

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    var showQuickAddDialog by remember { mutableStateOf(false) }

    var quickAddName by remember { mutableStateOf("") }



    var roaster by remember { mutableStateOf("") }

    var name by remember { mutableStateOf("") }

    var origin by remember { mutableStateOf("") }

    var region by remember { mutableStateOf("") }

    var estate by remember { mutableStateOf("") }

    var variety by remember { mutableStateOf("") }

    var roastLevel by remember { mutableStateOf("") }

    var roastDate by remember { mutableStateOf("") }

    var notes by remember { mutableStateOf("") }

    var imageUri by remember { mutableStateOf("") }

    var localPhotoPaths by remember { mutableStateOf<List<String>>(emptyList()) }

    var tagInput by remember { mutableStateOf("") }

    val tags = remember { mutableStateListOf<String>() }

    val selectedImpressionTagIds = remember { mutableStateListOf<Long>() }

    var originalGrindSize by remember { mutableStateOf("") }

    // 官方萃取建议

    var dose by remember { mutableStateOf("") }

    var brewRatio by remember { mutableStateOf("") }

    var waterAmount by remember { mutableStateOf("") }

    var pouringDurationSeconds by remember { mutableStateOf("") }

    var brewTime by remember { mutableStateOf("") }

    var waterTemp by remember { mutableStateOf("") }

    // 烘焙度/处理法下拉选状态

    var selectedRoastDegreeId by remember { mutableStateOf<Long?>(null) }

    var selectedProcessMethodId by remember { mutableStateOf<Long?>(null) }

    var customProcessName by remember { mutableStateOf("") }

    var customRoastLevel by remember { mutableStateOf("") }

    var roastLevelDropdownExpanded by remember { mutableStateOf(false) }

    var processDropdownExpanded by remember { mutableStateOf(false) }

    var restDaysOverride by remember { mutableStateOf<String>("") }  // 手动覆盖养豆天数

    var peakFlavorDaysOverride by remember { mutableStateOf<String>("") }  // 手动覆盖赏味天数

    // 标记用户是否手动修改过（避免自动填入覆盖用户已编辑的值）

    var userModifiedRestDays by remember { mutableStateOf(false) }

    var userModifiedPeakFlavorDays by remember { mutableStateOf(false) }

    // 根据烘焙度配置计算出的参考天数（只读显示）

    var suggestedRestDays by remember { mutableStateOf<Int?>(null) }

    var suggestedPeakFlavorDays by remember { mutableStateOf<Int?>(null) }



    val isEditing = beanId > 0

    val recResult by viewModel.recognitionResult.collectAsState(initial = null)

    val recProcessing by viewModel.recognitionProcessing.collectAsState(initial = false)



    // Gallery picker - keyword mode (for OCR)

    val keywordGalleryLauncher = rememberLauncherForActivityResult(

        ActivityResultContracts.GetContent()

    ) { uri: Uri? ->

        uri?.let {

            scope.launch {

                val bitmap = BitmapLoader.loadFromUri(context, it)

                if (bitmap != null) {

                    viewModel.runKeywordRecognition(bitmap)

                }

            }

        }

    }



    // Gallery picker for bean photos (album only, no camera)

    val photoGalleryLauncher = rememberLauncherForActivityResult(

        ActivityResultContracts.GetContent()

    ) { uri: Uri? ->

        uri?.let {

            if (localPhotoPaths.size < 6) {

                scope.launch {

                    val relativePath = ImageUtils.compressAndSaveBeanPhoto(context, it)

                    if (relativePath != null) {

                        localPhotoPaths = localPhotoPaths + relativePath

                    }

                }

            }

        }

    }



    val allImpressionTags by viewModel.allImpressionTags.collectAsState(initial = emptyList())

    var showPhotoOptions by remember { mutableStateOf(false) }



    // Apply recognition result with user feedback

    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recResult) {

        recResult?.let { result ->

            if (!result.success) {

                statusMessage = "${result.engineName}: ${result.rawResponse.take(80)}"

            } else {

                val filled = mutableListOf<String>()

                if (result.roaster.isNotEmpty()) { roaster = result.roaster; filled.add("烘焙商") }

                if (result.name.isNotEmpty()) { name = result.name; filled.add("豆名") }

                if (result.origin.isNotEmpty()) { origin = result.origin; filled.add("产地") }

                if (result.estate.isNotEmpty()) { estate = result.estate; filled.add("庄园") }

                if (result.variety.isNotEmpty()) { variety = result.variety; filled.add("品种") }

                if (result.process.isNotEmpty()) { /* 处理法改由下拉选，这里不做OCR填充 */ }

                if (result.roastLevel.isNotEmpty()) { /* 烘焙度改由下拉选，这里不做OCR填充 */ }

                if (result.roastDate.isNotEmpty()) { roastDate = result.roastDate; filled.add("烘焙日期") }

                result.flavors.forEach { f ->

                    if (!tags.contains(f)) { tags.add(f); filled.add("风味:$f") }

                }

                // 萃取建议字段

                result.dose?.let { d -> dose = d.toString(); filled.add("粉量") }

                if (result.brewRatio.isNotEmpty()) { brewRatio = result.brewRatio; filled.add("粉水比") }

                result.waterAmount?.let { w -> waterAmount = w.toString(); filled.add("注水量") }

                result.brewTime?.let { t -> brewTime = t.toString(); filled.add("萃取时长") }

                result.waterTemp?.let { temp -> waterTemp = temp.toString(); filled.add("水温") }

                statusMessage = if (filled.isEmpty()) {

                    "识别完成，但未提取到信息"

                } else {

                    "${result.engineName} 已填入: ${filled.joinToString("、")}"

                }

            }

        }

    }



    // Load existing bean

    LaunchedEffect(beanId) {

        if (isEditing) {

            viewModel.loadBean(beanId)

            viewModel.loadTags(beanId)

            viewModel.loadImpressionTags(beanId)

        }

    }



    val bean by viewModel.selectedBean.collectAsState(initial = null)

    val existingTags by viewModel.tags.collectAsState(initial = emptyList())

    val existingImpressionTags by viewModel.impressionTags.collectAsState(initial = emptyList())

    LaunchedEffect(existingImpressionTags) {

        if (isEditing && existingImpressionTags.isNotEmpty() && selectedImpressionTagIds.isEmpty()) {

            selectedImpressionTagIds.clear()

            selectedImpressionTagIds.addAll(existingImpressionTags.map { it.id })

        }

    }



    val roastDegrees by viewModel.allRoastDegrees.collectAsState(initial = emptyList())

    val processMethods by viewModel.allProcessMethods.collectAsState(initial = emptyList())



    // 烘焙度选中时：从配置表查参考天数并自动填入

    fun loadSuggestedDays(roastDegreeId: Long?) {

        if (roastDegreeId == null) {

            suggestedRestDays = null

            suggestedPeakFlavorDays = null

            // 清空覆盖值（仅当用户未手动修改时）

            if (!userModifiedRestDays) restDaysOverride = ""

            if (!userModifiedPeakFlavorDays) peakFlavorDaysOverride = ""

            return

        }

        scope.launch {

            val restConfig = viewModel.getRestPeriodConfigByRoastDegreeId(roastDegreeId)

            val peakConfig = viewModel.getPeakFlavorConfigByRoastDegreeId(roastDegreeId)

            val rest = restConfig?.restDays

            val peak = peakConfig?.peakFlavorDays

            suggestedRestDays = rest

            suggestedPeakFlavorDays = peak

            // 自动填入（仅当用户未手动修改时）

            if (!userModifiedRestDays) restDaysOverride = rest?.toString() ?: ""

            if (!userModifiedPeakFlavorDays) peakFlavorDaysOverride = peak?.toString() ?: ""

        }

    }



    LaunchedEffect(bean, existingTags, roastDegrees, processMethods) {

        if (isEditing && bean != null) {

            val b = bean!!

            roaster = b.roaster

            name = b.name

            origin = b.origin

            region = b.region

            estate = b.estate

            variety = b.variety

            originalGrindSize = b.grindSize

            roastDate = b.roastDate?.let { DateUtils.formatDate(it) } ?: ""

            notes = b.notes

            imageUri = b.imageUri

            localPhotoPaths = b.localPhotoPaths

            // 萃取建议

            dose = b.dose?.toString() ?: ""

            brewRatio = b.brewRatio ?: ""

            waterAmount = b.waterAmount?.toString() ?: ""

            pouringDurationSeconds = b.pouringDurationSeconds?.toString() ?: ""

            brewTime = b.brewTime?.toString() ?: ""

            waterTemp = b.waterTemp?.toString() ?: ""

            // 养豆期/赏味期手动覆盖

            restDaysOverride = b.restDays?.toString() ?: ""

            peakFlavorDaysOverride = b.peakFlavorDays?.toString() ?: ""

            userModifiedRestDays = b.restDays != null  // 有保存值则标记为已修改，防止 loadSuggestedDays 覆盖

            userModifiedPeakFlavorDays = b.peakFlavorDays != null

            // roastLevel字符串匹配到id（兼容旧数据）

            if (roastDegrees.isNotEmpty()) {

                val matchedRoast = roastDegrees.find { it.name == b.roastLevel }

                if (matchedRoast != null) {

                    selectedRoastDegreeId = matchedRoast.id

                    if (selectedRoastDegreeId != null) loadSuggestedDays(selectedRoastDegreeId)

                } else if (b.roastLevel.isNotEmpty()) {

                    customRoastLevel = b.roastLevel

                }

            }

            // process字符串匹配到id

            if (processMethods.isNotEmpty()) {

                val matchedProcess = processMethods.find { it.name == b.process }

                if (matchedProcess != null) {

                    selectedProcessMethodId = matchedProcess.id

                } else if (b.process.isNotEmpty()) {

                    customProcessName = b.process

                }

            }

            if (tags.isEmpty() && existingTags.isNotEmpty()) {

                tags.clear()

                tags.addAll(existingTags)

            }

        }

    }



    Scaffold(

        topBar = {

            TopAppBar(

                title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(if (isEditing) "编辑豆子" else "添加豆子") } },

                colors = TopAppBarDefaults.topAppBarColors(

                    containerColor = MaterialTheme.colorScheme.primary,

                    titleContentColor = MaterialTheme.colorScheme.onPrimary

                )

            )

        }

    ) { padding ->

        Column(

            modifier = Modifier

                .fillMaxSize()

                .padding(padding)

                .imePadding()

                .padding(16.dp)

                .verticalScroll(rememberScrollState()),

            verticalArrangement = Arrangement.spacedBy(12.dp)

        ) {

            // Recognition Section

            Surface(modifier = Modifier.fillMaxWidth()) {

                Column(modifier = Modifier.padding(16.dp)) {

                    Text("图片识别", style = MaterialTheme.typography.titleMedium,

                        modifier = Modifier.padding(bottom = 8.dp))

                    OutlinedButton(

                        onClick = {

                            keywordGalleryLauncher.launch("image/*")

                        },

                        modifier = Modifier.fillMaxWidth()

                    ) {

                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("从相册选择图片识别")

                    }

                    if (recProcessing) {

                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

                        Text("正在识别...", style = MaterialTheme.typography.bodySmall)

                    }

                    statusMessage?.let { msg ->

                        val isError = msg.contains("识别失败") || msg.contains("出错")

                        Text(msg, style = MaterialTheme.typography.bodySmall,

                            color = if (isError) MaterialTheme.colorScheme.error

                            else MaterialTheme.colorScheme.primary,

                            modifier = Modifier.padding(top = 4.dp))

                    }

                }

            }



            // Form Fields

            OutlinedTextField(value = roaster, onValueChange = { roaster = it },

                label = { Text("烘焙商") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = name, onValueChange = { name = it },

                label = { Text("豆名 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            // Bean Image preview

            if (imageUri.isNotEmpty()) {

                AsyncImage(

                    model = File(imageUri),

                    contentDescription = "豆袋照片",

                    modifier = Modifier.fillMaxWidth().height(150.dp),

                    contentScale = ContentScale.Crop

                )

            }



            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {

                OutlinedTextField(value = origin, onValueChange = { origin = it },

                    label = { Text("产地") }, modifier = Modifier.weight(1f), singleLine = true)

                OutlinedTextField(value = region, onValueChange = { region = it },

                    label = { Text("产区") }, modifier = Modifier.weight(1f), singleLine = true)

            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {

                OutlinedTextField(value = variety, onValueChange = { variety = it },

                    label = { Text("品种") }, modifier = Modifier.weight(1f), singleLine = true)

                OutlinedTextField(value = estate, onValueChange = { estate = it },

                    label = { Text("庄园") }, modifier = Modifier.weight(1f), singleLine = true)

            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {

                // 处理法（可自定义）

                ExposedDropdownMenuBox(

                    expanded = processDropdownExpanded,

                    onExpandedChange = { processDropdownExpanded = it },

                    modifier = Modifier.weight(1f)

                ) {

                    OutlinedTextField(

                        value = customProcessName.ifEmpty { processMethods.find { it.id == selectedProcessMethodId }?.name ?: "" },

                        onValueChange = {

                            customProcessName = it

                            selectedProcessMethodId = null

                        },

                        label = { Text("处理法") },

                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = processDropdownExpanded) },

                        modifier = Modifier.menuAnchor().fillMaxWidth(),

                        singleLine = true,

                        placeholder = { Text("输入或选择") }

                    )

                    ExposedDropdownMenu(

                        expanded = processDropdownExpanded,

                        onDismissRequest = { processDropdownExpanded = false },

                        modifier = Modifier.heightIn(max = 250.dp)

                    ) {

                        DropdownMenuItem(

                            text = { Text("不选择") },

                            onClick = {

                                selectedProcessMethodId = null

                                customProcessName = ""

                                processDropdownExpanded = false

                            }

                        )

                        processMethods.forEach { method ->

                            DropdownMenuItem(

                                text = { Text(method.name) },

                                onClick = {

                                    selectedProcessMethodId = method.id

                                    customProcessName = ""

                                    processDropdownExpanded = false

                                }

                            )

                        }

                    }

                }

                // 烘焙度（可自定义）

                ExposedDropdownMenuBox(

                    expanded = roastLevelDropdownExpanded,

                    onExpandedChange = { roastLevelDropdownExpanded = it },

                    modifier = Modifier.weight(1f)

                ) {

                    OutlinedTextField(

                        value = customRoastLevel.ifEmpty { roastDegrees.find { it.id == selectedRoastDegreeId }?.name ?: "" },

                        onValueChange = {

                            customRoastLevel = it

                            selectedRoastDegreeId = null

                            suggestedRestDays = null

                            suggestedPeakFlavorDays = null

                        },

                        label = { Text("烘焙度") },

                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roastLevelDropdownExpanded) },

                        modifier = Modifier.menuAnchor().fillMaxWidth(),

                        singleLine = true,

                        placeholder = { Text("输入或选择") }

                    )

                    ExposedDropdownMenu(

                        expanded = roastLevelDropdownExpanded,

                        onDismissRequest = { roastLevelDropdownExpanded = false },

                        modifier = Modifier.heightIn(max = 250.dp)

                    ) {

                        DropdownMenuItem(

                            text = { Text("不选择") },

                            onClick = {

                                selectedRoastDegreeId = null

                                customRoastLevel = ""

                                suggestedRestDays = null

                                suggestedPeakFlavorDays = null

                                roastLevelDropdownExpanded = false

                            }

                        )

                        roastDegrees.forEach { degree ->

                            DropdownMenuItem(

                                text = { Text(degree.name) },

                                onClick = {

                                    selectedRoastDegreeId = degree.id

                                    customRoastLevel = ""

                                    loadSuggestedDays(degree.id)

                                    roastLevelDropdownExpanded = false

                                }

                            )

                        }

                    }

                }

            }

            OutlinedTextField(value = roastDate, onValueChange = { roastDate = it },

                label = { Text("烘焙日期") }, modifier = Modifier.fillMaxWidth(), singleLine = true,

                placeholder = { Text("格式：2026/05/10") })

            // 养豆期/赏味期（基于烘焙度配置自动计算，可手动覆盖）

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {

                // 养豆天数

                OutlinedTextField(

                    value = restDaysOverride,

                    onValueChange = {

                        restDaysOverride = it.filter { c -> c.isDigit() }

                        userModifiedRestDays = true

                    },

                    label = { Text("养豆天数") },

                    modifier = Modifier.weight(1f),

                    singleLine = true,

                    placeholder = {

                        Text(suggestedRestDays?.let { "参考 $it 天" } ?: "")

                    }

                )

                // 赏味天数

                OutlinedTextField(

                    value = peakFlavorDaysOverride,

                    onValueChange = {

                        peakFlavorDaysOverride = it.filter { c -> c.isDigit() }

                        userModifiedPeakFlavorDays = true

                    },

                    label = { Text("赏味天数") },

                    modifier = Modifier.weight(1f),

                    singleLine = true,

                    placeholder = {

                        Text(suggestedPeakFlavorDays?.let { "参考 $it 天" } ?: "")

                    }

                )

            }

            // 提示文字

            if (suggestedRestDays != null || suggestedPeakFlavorDays != null) {

                Text(

                    text = "提示：选择烘焙度后自动带出参考天数，可手动修改覆盖",

                    style = MaterialTheme.typography.bodySmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant

                )

            }



            // Impression Tags

            Text("印象标签", style = MaterialTheme.typography.titleMedium)

            if (allImpressionTags.isEmpty()) {

                Text("暂无印象标签，可在设置中添加", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            } else {

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {

                    allImpressionTags.forEach { tag ->

                        val isSelected = selectedImpressionTagIds.contains(tag.id)

                        FilterChip(

                            selected = isSelected,

                            onClick = {

                                if (isSelected) {

                                    selectedImpressionTagIds.remove(tag.id)

                                } else if (selectedImpressionTagIds.size < 5) {

                                    selectedImpressionTagIds.add(tag.id)

                                }

                            },

                            label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },

                            colors = FilterChipDefaults.filterChipColors(

                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,

                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer

                            )

                        )

                    }

                    // Quick-add button

                    if (selectedImpressionTagIds.size < 5) {

                        IconButton(

                            onClick = { quickAddName = ""; showQuickAddDialog = true },

                            modifier = Modifier.size(32.dp)

                        ) {

                            Icon(Icons.Default.Add, "新增标签", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))

                        }

                    }

                }

            }



            Spacer(modifier = Modifier.height(4.dp))



            // Flavor Tags

            Text("风味标签", style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                OutlinedTextField(value = tagInput, onValueChange = { tagInput = it },

                    label = { Text("输入风味词") }, modifier = Modifier.weight(1f), singleLine = true)

                IconButton(onClick = {

                    if (tagInput.isNotBlank() && !tags.contains(tagInput.trim())) {

                        tags.add(tagInput.trim()); tagInput = ""

                    }

                }, modifier = Modifier.size(48.dp)) {

                    Icon(Icons.Default.Add, contentDescription = "添加标签")

                }

            }

            if (tags.isNotEmpty()) {

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {

                    tags.forEach { tag ->

                        InputChip(selected = false, onClick = { tags.remove(tag) },

                            label = { Text(tag) }, trailingIcon = { Icon(Icons.Default.Close, "删除") })

                    }

                }

            }





            OutlinedTextField(value = notes, onValueChange = { notes = it },

                label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            // 豆子照片区块（在官方萃取建议上方）

            if (localPhotoPaths.isNotEmpty() || localPhotoPaths.size < 6) {

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("豆子照片", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(4.dp))

                // 已有照片 Grid

                if (localPhotoPaths.isNotEmpty()) {

                    androidx.compose.foundation.layout.Column(

                        verticalArrangement = Arrangement.spacedBy(8.dp)

                    ) {

                        val rows = localPhotoPaths.chunked(3)

                        rows.forEach { rowPhotos ->

                            Row(

                                horizontalArrangement = Arrangement.spacedBy(8.dp),

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                rowPhotos.forEach { photoPath ->

                                    Box(modifier = Modifier.weight(1f)) {

                                        AsyncImage(

                                            model = File(ImageUtils.getBeanPhotoFile(context, photoPath).absolutePath),

                                            contentDescription = "豆子照片",

                                            modifier = Modifier

                                                .aspectRatio(1f)

                                                .fillMaxWidth(),

                                            contentScale = ContentScale.Crop

                                        )

                                        IconButton(

                                            onClick = {

                                                localPhotoPaths = localPhotoPaths - photoPath

                                                ImageUtils.deleteBeanPhoto(photoPath, context)

                                            },

                                            modifier = Modifier.align(Alignment.TopEnd)

                                        ) {

                                            Surface(

                                                shape = MaterialTheme.shapes.small,

                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)

                                            ) {

                                                Icon(

                                                    Icons.Default.Close,

                                                    contentDescription = "删除",

                                                    modifier = Modifier.padding(2.dp),

                                                    tint = MaterialTheme.colorScheme.error

                                                )

                                            }

                                        }

                                    }

                                }

                                repeat(3 - rowPhotos.size) {

                                    Spacer(modifier = Modifier.weight(1f))

                                }

                            }

                        }

                    }

                    Spacer(modifier = Modifier.height(8.dp))

                }

                // 添加照片按钮

                if (localPhotoPaths.size < 6) {

                    OutlinedButton(

                        onClick = { showPhotoOptions = true },

                        modifier = Modifier.fillMaxWidth()

                    ) {

                        Icon(Icons.Default.AddAPhoto, contentDescription = null)

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("添加照片（${localPhotoPaths.size}/6）")

                    }

                }

            }



            // 官方萃取建议

            Text("官方萃取建议", style = MaterialTheme.typography.titleMedium)

            // dose + waterAmount（调换后的位置）

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {

                OutlinedTextField(value = dose, onValueChange = { dose = it },

                    label = { Text("粉量(g)") }, modifier = Modifier.weight(1f), singleLine = true)

                OutlinedTextField(value = waterAmount, onValueChange = { waterAmount = it },

                    label = { Text("注水量(ml)") }, modifier = Modifier.weight(1f), singleLine = true)

            }

            // 自动计算粉水比（输入粉量+注水量后显示）

            val autoDose = dose.toFloatOrNull() ?: 0f

            val autoWater = waterAmount.toFloatOrNull() ?: 0f

            val derivedRatio = if (autoDose > 0 && autoWater > 0) String.format("%.1f", autoWater / autoDose) else ""

            if (derivedRatio.isNotEmpty()) {

                Row(

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.spacedBy(8.dp),

                    modifier = Modifier.padding(top = 4.dp)

                ) {

                    Text(

                        text = "自动计算粉水比 1:${derivedRatio}",

                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.secondary,

                        modifier = Modifier.weight(1f)

                    )

                    TextButton(

                        onClick = { brewRatio = derivedRatio }

                    ) {

                        Text("应用到粉水比")

                    }

                }

            }

            // brewRatio + waterTemp（换位置）

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {

                OutlinedTextField(value = brewRatio, onValueChange = { brewRatio = it },

                    label = { Text("粉水比") }, modifier = Modifier.weight(1f), singleLine = true,

                    placeholder = { Text("如 1:15") })

                OutlinedTextField(value = waterTemp, onValueChange = { waterTemp = it },

                    label = { Text("水温(°C)") }, modifier = Modifier.weight(1f), singleLine = true,

                    placeholder = { Text("可留空") })

            }

            // 注水时长 + 萃取时长

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {

                OutlinedTextField(value = pouringDurationSeconds, onValueChange = { pouringDurationSeconds = it },

                    label = { Text("注水时长(s)") }, modifier = Modifier.weight(1f), singleLine = true)

                OutlinedTextField(value = brewTime, onValueChange = { brewTime = it },

                    label = { Text("萃取时长(s)") }, modifier = Modifier.weight(1f), singleLine = true)

            }



            Button(onClick = {

                scope.launch {

                    val savedGrindSize = if (isEditing) originalGrindSize else ""

                    val existingCreatedAt = if (isEditing) (bean?.createdAt ?: System.currentTimeMillis()) else System.currentTimeMillis()

                    val beanToSave = CoffeeBean(

                        id = if (isEditing) beanId else 0,

                        roaster = roaster, name = name, origin = origin, region = region, estate = estate,

                        variety = variety,

                        process = customProcessName.ifEmpty { selectedProcessMethodId?.let { processMethods.find { m -> m.id == it }?.name } ?: "" },

                        roastLevel = customRoastLevel.ifEmpty { selectedRoastDegreeId?.let { roastDegrees.find { d -> d.id == it }?.name } ?: "" },

                        grindSize = savedGrindSize,

                        roastDate = DateUtils.parseDate(roastDate),

                        notes = notes, imageUri = imageUri,

                        localPhotoPaths = localPhotoPaths,

                        isFavorite = bean?.isFavorite ?: false,

                        isArchived = bean?.isArchived ?: false,

                        createdAt = existingCreatedAt,

                        updatedAt = System.currentTimeMillis(),

                        dose = dose.toFloatOrNull(),

                        brewRatio = brewRatio.ifBlank { null },

                        waterAmount = waterAmount.toFloatOrNull(),

                        brewTime = brewTime.toIntOrNull(),

                        waterTemp = waterTemp.toIntOrNull(),

                        pouringDurationSeconds = pouringDurationSeconds.toIntOrNull(),

                        restDays = restDaysOverride.toIntOrNull(),

                        peakFlavorDays = peakFlavorDaysOverride.toIntOrNull()

                    )

                    if (isEditing) viewModel.updateBeanSync(beanToSave, tags.toList(), selectedImpressionTagIds.toList())

                    else viewModel.saveBeanSync(beanToSave, tags.toList(), selectedImpressionTagIds.toList())

                    navController.popBackStack()

                }

            }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("保存") }



            // Photo Options Bottom Sheet

            if (showPhotoOptions) {

                ModalBottomSheet(

                    onDismissRequest = { showPhotoOptions = false }

                ) {

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(24.dp),

                        verticalArrangement = Arrangement.spacedBy(16.dp)

                    ) {

                        Text("添加豆子照片（最多6张）", style = MaterialTheme.typography.titleMedium)

                        Button(

                            onClick = {

                                showPhotoOptions = false

                                photoGalleryLauncher.launch("image/*")

                            },

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)

                            Spacer(modifier = Modifier.width(8.dp))

                            Text("从相册选择")

                        }

                        Spacer(modifier = Modifier.height(16.dp))

                    }

                }

            }

        }

    }



    // Quick-add impression tag dialog

    if (showQuickAddDialog) {

        AlertDialog(

            onDismissRequest = { showQuickAddDialog = false },

            title = { Text("添加印象标签") },

            text = {

                OutlinedTextField(

                    value = quickAddName,

                    onValueChange = { quickAddName = it },

                    label = { Text("标签名称") },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )

            },

            confirmButton = {

                TextButton(onClick = {

                    if (quickAddName.isNotBlank()) {

                        scope.launch {

                            val newId = viewModel.addImpressionTag(quickAddName.trim())

                            selectedImpressionTagIds.add(newId)

                        }

                        showQuickAddDialog = false

                    }

                }, enabled = quickAddName.isNotBlank()) {

                    Text("添加并选中")

                }

            },

            dismissButton = {

                TextButton(onClick = { showQuickAddDialog = false }) { Text("取消") }

            }

        )

    }

}
