package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.ui.component.BeanCard
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.viewmodel.BeanViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BeanListScreen(
    navController: NavController,
    viewModel: BeanViewModel = viewModel()
) {
    val beans by viewModel.allBeans.collectAsState(initial = emptyList())

    // 多选状态
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedBeans by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    // 显示的豆子（筛选收藏）
    val displayedBeans = if (showFavoritesOnly) beans.filter { it.isFavorite } else beans

    // 进入多选模式
    fun enterSelectionMode(bean: CoffeeBean) {
        isSelectionMode = true
        selectedBeans = setOf(bean.id)
    }

    // 切换选中状态
    fun toggleSelection(beanId: Long) {
        selectedBeans = if (selectedBeans.contains(beanId)) {
            selectedBeans - beanId
        } else {
            selectedBeans + beanId
        }
        if (selectedBeans.isEmpty()) {
            isSelectionMode = false
        }
    }

    // 全选/取消全选
    fun toggleSelectAll() {
        if (selectedBeans.size == beans.size) {
            // 已全选 → 取消全部
            selectedBeans = emptySet()
            isSelectionMode = false
        } else {
            // 未全选 → 选中全部
            selectedBeans = beans.map { it.id }.toSet()
        }
    }

    // 退出多选模式
    fun exitSelectionMode() {
        isSelectionMode = false
        selectedBeans = emptySet()
    }

    // 删除所选
    fun deleteSelected() {
        showDeleteDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedBeans.size} 条已选")
                    } else {
                        Text("🫘 我的豆子")
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (!isSelectionMode) {
                        IconButton(onClick = { navController.navigate(Screen.BeanEdit.createRoute()) }) {
                            Icon(Icons.Default.Add, contentDescription = "添加豆子")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSelectionMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    titleContentColor = if (isSelectionMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = if (isSelectionMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = if (isSelectionMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode && selectedBeans.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { toggleSelectAll() }) {
                                Icon(Icons.Default.SelectAll, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("全选")
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "已选 ${selectedBeans.size} 条",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = { deleteSelected() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("删除")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (beans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有咖啡豆，点击 + 添加吧 🫘",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                // 筛选栏
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !showFavoritesOnly,
                            onClick = { showFavoritesOnly = false },
                            label = { Text("全部") }
                        )
                        FilterChip(
                            selected = showFavoritesOnly,
                            onClick = { showFavoritesOnly = true },
                            label = { Text("❤ 收藏") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                }

                // 空状态
                if (displayedBeans.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (showFavoritesOnly) "还没有收藏的豆子 🫑" else "还没有咖啡豆，点击 + 添加吧 🫘",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(displayedBeans) { bean ->
                    val isSelected = selectedBeans.contains(bean.id)

                    BeanCard(
                        bean = bean,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                toggleSelection(bean.id)
                            } else {
                                navController.navigate(Screen.BeanDetail.createRoute(bean.id))
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                enterSelectionMode(bean)
                            }
                        },
                        onFavoriteClick = {
                            viewModel.toggleFavorite(bean)
                        }
                    )
                }
            }
        }
    }

    // 批量删除确认弹窗
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = {
                Text("确定要删除这 ${selectedBeans.size} 条豆子吗？\n该豆子的所有冲煮记录也会被删除。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        beans.filter { selectedBeans.contains(it.id) }.forEach {
                            viewModel.deleteBean(it)
                        }
                        showDeleteDialog = false
                        exitSelectionMode()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}
