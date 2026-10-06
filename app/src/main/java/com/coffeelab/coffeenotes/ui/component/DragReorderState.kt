package com.coffeelab.coffeenotes.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 拖拽排序状态机。
 *
 * 抽它的理由：这段「累积拖动距离 → 与相邻项中心比较 → 换位→ 重置偏移」的逻辑原先在
 * SingleNameManagementScreen / RoastDegreeConfigScreen / BrewMethodListScreen / BeanListScreen
 * 四处各写一遍，已经漂移过一次（BeanListScreen 少写了 `accumulatedDrag = 0f` 重置，
 * 拖拽项会「甩」在手指后面）。
 *
 * 算法：把被拖项的当前中心（含累积偏移）与所有可见项中心比较，
 * 向下拖越过谁就取谁的最大索引，向上拖取最小索引，即目标位置。
 *
 * @param items 要重排的列表（调用方持有其可变副本）
 * @param onReorderStart 拖拽开始（用于暂停其他交互）
 * @param onReorderEnd 拖拽结束，参数为最终顺序
 */
@Composable
fun <T> rememberDragReorderState(
    lazyListState: LazyListState,
    items: MutableList<T>,
    onReorderStart: () -> Unit = {},
    onReorderEnd: (List<T>) -> Unit = {}
): DragReorderState<T> {
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    var dragStartItemOffset by remember { mutableFloatStateOf(0f) }

    val smoothDragOffset by animateFloatAsState(
        targetValue = accumulatedDrag,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
    )

    return DragReorderState(
        draggingItemIndex = draggingItemIndex,
        smoothDragOffset = if (draggingItemIndex >= 0) smoothDragOffset else 0f,
        accumulatedDrag = accumulatedDrag,
        dragStartItemOffset = dragStartItemOffset,
        onDragStart = { index ->
            draggingItemIndex = index
            accumulatedDrag = 0f
            dragStartItemOffset = lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == index }?.offset?.toFloat() ?: 0f
            onReorderStart()
        },
        onDrag = { dragAmount ->
            accumulatedDrag += dragAmount
            val layoutInfo = lazyListState.layoutInfo
            val myInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }
            if (myInfo != null) {
                val currentCenterY = myInfo.offset + myInfo.size / 2 + accumulatedDrag

                var targetIndex = draggingItemIndex
                layoutInfo.visibleItemsInfo
                    .filter { it.index != draggingItemIndex }
                    .forEach { info ->
                        val itemCenter = info.offset + info.size / 2
                        if (accumulatedDrag > 0 && info.index > draggingItemIndex && currentCenterY > itemCenter) {
                            targetIndex = maxOf(targetIndex, info.index)
                        } else if (accumulatedDrag < 0 && info.index < draggingItemIndex && currentCenterY < itemCenter) {
                            targetIndex = minOf(targetIndex, info.index)
                        }
                    }
                targetIndex = targetIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
                if (targetIndex != draggingItemIndex) {
                    items.add(targetIndex, items.removeAt(draggingItemIndex))
                    draggingItemIndex = targetIndex
                    // 重置偏移，否则换位后item 会闪跳
                    accumulatedDrag = 0f
                    dragStartItemOffset = lazyListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == targetIndex }?.offset?.toFloat() ?: 0f
                }
            }
        },
        onDragEnd = {
            draggingItemIndex = -1
            accumulatedDrag = 0f
            onReorderEnd(items.toList())
        }
    )
}

/** [rememberDragReorderState] 的返回物，喂给 [DraggableItem] 的对应参数 */
class DragReorderState<T>(
    val draggingItemIndex: Int,
    val smoothDragOffset: Float,
    val accumulatedDrag: Float,
    val dragStartItemOffset: Float,
    val onDragStart: (Int) -> Unit,
    val onDrag: (Float) -> Unit,
    val onDragEnd: () -> Unit
)
