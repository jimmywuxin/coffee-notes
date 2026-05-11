package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        when (searchScope) {
                            "beans" -> "搜索烘焙商、豆名、产地..."
                            "records" -> "搜索器具、风味、研磨度、冲煮手法..."
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
                    val equipmentTypes = allRecords.map { it.equipment }.distinct().filter { it.isNotEmpty() }
                    if (equipmentTypes.isNotEmpty()) {
                        Text("按器具：", style = MaterialTheme.typography.bodyMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            equipmentTypes.forEach { equip ->
                                SuggestionChip(
                                    onClick = { query = equip },
                                    label = { Text(equip) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    // 按冲煮手法
                    val methodNames = allRecords.mapNotNull { record ->
                        allMethods.find { it.id == record.methodId }?.name
                    }.distinct().filter { it.isNotEmpty() }
                    if (methodNames.isNotEmpty()) {
                        Text("按冲煮手法：", style = MaterialTheme.typography.bodyMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            methodNames.forEach { method ->
                                SuggestionChip(
                                    onClick = { query = method },
                                    label = { Text(method) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (searchScope == "all" || searchScope == "beans") {
                    if (allBeans.isNotEmpty()) {
                        Text("按烘焙商：", style = MaterialTheme.typography.bodyMedium)
                        val roasters = allBeans.map { it.roaster }.filter { it.isNotEmpty() }.distinct()
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            roasters.forEach { roaster ->
                                SuggestionChip(
                                    onClick = { query = roaster },
                                    label = { Text(roaster) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("按处理法：", style = MaterialTheme.typography.bodyMedium)
                        val processes = allBeans.map { it.process }.filter { it.isNotEmpty() }.distinct()
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            processes.forEach { process ->
                                SuggestionChip(
                                    onClick = { query = process },
                                    label = { Text(process) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
