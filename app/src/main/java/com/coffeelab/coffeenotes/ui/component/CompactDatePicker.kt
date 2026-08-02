package com.coffeelab.coffeenotes.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

/**
 * 紧凑日期选择面板（替代 Material3 DatePicker——M3 DatePicker 在中文 locale 下
 * 周六/周日表头文字重叠渲染成「根」，1.4.0 仍未修复，故自绘）。
 *
 * 结构：年份/月份切换 + 一二三四五六日表头 + 6 行×7 列日表。
 */
@Composable
fun CompactDatePicker(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var year by remember { mutableIntStateOf(initialDate.year) }
    var month by remember { mutableIntStateOf(initialDate.monthValue) }
    var selectedDay by remember { mutableIntStateOf(initialDate.dayOfMonth) }

    val daysInMonth = remember(year, month) { YearMonth.of(year, month).lengthOfMonth() }
    // 当月 1 号是星期几（1=周一 … 7=周日），前面补空格让日期对齐
    val leadingBlanks = remember(year, month) {
        val dow = LocalDate.of(year, month, 1).dayOfWeek.value
        (dow - 1 + 7) % 7  // 周一开头：周一=0
    }
    val totalCells = leadingBlanks + daysInMonth

    fun changeMonth(delta: Int) {
        var y = year
        var m = month + delta
        if (m < 1) { m = 12; y-- }
        if (m > 12) { m = 1; y++ }
        year = y
        month = m
        // 切换月份后若原选中日超出新月份天数，收敛到月末
        val maxDay = YearMonth.of(y, m).lengthOfMonth()
        if (selectedDay > maxDay) selectedDay = maxDay
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // ===== 年份/月份切换 =====
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            IconButton(onClick = { changeMonth(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
            }
            Text(
                text = "%d年%d月".format(year, month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { changeMonth(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
            }
        }

        // ===== 周表头（周一开头） =====
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (label == "六" || label == "日")
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // ===== 日期网格（6 行 × 7 列，动态） =====
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - leadingBlanks + 1
                    if (day < 1 || day > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val isSelected = day == selectedDay
                        val isToday = day == LocalDate.now().dayOfMonth &&
                            year == LocalDate.now().year && month == LocalDate.now().monthValue
                        Surface(
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                .padding(2.dp)
                                .clickable {
                                    selectedDay = day
                                    onDateSelected(LocalDate.of(year, month, day))
                                },
                            shape = MaterialTheme.shapes.extraSmall,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
