package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.ui.component.BeanCard
import com.coffeelab.coffeenotes.ui.component.RecordCard
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewMethodViewModel
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    navController: NavController,
    searchScope: String = "all",
    beanViewModel: BeanViewModel = viewModel(),
    brewViewModel: BrewViewModel = viewModel(),
    methodViewModel: BrewMethodViewModel = viewModel()
) {
    val allBeans by beanViewModel.allBeans.collectAsState(initial = emptyList())
    val allRecords by brewViewModel.allRecords.collectAsState(initial = emptyList())
    val allMethods by methodViewModel.allMethods.collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Any>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        searchJob = scope.launch {
            delay(300) // debounce
            val beans = if (searchScope == "all" || searchScope == "beans") {
                allBeans.filter {
                    it.roaster.contains(query, ignoreCase = true) ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.origin.contains(query, ignoreCase = true) ||
                    it.process.contains(query, ignoreCase = true) ||
                    it.variety.contains(query, ignoreCase = true)
                }
            } else emptyList()
            val records = if (searchScope == "all" || searchScope == "records") {
                allRecords.filter { record ->
                    val methodName = allMethods.find { it.id == record.methodId }?.name ?: ""
                    record.equipment.contains(query, ignoreCase = true) ||
                    record.flavorNotes.contains(query, ignoreCase = true) ||
                    record.grindSize.contains(query, ignoreCase = true) ||
                    methodName.contains(query, ignoreCase = true)
                }
            } else emptyList()
            searchResults = beans + records
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔍 搜索") },
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
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        when (searchScope) {
                            "beans" -> "搜索烘焙商、豆名、产地..."
                            "records" -> "搜索器具、冲煮手法..."
                            else -> "搜索烘焙商、豆名、产地、器具..."
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (query.isNotBlank()) {
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("未找到匹配结果")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 先显示匹配的豆子
                        val matchedBeans = searchResults.filterIsInstance<com.coffeelab.coffeenotes.data.entity.CoffeeBean>()
                        items(matchedBeans) { bean ->
                            BeanCard(
                                bean = bean,
                                onClick = {
                                    navController.navigate(Screen.BeanDetail.createRoute(bean.id))
                                },
                                onFavoriteClick = {
                                    beanViewModel.toggleFavorite(bean)
                                }
                            )
                        }
                        // 再显示匹配的冲煮记录
                        val matchedRecords = searchResults.filterIsInstance<com.coffeelab.coffeenotes.data.entity.BrewRecord>()
                        items(matchedRecords) { record ->
                            val beanName = allBeans.find { it.id == record.beanId }?.name ?: "未知"
                            RecordCard(
                                record = record,
                                beanName = beanName,
                                onClick = {
                                    navController.navigate(
                                        Screen.BrewEdit.createRoute(record.id, record.beanId)
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                // Filters section
                Text("按条件筛选", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (searchScope == "all" || searchScope == "records") {
                    var equipExpanded by remember { mutableStateOf(false) }
                    var methodExpanded by remember { mutableStateOf(false) }
                    var selectedEquipment by remember { mutableStateOf("") }
                    var selectedMethod by remember { mutableStateOf("") }

                    val equipmentTypes = listOf("全部") + allRecords.map { it.equipment }.distinct().filter { it.isNotEmpty() }
                    val methodNames = listOf("全部") + allRecords.mapNotNull { record ->
                        allMethods.find { it.id == record.methodId }?.name
                    }.distinct().filter { it.isNotEmpty() }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 器具下拉框
                        ExposedDropdownMenuBox(
                            expanded = equipExpanded,
                            onExpandedChange = { equipExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = if (selectedEquipment.isEmpty()) "器具" else selectedEquipment,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("器具") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = equipExpanded,
                                onDismissRequest = { equipExpanded = false },
                                modifier = Modifier.heightIn(max = 250.dp)
                            ) {
                                equipmentTypes.forEach { equip ->
                                    DropdownMenuItem(
                                        text = { Text(equip) },
                                        onClick = {
                                            selectedEquipment = equip
                                            query = if (equip == "全部") "" else equip
                                            equipExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 冲煮手法下拉框
                        ExposedDropdownMenuBox(
                            expanded = methodExpanded,
                            onExpandedChange = { methodExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = if (selectedMethod.isEmpty()) "冲煮手法" else selectedMethod,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("冲煮手法") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = methodExpanded,
                                onDismissRequest = { methodExpanded = false },
                                modifier = Modifier.heightIn(max = 250.dp)
                            ) {
                                methodNames.forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method) },
                                        onClick = {
                                            selectedMethod = method
                                            query = if (method == "全部") "" else method
                                            methodExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (searchScope == "all" || searchScope == "beans") {
                    if (allBeans.isNotEmpty()) {
                        var roasterExpanded by remember { mutableStateOf(false) }
                        var processExpanded by remember { mutableStateOf(false) }
                        var selectedRoaster by remember { mutableStateOf("") }
                        var selectedProcess by remember { mutableStateOf("") }

                        val roasters = listOf("全部") + allBeans.map { it.roaster }.filter { it.isNotEmpty() }.distinct()
                        val processes = listOf("全部") + allBeans.map { it.process }.filter { it.isNotEmpty() }.distinct()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 烘焙商下拉框
                            ExposedDropdownMenuBox(
                                expanded = roasterExpanded,
                                onExpandedChange = { roasterExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = if (selectedRoaster.isEmpty()) "烘焙商" else selectedRoaster,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("烘焙商") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roasterExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = roasterExpanded,
                                    onDismissRequest = { roasterExpanded = false },
                                    modifier = Modifier.heightIn(max = 250.dp)
                                ) {
                                    roasters.forEach { roaster ->
                                        DropdownMenuItem(
                                            text = { Text(roaster) },
                                            onClick = {
                                                selectedRoaster = roaster
                                                query = if (roaster == "全部") "" else roaster
                                                roasterExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 处理法下拉框
                            ExposedDropdownMenuBox(
                                expanded = processExpanded,
                                onExpandedChange = { processExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = if (selectedProcess.isEmpty()) "处理法" else selectedProcess,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("处理法") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = processExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = processExpanded,
                                    onDismissRequest = { processExpanded = false },
                                    modifier = Modifier.heightIn(max = 250.dp)
                                ) {
                                    processes.forEach { process ->
                                        DropdownMenuItem(
                                            text = { Text(process) },
                                            onClick = {
                                                selectedProcess = process
                                                query = if (process == "全部") "" else process
                                                processExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
