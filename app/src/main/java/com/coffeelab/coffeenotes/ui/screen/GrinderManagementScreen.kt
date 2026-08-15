package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.ui.component.SingleNameManagementScreen
import com.coffeelab.coffeenotes.viewmodel.GrinderViewModel

@Composable
fun GrinderManagementScreen(
    navController: NavController,
    grinderViewModel: GrinderViewModel = viewModel()
) {
    val grinderList by grinderViewModel.allGrinders.collectAsStateWithLifecycle(initialValue = emptyList())
    SingleNameManagementScreen(
        title = "磨豆机管理",
        sortTitle = "磨豆机排序",
        addPlaceholder = "磨豆机名称",
        emptyEmoji = "⚙️",
        emptyMessage = "还没有磨豆机",
        icon = Icons.Default.Refresh,
        items = grinderList,
        getId = { it.id },
        getName = { it.name },
        onAdd = { grinderViewModel.addGrinder(it) },
        onRename = { item, name -> grinderViewModel.updateGrinder(item.copy(name = name)) },
        onDelete = { grinderViewModel.deleteGrinder(it) },
        onSaveOrder = { grinderViewModel.saveGrinderOrder(it) }
    )
}
