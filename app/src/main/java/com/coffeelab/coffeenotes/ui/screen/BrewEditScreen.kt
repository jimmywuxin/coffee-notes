package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.BrewRecipe
import com.coffeelab.coffeenotes.data.entity.Grinder
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import com.coffeelab.coffeenotes.data.entity.Equipment
import com.coffeelab.coffeenotes.viewmodel.RecipeViewModel
import com.coffeelab.coffeenotes.viewmodel.EquipmentViewModel
import com.coffeelab.coffeenotes.viewmodel.GrinderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrewEditScreen(
    navController: NavController,
    recordId: Long,
    beanId: Long,
    brewViewModel: BrewViewModel = viewModel(),
    beanViewModel: BeanViewModel = viewModel(),
    recipeViewModel: RecipeViewModel = viewModel(),
    equipmentViewModel: EquipmentViewModel = viewModel(),
    grinderViewModel: GrinderViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val beans by beanViewModel.allBeans.collectAsState(initial = emptyList())
    val recipes by recipeViewModel.allRecipes.collectAsState(initial = emptyList())

    val isEditing = recordId > 0
    var showDeleteDialog by remember { mutableStateOf(false) }

    // State
    var selectedBeanId by remember { mutableStateOf(beanId) }
    var selectedRecipeId by remember { mutableStateOf(-1L) }
    var equipment by remember { mutableStateOf("") }
    var coffeeWeight by remember { mutableStateOf("") }
    var coffeeWaterRatio by remember { mutableStateOf("") }
    var waterAmount by remember { mutableStateOf("") }
    var waterTemp by remember { mutableStateOf("") }
    var grinder by remember { mutableStateOf("") }
    var grindSize by remember { mutableStateOf("") }
    var extractionTime by remember { mutableStateOf("") }
    var flavorNotes by remember { mutableStateOf("") }
    var showCustomRatio by remember { mutableStateOf(false) }

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

    // Equipment list
    val equipmentList by equipmentViewModel.allEquipment.collectAsState(initial = emptyList())
    val equipmentItems = if (equipmentList.isNotEmpty()) {
        equipmentList.map { it.name }
    } else {
        Equipment.DEFAULT_EQUIPMENT
    }

    // Grinder list
    val grinderList by grinderViewModel.allGrinders.collectAsState(initial = emptyList())
    val grinderItems = if (grinderList.isNotEmpty()) {
        grinderList.map { it.name }
    } else {
        Grinder.DEFAULT_GRINDERS
    }

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
                selectedRecipeId = r.recipeId ?: -1L
                equipment = r.equipment
                coffeeWeight = if (r.coffeeWeight > 0) r.coffeeWeight.toString() else ""
                coffeeWaterRatio = if (r.coffeeWaterRatio > 0) {
                    val s = r.coffeeWaterRatio.toString()
                    if (s.endsWith(".0")) s.dropLast(2) else s
                } else ""
                waterAmount = if (r.waterAmount > 0) r.waterAmount.toString() else ""
                waterTemp = if (r.waterTemp > 0) r.waterTemp.toString() else ""
                grinder = r.grinder
                grindSize = r.grindSize
                extractionTime = if (r.extractionTime > 0) r.extractionTime.toString() else ""
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
            }
        }
    }

    // Apply recipe to fill parameters
    fun applyRecipe(recipe: BrewRecipe) {
        equipment = recipe.equipment
        coffeeWeight = if (recipe.coffeeWeight > 0) recipe.coffeeWeight.toString() else ""
        val ratioStr = if (recipe.coffeeWaterRatio > 0) recipe.coffeeWaterRatio.toString() else ""
        coffeeWaterRatio = if (ratioStr.endsWith(".0")) ratioStr.dropLast(2) else ratioStr
        showCustomRatio = false
        waterTemp = if (recipe.waterTemp > 0) recipe.waterTemp.toString() else ""
        grinder = recipe.grinder
        grindSize = recipe.grindSize
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑冲煮记录" else "新增冲煮记录") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    onExpandedChange = { beanExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedBeanName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("咖啡豆") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = beanExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = beanExpanded,
                        onDismissRequest = { beanExpanded = false }
                    ) {
                        beans.forEach { bean ->
                            DropdownMenuItem(
                                text = { Text("${bean.roaster} - ${bean.name}") },
                                onClick = {
                                    selectedBeanId = bean.id
                                    beanExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Select Recipe
            Text("选择配方（可选）", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filteredRecipes = recipes
                if (filteredRecipes.isEmpty()) {
                    Text("暂无配方", style = MaterialTheme.typography.bodySmall)
                } else {
                    filteredRecipes.forEach { recipe ->
                        SuggestionChip(
                            onClick = { applyRecipe(recipe) },
                            label = { Text(recipe.name) }
                        )
                    }
                }
            }

            // Equipment
            Text("器具", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                equipmentItems.forEach { item ->
                    FilterChip(
                        selected = equipment == item,
                        onClick = { equipment = item },
                        label = { Text(item) }
                    )
                }
            }

            // Brew Parameters
            Text("冲煮参数", style = MaterialTheme.typography.titleMedium)
            // ratioDisplay 存分母数值字符串（如 "15"、"4.5"），显示时加 "1:" 前缀
            val ratioOptions = listOf("15", "16", "17", "2")
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
                    value = waterTemp,
                    onValueChange = { waterTemp = it },
                    label = { Text("水温 (℃)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = extractionTime,
                    onValueChange = { extractionTime = it },
                    label = { Text("萃取时长 (秒)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Grinder + Grind Size
            Text("研磨", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Grinder dropdown
                var grinderExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = grinderExpanded,
                    onExpandedChange = { grinderExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = grinder,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("磨豆机") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = grinderExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = grinderExpanded,
                        onDismissRequest = { grinderExpanded = false }
                    ) {
                        grinderItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    grinder = item
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

            // Tasting Scores
            Text("品鉴评分", style = MaterialTheme.typography.titleMedium)
            StarRatingRow(label = "酸度", rating = acidity, onRatingChange = { acidity = it })
            StarRatingRow(label = "甜感", rating = sweetness, onRatingChange = { sweetness = it })
            StarRatingRow(label = "苦味", rating = bitterness, onRatingChange = { bitterness = it })
            StarRatingRow(label = "口感", rating = mouthfeel, onRatingChange = { mouthfeel = it })
            StarRatingRow(label = "回甘", rating = aftertaste, onRatingChange = { aftertaste = it })
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            StarRatingRow(label = "总评 ⭐", rating = overall, onRatingChange = { overall = it }, large = true)

            // Flavor Notes
            OutlinedTextField(
                value = flavorNotes,
                onValueChange = { flavorNotes = it },
                label = { Text("风味笔记") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Save
            Button(
                onClick = {
                    scope.launch {
                        val record = BrewRecord(
                            id = if (isEditing) recordId else 0,
                            beanId = selectedBeanId,
                            recipeId = if (selectedRecipeId > 0) selectedRecipeId else null,
                            dateTime = if (isEditing) (brewViewModel.getRecord(recordId)?.dateTime
                                ?: System.currentTimeMillis()) else System.currentTimeMillis(),
                            equipment = equipment,
                            coffeeWeight = coffeeWeight.toDoubleOrNull() ?: 0.0,
                            coffeeWaterRatio = coffeeWaterRatio.toDoubleOrNull() ?: 0.0,
                            waterAmount = waterAmount.toDoubleOrNull() ?: 0.0,
                            waterTemp = waterTemp.toDoubleOrNull() ?: 0.0,
                            grinder = grinder,
                            grindSize = grindSize,
                            extractionTime = extractionTime.toIntOrNull() ?: 0,
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
}

@Composable
fun StarRatingRow(
    label: String,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    large: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp)
        )
        for (i in 1..5) {
            IconButton(
                onClick = { onRatingChange(if (rating == i) 0 else i) },
                modifier = Modifier.size(if (large) 40.dp else 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "星 $i",
                    tint = if (i <= rating) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(if (large) 32.dp else 24.dp)
                )
            }
        }
    }
}
