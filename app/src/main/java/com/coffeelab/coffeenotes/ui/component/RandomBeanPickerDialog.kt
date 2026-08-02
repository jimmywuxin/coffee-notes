package com.coffeelab.coffeenotes.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeelab.coffeenotes.data.entity.CoffeeBean

/**
 * 随机选豆弹窗：从【在喝（未归档）】豆子中随机挑一包展示，可重新随机或直接进入详情。
 *
 * @param activeBeans 在喝的豆子列表（调用方传入时已过滤归档，这里再做一次兜底过滤）
 */
@Composable
fun RandomBeanPickerDialog(
    activeBeans: List<CoffeeBean>,
    onDismiss: () -> Unit,
    onPick: (CoffeeBean) -> Unit
) {
    val pool = remember(activeBeans) { activeBeans.filter { !it.isArchived } }
    var current by remember(pool) { mutableStateOf(pool.randomOrNull()) }
    val selected = current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎲 随机选豆") },
        text = {
            if (pool.isEmpty()) {
                Text("在喝的豆子为空，先添加豆子吧")
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selected?.let { "${it.roaster} ${it.name}" } ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = selected?.let { bean ->
                            listOfNotNull(
                                bean.origin.ifEmpty { null },
                                bean.process.ifEmpty { null },
                                bean.roastLevel.ifEmpty { null }
                            ).joinToString(" · ")
                        } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            if (pool.isNotEmpty()) {
                TextButton(onClick = { current = pool.randomOrNull() }) { Text("换一个") }
            }
        },
        dismissButton = {
            if (pool.isNotEmpty()) {
                TextButton(onClick = { selected?.let(onPick) }) { Text("就喝这包") }
            } else {
                TextButton(onClick = onDismiss) { Text("知道了") }
            }
        }
    )
}
