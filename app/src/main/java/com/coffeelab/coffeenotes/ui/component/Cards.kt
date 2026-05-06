package com.coffeelab.coffeenotes.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.util.DateUtils

// ==================== BeanCard ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BeanCard(
    bean: CoffeeBean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (isSelected) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bean.name.ifEmpty { "未命名豆子" },
                    style = MaterialTheme.typography.titleMedium
                )
                if (bean.roaster.isNotEmpty()) {
                    Text(
                        text = bean.roaster,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bean.origin.isNotEmpty()) {
                        Text(
                            text = bean.origin,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (bean.region.isNotEmpty()) {
                        Text(
                            text = bean.region,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (bean.process.isNotEmpty()) {
                        Text(
                            text = bean.process,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (bean.roastLevel.isNotEmpty()) {
                        Text(
                            text = bean.roastLevel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            if (!isSelectionMode) {
                Icon(
                    imageVector = if (bean.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (bean.isFavorite) "取消收藏" else "收藏",
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onFavoriteClick() },
                    tint = if (bean.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== RecordCard ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordCard(
    record: BrewRecord,
    beanName: String,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (isSelected) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = beanName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${record.equipment} · ${DateUtils.formatDateTime(record.dateTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = buildString {
                        append("${record.coffeeWeight}g")
                        append(" · 1:${String.format("%.1f", record.coffeeWaterRatio)}")
                        if (record.waterTemp > 0) append(" · ${record.waterTemp}℃")
                        if (record.isIced) append(" · ❄️${record.iceAmount}g")
                        if (record.bypassAmount > 0) append(" · +${record.bypassAmount}ml bypass")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (record.overallRating > 0) {
                Text(
                    text = "★".repeat(record.overallRating),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
