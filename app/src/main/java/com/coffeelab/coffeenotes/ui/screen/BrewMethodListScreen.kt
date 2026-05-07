package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.Converters
import com.coffeelab.coffeenotes.data.entity.BrewMethod
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BrewMethodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewMethodListScreen(
    navController: NavController,
    viewModel: BrewMethodViewModel = viewModel()
) {
    val methods by viewModel.allMethods.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("☕ 冲煮手法") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.BrewMethodEdit.createRoute()) }) {
                        Icon(Icons.Default.Add, contentDescription = "新建手法", tint = MaterialTheme.colorScheme.onPrimary)
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
        if (methods.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有冲煮手法\n点击 + 新建 🧪",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(methods) { method ->
                    MethodCard(
                        method = method,
                        onClick = {
                            navController.navigate(Screen.BrewMethodEdit.createRoute(method.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MethodCard(
    method: BrewMethod,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (method.isPreset) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "预置",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(method.name, style = MaterialTheme.typography.titleMedium)
                }
                val parsedSteps = Converters.parseSteps(method.steps)
                val stepSummary = parsedSteps.withIndex().joinToString(" → ") { (i, step) ->
                    val water = step.waterAmount?.let { "${it}ml" } ?: "至总水量"
                    "${i + 1}: ${water}/${step.durationSeconds}s"
                }
                Text(
                    stepSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
