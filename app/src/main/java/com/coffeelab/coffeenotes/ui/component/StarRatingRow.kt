package com.coffeelab.coffeenotes.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StarRatingRow(
    label: String,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    large: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp)
        )
        for (i in 1..5) {
            IconButton(
                onClick = { onRatingChange(if (rating == i) 0 else i) },
                modifier = Modifier.size(if (large) 36.dp else 28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "星 $i",
                    tint = if (i <= rating) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(if (large) 28.dp else 20.dp)
                )
            }
        }
    }
}
