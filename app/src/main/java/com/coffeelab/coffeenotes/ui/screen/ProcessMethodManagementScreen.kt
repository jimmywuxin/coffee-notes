package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.ui.component.SingleNameManagementScreen
import com.coffeelab.coffeenotes.viewmodel.ProcessMethodViewModel

@Composable
fun ProcessMethodManagementScreen(
    navController: NavController,
    viewModel: ProcessMethodViewModel = viewModel()
) {
    val items by viewModel.allProcessMethods.collectAsStateWithLifecycle(initialValue = emptyList())
    SingleNameManagementScreen(
        title = "处理法管理",
        sortTitle = "处理法排序",
        addPlaceholder = "处理法名称",
        emptyEmoji = "💧",
        emptyMessage = "暂无处理法",
        icon = Icons.Default.WaterDrop,
        items = items,
        getId = { it.id },
        getName = { it.name },
        onAdd = { viewModel.addProcessMethod(it) },
        onRename = { item, name -> viewModel.updateProcessMethod(item.copy(name = name)) },
        onDelete = { viewModel.deleteProcessMethod(it) },
        onSaveOrder = { viewModel.saveOrder(it) }
    )
}
