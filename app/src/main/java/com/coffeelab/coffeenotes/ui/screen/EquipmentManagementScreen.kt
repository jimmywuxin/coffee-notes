package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.ui.component.SingleNameManagementScreen
import com.coffeelab.coffeenotes.viewmodel.EquipmentViewModel

@Composable
fun EquipmentManagementScreen(
    navController: NavController,
    equipmentViewModel: EquipmentViewModel = viewModel()
) {
    val equipmentList by equipmentViewModel.allEquipment.collectAsStateWithLifecycle(initialValue = emptyList())
    SingleNameManagementScreen(
        title = "器具管理",
        sortTitle = "器具排序",
        addPlaceholder = "器具名称",
        emptyEmoji = "🔧",
        emptyMessage = "还没有器具",
        icon = Icons.Default.LocalCafe,
        items = equipmentList,
        getId = { it.id },
        getName = { it.name },
        onAdd = { equipmentViewModel.addEquipment(it) },
        onRename = { item, name -> equipmentViewModel.updateEquipment(item.copy(name = name)) },
        onDelete = { equipmentViewModel.deleteEquipment(it) },
        onSaveOrder = { equipmentViewModel.saveEquipmentOrder(it) }
    )
}
