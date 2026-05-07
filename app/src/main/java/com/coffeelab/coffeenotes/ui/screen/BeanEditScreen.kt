package com.coffeelab.coffeenotes.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.util.BitmapLoader
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

    var roaster by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var estate by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("") }
    var process by remember { mutableStateOf("") }
    var roastLevel by remember { mutableStateOf("") }
    var roastDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() }
    var originalGrindSize by remember { mutableStateOf("") }
    // 官方萃取建议
    var dose by remember { mutableStateOf("") }
    var brewRatio by remember { mutableStateOf("") }
    var waterAmount by remember { mutableStateOf("") }
    var brewTime by remember { mutableStateOf("") }
    var waterTemp by remember { mutableStateOf("") }

    val isEditing = beanId > 0
    val recResult by viewModel.recognitionResult.collectAsState(initial = null)
    val recProcessing by viewModel.recognitionProcessing.collectAsState(initial = false)

    // Gallery picker - keyword mode
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

    // Gallery picker - AI mode
    val aiGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val bitmap = BitmapLoader.loadFromUri(context, it)
                if (bitmap != null) {
                    viewModel.runAiRecognition(bitmap)
                }
            }
        }
    }

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
                if (result.process.isNotEmpty()) { process = result.process; filled.add("处理法") }
                if (result.roastLevel.isNotEmpty()) { roastLevel = result.roastLevel; filled.add("烘焙度") }
                if (result.roastDate.isNotEmpty()) { roastDate = result.roastDate; filled.add("烘焙日期") }
                result.flavors.forEach { f ->
                    if (!tags.contains(f)) { tags.add(f); filled.add("风味:$f") }
                }
                // 萃取建议字段
                result.dose?.let { d -> dose = d.toString(); filled.add("粉量") }
                if (result.brewRatio.isNotEmpty()) { brewRatio = result.brewRatio; filled.add("粉水比") }
                result.waterAmount?.let { w -> waterAmount = w.toString(); filled.add("注水量") }
                result.brewTime?.let { t -> brewTime = t.toString(); filled.add("萃取时间") }
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
        }
    }

    val bean by viewModel.selectedBean.collectAsState(initial = null)
    val existingTags by viewModel.tags.collectAsState(initial = emptyList())
    LaunchedEffect(bean, existingTags) {
        if (isEditing && bean != null) {
            val b = bean!!
            roaster = b.roaster
            name = b.name
            origin = b.origin
            region = b.region
            estate = b.estate
            variety = b.variety
            process = b.process
            roastLevel = b.roastLevel
            originalGrindSize = b.grindSize
            roastDate = if (b.roastDate != null) b.roastDate.toString() else ""
            notes = b.notes
            imageUri = b.imageUri
            // 萃取建议
            dose = b.dose?.toString() ?: ""
            brewRatio = b.brewRatio ?: ""
            waterAmount = b.waterAmount?.toString() ?: ""
            brewTime = b.brewTime?.toString() ?: ""
            waterTemp = b.waterTemp?.toString() ?: ""
            if (tags.isEmpty() && existingTags.isNotEmpty()) {
                tags.clear()
                tags.addAll(existingTags)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑豆子" else "添加豆子") },
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
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recognition Section
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("拍照识别", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                navController.navigate(Screen.Camera.createRoute(beanId, "keyword"))
                            }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("拍照", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(onClick = {
                                keywordGalleryLauncher.launch("image/*")
                            }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("相册", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                navController.navigate(Screen.Camera.createRoute(beanId, "ai"))
                            }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI 拍照", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(onClick = {
                                aiGalleryLauncher.launch("image/*")
                            }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI 相册", style = MaterialTheme.typography.labelMedium)
                            }
                        }
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

            // Bean Image preview
            if (imageUri.isNotEmpty()) {
                AsyncImage(
                    model = File(imageUri),
                    contentDescription = "豆袋照片",
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Form Fields
            OutlinedTextField(value = roaster, onValueChange = { roaster = it },
                label = { Text("烘焙商") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("豆名 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
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
                OutlinedTextField(value = process, onValueChange = { process = it },
                    label = { Text("处理法") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = roastLevel, onValueChange = { roastLevel = it },
                    label = { Text("烘焙度") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            OutlinedTextField(value = roastDate, onValueChange = { roastDate = it },
                label = { Text("烘焙日期") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

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
            // brewRatio + brewTime（调换后的位置）
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = brewRatio, onValueChange = { brewRatio = it },
                    label = { Text("粉水比") }, modifier = Modifier.weight(1f), singleLine = true,
                    placeholder = { Text("如 1:15") })
                OutlinedTextField(value = brewTime, onValueChange = { brewTime = it },
                    label = { Text("萃取时间(s)") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            OutlinedTextField(value = waterTemp, onValueChange = { waterTemp = it },
                label = { Text("水温(°C)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("可留空") })

            Button(onClick = {
                scope.launch {
                    val savedGrindSize = if (isEditing) originalGrindSize else ""
                    val existingCreatedAt = if (isEditing) (bean?.createdAt ?: System.currentTimeMillis()) else System.currentTimeMillis()
                    val beanToSave = CoffeeBean(
                        id = if (isEditing) beanId else 0,
                        roaster = roaster, name = name, origin = origin, region = region, estate = estate,
                        variety = variety, process = process, roastLevel = roastLevel,
                        grindSize = savedGrindSize,
                        roastDate = roastDate.toLongOrNull(), notes = notes, imageUri = imageUri,
                        createdAt = existingCreatedAt,
                        updatedAt = System.currentTimeMillis(),
                        dose = dose.toFloatOrNull(),
                        brewRatio = brewRatio.ifBlank { null },
                        waterAmount = waterAmount.toFloatOrNull(),
                        brewTime = brewTime.toIntOrNull(),
                        waterTemp = waterTemp.toIntOrNull()
                    )
                    if (isEditing) viewModel.updateBeanSync(beanToSave, tags.toList())
                    else viewModel.saveBeanSync(beanToSave, tags.toList())
                    navController.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("保存") }
        }
    }
}
