package com.coffeelab.coffeenotes.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 五维雷达图（风味雷达图）
 *
 * 5个角代表的维度含义：
 * - 酸度：咖啡的酸感强度，低酸像水果般清新，高酸像醋般刺激
 * - 甜感：咖啡的甜味强度，来源于糖分焦化产生的甜感
 * - 苦味：咖啡的苦味强度，高苦通常表示萃取过度或豆子深烘
 * - 口感：咖啡在口腔中的醇厚度感受，Body 感，低口感如水，高口感如奶油
 * - 回甘：吞咽后余韵的甜感长度，好回甘带来持久的正向余韵体验
 *
 * @param values 5个维度的分值（1-5），顺序固定为：酸度、甜感、苦味、口感、回甘
 * @param labels 5个维度的标签，默认已有
 * @param maxValue 最大值，默认为5
 */
@Composable
fun RadarChart(
    values: List<Float>,
    labels: List<String> = listOf("酸度", "甜感", "苦味", "口感", "回甘"),
    maxValue: Float = 5f,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2 - 10.dp.toPx()
        val radius = minOf(centerX, centerY) * 0.6f
        val axisCount = values.size.coerceAtLeast(5)
        val angleStep = (2 * PI / axisCount).toFloat()

        // 绘制同心多边形（背景网格），使用 onSurface 色（更清晰）
        val gridLevels = listOf(0.25f, 0.5f, 0.75f, 1f)
        gridLevels.forEach { level ->
            drawPolygon(
                centerX = centerX,
                centerY = centerY,
                radius = radius * level,
                sides = axisCount,
                angleStep = angleStep,
                color = onSurface.copy(alpha = 0.2f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // 绘制轴线和维度标签
        for (i in 0 until axisCount) {
            val angle = angleStep * i - PI.toFloat() / 2
            val endX = centerX + radius * cos(angle)
            val endY = centerY + radius * sin(angle)

            // 轴线
            drawLine(
                color = onSurface.copy(alpha = 0.2f),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 1.5.dp.toPx()
            )

            // 绘制维度标签
            val labelText = labels.getOrElse(i) { "" }
            val labelRadius = radius + 22.dp.toPx()
            val labelX = centerX + labelRadius * cos(angle)
            val labelY = centerY + labelRadius * sin(angle)

            val textLayout = textMeasurer.measure(
                text = labelText,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = onSurface.copy(alpha = 0.7f)
                )
            )
            val textWidth = textLayout.size.width
            val textHeight = textLayout.size.height

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    labelX - textWidth / 2,
                    labelY - textHeight / 2
                )
            )
        }

        // 绘制数据区域
        if (values.isNotEmpty()) {
            val dataPath = Path()
            values.forEachIndexed { i, value ->
                val angle = angleStep * i - PI.toFloat() / 2
                val normalizedValue = (value / maxValue).coerceIn(0f, 1f)
                val pointRadius = radius * normalizedValue
                val x = centerX + pointRadius * cos(angle)
                val y = centerY + pointRadius * sin(angle)
                if (i == 0) {
                    dataPath.moveTo(x, y)
                } else {
                    dataPath.lineTo(x, y)
                }
            }
            dataPath.close()

            // 填充区域（半透明）
            drawPath(
                path = dataPath,
                color = primaryColor.copy(alpha = 0.3f),
                style = Fill
            )
            // 描边
            drawPath(
                path = dataPath,
                color = primaryColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // 绘制数据点
            values.forEachIndexed { i, value ->
                val angle = angleStep * i - PI.toFloat() / 2
                val normalizedValue = (value / maxValue).coerceIn(0f, 1f)
                val pointRadius = radius * normalizedValue
                val x = centerX + pointRadius * cos(angle)
                val y = centerY + pointRadius * sin(angle)
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

private fun DrawScope.drawPolygon(
    centerX: Float,
    centerY: Float,
    radius: Float,
    sides: Int,
    angleStep: Float,
    color: Color,
    style: Stroke
) {
    val path = Path()
    for (i in 0 until sides) {
        val angle = angleStep * i - PI.toFloat() / 2
        val x = centerX + radius * cos(angle)
        val y = centerY + radius * sin(angle)
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    drawPath(path, color = color, style = style)
}
