package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.ui.component.EmptyState
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveListScreen(
    navController: NavController,
    viewModel: BeanViewModel = viewModel()
) {
    val archivedBeans by viewModel.archivedBeans.collectAsStateWithLifecycle(initialValue = emptyList())
    var showUnarchiveDialog by remember { mutableStateOf<CoffeeBean?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Archive, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("已归档的豆子") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (archivedBeans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    emoji = "📁",
                    message = "还没有归档的豆子"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(archivedBeans, key = { it.id }) { bean ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(Screen.BeanDetail.createRoute(bean.id))
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bean.name.ifEmpty { "(未命名)" },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${bean.roaster} ${if (bean.origin.isNotEmpty()) "· ${bean.origin}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (bean.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (bean.isFavorite) "已收藏" else "未收藏",
                                modifier = Modifier.padding(start = 8.dp),
                                tint = if (bean.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(onClick = { showUnarchiveDialog = bean }) {
                                Icon(
                                    Icons.Default.Unarchive,
                                    contentDescription = "取消归档",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Unarchive Confirmation Dialog
    showUnarchiveDialog?.let { bean ->
        AlertDialog(
            onDismissRequest = { showUnarchiveDialog = null },
            title = { Text("取消归档") },
            text = { Text("将「${bean.name}」恢复到豆子列表？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unarchiveBean(bean)
                        showUnarchiveDialog = null
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showUnarchiveDialog = null }) { Text("取消") }
            }
        )
    }
}
