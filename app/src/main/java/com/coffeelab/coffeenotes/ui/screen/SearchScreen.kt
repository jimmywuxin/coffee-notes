package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.layout.*
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
import com.coffeelab.coffeenotes.viewmodel.BrewViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    beanViewModel: BeanViewModel = viewModel(),
    brewViewModel: BrewViewModel = viewModel()
) {
    val allBeans by beanViewModel.allBeans.collectAsState(initial = emptyList())
    val allRecords by brewViewModel.allRecords.collectAsState(initial = emptyList())

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
            val beans = allBeans.filter {
                it.roaster.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true) ||
                it.origin.contains(query, ignoreCase = true) ||
                it.process.contains(query, ignoreCase = true) ||
                it.variety.contains(query, ignoreCase = true)
            }
            val records = allRecords.filter {
                it.equipment.contains(query, ignoreCase = true) ||
                it.flavorNotes.contains(query, ignoreCase = true) ||
                it.grindSize.contains(query, ignoreCase = true)
            }
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索烘焙商、豆名、产地、器具...") },
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

                val equipmentTypes = allRecords.map { it.equipment }.distinct().filter { it.isNotEmpty() }
                if (equipmentTypes.isNotEmpty()) {
                    Text("按器具：", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        equipmentTypes.take(6).forEach { equip ->
                            SuggestionChip(
                                onClick = { query = equip },
                                label = { Text(equip) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (allBeans.isNotEmpty()) {
                    Text("按烘焙商：", style = MaterialTheme.typography.bodyMedium)
                    val roasters = allBeans.map { it.roaster }.filter { it.isNotEmpty() }.distinct()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        roasters.take(5).forEach { roaster ->
                            SuggestionChip(
                                onClick = { query = roaster },
                                label = { Text(roaster) }
                            )
                        }
                    }
                }
            }
        }
    }
}
