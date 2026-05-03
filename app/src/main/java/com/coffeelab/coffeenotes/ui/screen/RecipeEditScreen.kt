package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.BrewRecipe
import com.coffeelab.coffeenotes.data.entity.Equipment
import com.coffeelab.coffeenotes.data.entity.Grinder
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.RecipeViewModel
import com.coffeelab.coffeenotes.viewmodel.EquipmentViewModel
import com.coffeelab.coffeenotes.viewmodel.GrinderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeEditScreen(
    navController: NavController,
    recipeId: Long,
    viewModel: RecipeViewModel = viewModel(),
    beanViewModel: BeanViewModel = viewModel(),
    equipmentViewModel: EquipmentViewModel = viewModel(),
    grinderViewModel: GrinderViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val beans by beanViewModel.allBeans.collectAsState(initial = emptyList())
    val isEditing = recipeId > 0

    var name by remember { mutableStateOf("") }
    var selectedBeanId by remember { mutableStateOf(-1L) }
    var equipment by remember { mutableStateOf("") }
    var coffeeWeight by remember { mutableStateOf("") }
    var coffeeWaterRatio by remember { mutableStateOf("15") }
    var waterTemp by remember { mutableStateOf("") }
    var grinder by remember { mutableStateOf("") }
    var grindSize by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Equipment list from database
    val equipmentList by equipmentViewModel.allEquipment.collectAsState(initial = emptyList())
    val equipmentItems = if (equipmentList.isNotEmpty()) {
        equipmentList.map { it.name }
    } else {
        Equipment.DEFAULT_EQUIPMENT
    }

    // Grinder list from database
    val grinderList by grinderViewModel.allGrinders.collectAsState(initial = emptyList())
    val grinderItems = if (grinderList.isNotEmpty()) {
        grinderList.map { it.name }
    } else {
        Grinder.DEFAULT_GRINDERS
    }

    LaunchedEffect(recipeId) {
        if (isEditing) {
            val recipe = viewModel.getRecipe(recipeId)
            recipe?.let { r ->
                name = r.name
                selectedBeanId = r.beanId ?: -1L
                equipment = r.equipment
                coffeeWeight = if (r.coffeeWeight > 0) r.coffeeWeight.toString() else ""
                coffeeWaterRatio = if (r.coffeeWaterRatio > 0) r.coffeeWaterRatio.toString() else ""
                waterTemp = if (r.waterTemp > 0) r.waterTemp.toString() else ""
                grinder = r.grinder
                grindSize = r.grindSize
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

            // Ratio options
            val ratioOptions = listOf("15", "16", "17", "2")
            Text("粉水比", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ratioOptions.forEach { ratio ->
                    FilterChip(
                        selected = coffeeWaterRatio == ratio,
                        onClick = { coffeeWaterRatio = ratio },
                        label = { Text("1:$ratio") }
                    )
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
                    value = waterTemp,
                    onValueChange = { waterTemp = it },
                    label = { Text("水温 (℃)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Grinder + Grind Size
            Text("研磨", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            coffeeWaterRatio = coffeeWaterRatio.toDoubleOrNull() ?: 0.0,
                            waterTemp = waterTemp.toDoubleOrNull() ?: 0.0,
                            grinder = grinder,
                            grindSize = grindSize,
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
