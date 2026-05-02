package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.BrewRecipe
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import com.coffeelab.coffeenotes.data.entity.Equipment
import com.coffeelab.coffeenotes.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrewEditScreen(
    navController: NavController,
    recordId: Long,
    beanId: Long,
    brewViewModel: BrewViewModel = viewModel(),
    beanViewModel: BeanViewModel = viewModel(),
    recipeViewModel: RecipeViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val beans by beanViewModel.allBeans.collectAsState(initial = emptyList())
    val recipes by recipeViewModel.allRecipes.collectAsState(initial = emptyList())

    val isEditing = recordId > 0

    // State
    var selectedBeanId by remember { mutableStateOf(beanId) }
    var selectedRecipeId by remember { mutableStateOf(-1L) }
    var equipment by remember { mutableStateOf("") }
    var coffeeWeight by remember { mutableStateOf("") }
    var waterWeight by remember { mutableStateOf("") }
    var waterTemp by remember { mutableStateOf("") }
    var grindSize by remember { mutableStateOf("") }
    var extractionTime by remember { mutableStateOf("") }
    var bloomTime by remember { mutableStateOf("") }
    var pourCount by remember { mutableStateOf("") }
    var totalTime by remember { mutableStateOf("") }
    var flavorNotes by remember { mutableStateOf("") }

    // Rating states (1-5, 0 = not rated)
    var acidity by remember { mutableIntStateOf(0) }
    var sweetness by remember { mutableIntStateOf(0) }
    var bitterness by remember { mutableIntStateOf(0) }
    var mouthfeel by remember { mutableIntStateOf(0) }
    var aftertaste by remember { mutableIntStateOf(0) }
    var overall by remember { mutableIntStateOf(0) }

    // Equipment list
    val equipmentItems = Equipment.DEFAULT_EQUIPMENT

    // Load existing record
    LaunchedEffect(recordId) {
        if (isEditing) {
            val record = brewViewModel.getRecord(recordId)
            record?.let { r ->
                selectedBeanId = r.beanId
                selectedRecipeId = r.recipeId ?: -1L
                equipment = r.equipment
                coffeeWeight = if (r.coffeeWeight > 0) r.coffeeWeight.toString() else ""
                waterWeight = if (r.waterWeight > 0) r.waterWeight.toString() else ""
                waterTemp = if (r.waterTemp > 0) r.waterTemp.toString() else ""
                grindSize = r.grindSize
                extractionTime = if (r.extractionTime > 0) r.extractionTime.toString() else ""
                bloomTime = if (r.bloomTime > 0) r.bloomTime.toString() else ""
                pourCount = if (r.pourCount > 0) r.pourCount.toString() else ""
                totalTime = if (r.totalTime > 0) r.totalTime.toString() else ""
                flavorNotes = r.flavorNotes
                acidity = r.acidity
                sweetness = r.sweetness
                bitterness = r.bitterness
                mouthfeel = r.mouthfeel
                aftertaste = r.aftertaste
                overall = r.overallRating
            }
        }
    }

    // Apply recipe to fill parameters
    fun applyRecipe(recipe: BrewRecipe) {
        equipment = recipe.equipment
        coffeeWeight = if (recipe.coffeeWeight > 0) recipe.coffeeWeight.toString() else ""
        waterWeight = if (recipe.waterWeight > 0) recipe.waterWeight.toString() else ""
        waterTemp = if (recipe.waterTemp > 0) recipe.waterTemp.toString() else ""
        grindSize = recipe.grindSize
        bloomTime = if (recipe.bloomTime > 0) recipe.bloomTime.toString() else ""
        pourCount = if (recipe.pourCount > 0) recipe.pourCount.toString() else ""
        totalTime = if (recipe.totalTime > 0) recipe.totalTime.toString() else ""
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = coffeeWeight,
                    onValueChange = { coffeeWeight = it },
                    label = { Text("粉量 (g)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = waterWeight,
                    onValueChange = { waterWeight = it },
                    label = { Text("水量 (ml)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = waterTemp,
                    onValueChange = { waterTemp = it },
                    label = { Text("水温 (℃)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = grindSize,
                    onValueChange = { grindSize = it },
                    label = { Text("研磨度") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = extractionTime,
                    onValueChange = { extractionTime = it },
                    label = { Text("萃取时长 (秒)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bloomTime,
                    onValueChange = { bloomTime = it },
                    label = { Text("焖蒸时间 (秒)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pourCount,
                    onValueChange = { pourCount = it },
                    label = { Text("注水次数") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = totalTime,
                    onValueChange = { totalTime = it },
                    label = { Text("总时长 (秒)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

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
                            waterWeight = waterWeight.toDoubleOrNull() ?: 0.0,
                            waterTemp = waterTemp.toDoubleOrNull() ?: 0.0,
                            grindSize = grindSize,
                            extractionTime = extractionTime.toIntOrNull() ?: 0,
                            bloomTime = bloomTime.toIntOrNull() ?: 0,
                            pourCount = pourCount.toIntOrNull() ?: 0,
                            totalTime = totalTime.toIntOrNull() ?: 0,
                            acidity = acidity,
                            sweetness = sweetness,
                            bitterness = bitterness,
                            mouthfeel = mouthfeel,
                            aftertaste = aftertaste,
                            overallRating = overall,
                            flavorNotes = flavorNotes
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
