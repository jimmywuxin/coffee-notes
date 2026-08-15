package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.ui.component.SingleNameManagementScreen
import com.coffeelab.coffeenotes.viewmodel.ImpressionTagViewModel

@Composable
fun ImpressionTagManagementScreen(
    navController: NavController,
    viewModel: ImpressionTagViewModel = viewModel()
) {
    val items by viewModel.allImpressionTags.collectAsStateWithLifecycle(initialValue = emptyList())
    SingleNameManagementScreen(
        title = "印象标签管理",
        sortTitle = "印象标签排序",
        addPlaceholder = "标签名称",
        emptyEmoji = "🏷",
        emptyMessage = "暂无印象标签",
        icon = Icons.Default.Label,
        items = items,
        getId = { it.id },
        getName = { it.name },
        onAdd = { viewModel.addTag(it) },
        onRename = { item, name -> viewModel.updateTag(item.copy(name = name)) },
        onDelete = { viewModel.deleteTag(it) },
        onSaveOrder = { viewModel.saveOrder(it) }
    )
}
