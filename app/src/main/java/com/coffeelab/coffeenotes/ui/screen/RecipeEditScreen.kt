package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.BrewRecipe
import com.coffeelab.coffeenotes.data.entity.Equipment
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeEditScreen(
    navController: NavController,
    recipeId: Long,
    viewModel: RecipeViewModel = viewModel(),
    beanViewModel: BeanViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val beans by beanViewModel.allBeans.collectAsState(initial = emptyList())
    val isEditing = recipeId > 0

    var name by remember { mutableStateOf("") }
    var selectedBeanId by remember { mutableStateOf(-1L) }
    var equipment by remember { mutableStateOf("") }
    var coffeeWeight by remember { mutableStateOf("") }
    var waterWeight by remember { mutableStateOf("") }
    var waterTemp by remember { mutableStateOf("") }
    var grindSize by remember { mutableStateOf("") }
    var bloomTime by remember { mutableStateOf("") }
    var pourCount by remember { mutableStateOf("") }
    var totalTime by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val equipmentItems = Equipment.DEFAULT_EQUIPMENT

    LaunchedEffect(recipeId) {
        if (isEditing) {
            val recipe = viewModel.getRecipe(recipeId)
            recipe?.let { r ->
                name = r.name
                selectedBeanId = r.beanId ?: -1L
                equipment = r.equipment
                coffeeWeight = if (r.coffeeWeight > 0) r.coffeeWeight.toString() else ""
                waterWeight = if (r.waterWeight > 0) r.waterWeight.toString() else ""
                waterTemp = if (r.waterTemp > 0) r.waterTemp.toString() else ""
                grindSize = r.grindSize
                bloomTime = if (r.bloomTime > 0) r.bloomTime.toString() else ""
                pourCount = if (r.pourCount > 0) r.pourCount.toString() else ""
                totalTime = if (r.totalTime > 0) r.totalTime.toString() else ""
                notes = r.notes
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑配方" else "新建配方") },
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("配方名称 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
                    value = bloomTime,
                    onValueChange = { bloomTime = it },
                    label = { Text("焖蒸时间 (秒)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = pourCount,
                    onValueChange = { pourCount = it },
                    label = { Text("注水次数") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = totalTime,
                onValueChange = { totalTime = it },
                label = { Text("总时长 (秒)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = {
                    scope.launch {
                        val recipe = BrewRecipe(
                            id = if (isEditing) recipeId else 0,
                            name = name,
                            beanId = if (selectedBeanId > 0) selectedBeanId else null,
                            equipment = equipment,
                            coffeeWeight = coffeeWeight.toDoubleOrNull() ?: 0.0,
                            waterWeight = waterWeight.toDoubleOrNull() ?: 0.0,
                            waterTemp = waterTemp.toDoubleOrNull() ?: 0.0,
                            grindSize = grindSize,
                            bloomTime = bloomTime.toIntOrNull() ?: 0,
                            pourCount = pourCount.toIntOrNull() ?: 0,
                            totalTime = totalTime.toIntOrNull() ?: 0,
                            notes = notes
                        )
                        if (isEditing) {
                            viewModel.updateRecipe(recipe)
                        } else {
                            viewModel.saveRecipeSync(recipe)
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("保存配方")
            }
        }
    }
}
