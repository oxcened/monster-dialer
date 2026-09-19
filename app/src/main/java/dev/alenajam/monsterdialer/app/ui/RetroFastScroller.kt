package dev.alenajam.monsterdialer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val MinimumItemCount = 12
private val RetroScrollerInk = Color(0xFF202020)
private val RetroScrollerRail = Color(0xFFE6DDF0)
private val RetroScrollerThumb = Color(0xFFBBA5D3)

@Composable
fun RetroFastScroller(
    listState: LazyListState,
    contentDescription: String,
    minimumItemCount: Int = MinimumItemCount,
    minimumViewportMultiplier: Int = 2,
    modifier: Modifier = Modifier,
) {
    val layoutInfo = listState.layoutInfo
    val visibleItemCount = layoutInfo.visibleItemsInfo.size
    val totalItemCount = layoutInfo.totalItemsCount
    if (!shouldShowFastScroller(totalItemCount, visibleItemCount, minimumItemCount, minimumViewportMultiplier)) return

    val position = if (!listState.canScrollForward) {
        1f
    } else {
        scrollPosition(listState.firstVisibleItemIndex, totalItemCount, visibleItemCount)
    }
    RetroFastScrollerTrack(
        position = position,
        visibleFraction = visibleItemCount.toFloat() / totalItemCount,
        contentDescription = contentDescription,
        onPositionChanged = { fraction ->
            listState.requestScrollToItem(targetIndex(fraction, totalItemCount, visibleItemCount))
        },
        modifier = modifier,
    )
}

@Composable
fun RetroFastScroller(
    gridState: LazyGridState,
    contentDescription: String,
    minimumItemCount: Int = MinimumItemCount,
    minimumViewportMultiplier: Int = 2,
    modifier: Modifier = Modifier,
) {
    val layoutInfo = gridState.layoutInfo
    val visibleItemCount = layoutInfo.visibleItemsInfo.size
    val totalItemCount = layoutInfo.totalItemsCount
    if (!shouldShowFastScroller(totalItemCount, visibleItemCount, minimumItemCount, minimumViewportMultiplier)) return

    val position = if (!gridState.canScrollForward) {
        1f
    } else {
        scrollPosition(gridState.firstVisibleItemIndex, totalItemCount, visibleItemCount)
    }
    RetroFastScrollerTrack(
        position = position,
        visibleFraction = visibleItemCount.toFloat() / totalItemCount,
        contentDescription = contentDescription,
        onPositionChanged = { fraction ->
            gridState.requestScrollToItem(targetIndex(fraction, totalItemCount, visibleItemCount))
        },
        modifier = modifier,
    )
}

private fun shouldShowFastScroller(
    totalItemCount: Int,
    visibleItemCount: Int,
    minimumItemCount: Int,
    minimumViewportMultiplier: Int,
): Boolean =
    totalItemCount > minimumItemCount && totalItemCount > visibleItemCount * minimumViewportMultiplier

private fun scrollPosition(firstVisibleItem: Int, totalItemCount: Int, visibleItemCount: Int): Float =
    (firstVisibleItem.toFloat() / (totalItemCount - visibleItemCount).coerceAtLeast(1)).coerceIn(0f, 1f)

private fun targetIndex(fraction: Float, totalItemCount: Int, visibleItemCount: Int): Int {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    if (clampedFraction >= 1f) return (totalItemCount - 1).coerceAtLeast(0)
    return (clampedFraction * (totalItemCount - visibleItemCount).coerceAtLeast(0)).roundToInt()
}

@Composable
private fun RetroFastScrollerTrack(
    position: Float,
    visibleFraction: Float,
    contentDescription: String,
    onPositionChanged: suspend (Float) -> Unit,
    modifier: Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = modifier
            .width(18.dp)
            .fillMaxHeight()
            .background(RetroScrollerRail)
            .border(2.dp, RetroScrollerInk)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        coroutineScope.launch {
                            onPositionChanged(offset.y / size.height)
                        }
                    },
                    onDrag = { change, _ ->
                        coroutineScope.launch {
                            onPositionChanged(change.position.y / size.height)
                        }
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopCenter)
                .padding(vertical = 3.dp)
                .width(4.dp)
                .fillMaxHeight()
                .background(RetroScrollerInk.copy(alpha = 0.3f)),
        )
        val thumbHeight = (maxHeight * visibleFraction.coerceIn(0f, 1f)).coerceIn(48.dp, maxHeight)
        Box(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopEnd)
                .padding(vertical = 2.dp, horizontal = 1.dp)
                .width(14.dp)
                .height(thumbHeight)
                .offset(y = (maxHeight - thumbHeight).coerceAtLeast(0.dp) * position)
                .background(RetroScrollerThumb)
                .border(2.dp, RetroScrollerInk),
        ) {
            Column(
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(2.dp)
                            .background(RetroScrollerInk),
                    )
                }
            }
        }
    }
}
