package com.peterlaurence.trekme.features.common.presentation.ui.scrollbar


import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun Modifier.drawHorizontalScrollbar(
    state: ScrollState,
    reverseScrolling: Boolean = false
): Modifier = drawScrollbar(state, Orientation.Horizontal, reverseScrolling)

fun Modifier.drawVerticalScrollbar(
    state: ScrollState,
    reverseScrolling: Boolean = false
): Modifier = drawScrollbar(state, Orientation.Vertical, reverseScrolling)

fun Modifier.drawHorizontalScrollbar(
    state: LazyListState,
    reverseScrolling: Boolean = false
): Modifier = drawScrollbar(state, Orientation.Horizontal, reverseScrolling)

fun Modifier.drawVerticalScrollbar(
    state: LazyListState,
    reverseScrolling: Boolean = false
): Modifier = drawScrollbar(state, Orientation.Vertical, reverseScrolling)

private fun Modifier.drawScrollbar(
    state: ScrollState,
    orientation: Orientation,
    reverseScrolling: Boolean
): Modifier = drawScrollbar(
    orientation, reverseScrolling
) { reverseDirection, atEnd, thickness, color, alpha ->
    val showScrollbar = state.maxValue > 0
    val canvasSize = if (orientation == Orientation.Horizontal) size.width else size.height
    val totalSize = canvasSize + state.maxValue
    val thumbSize = canvasSize / totalSize * canvasSize
    val startOffset = state.value / totalSize * canvasSize
    val drawScrollbar = onDrawScrollbar(
        orientation, reverseDirection, atEnd, showScrollbar,
        thickness, color, alpha, thumbSize, startOffset
    )
    onDrawWithContent {
        drawContent()
        drawScrollbar()
    }
}.drawBehind {
    // drawWithCache + scrollState is broken in compose 1.3.0+ - let's trigger a redraw when the scroll state changes
    state.value
    state.maxValue
}

private fun Modifier.drawScrollbar(
    state: LazyListState,
    orientation: Orientation,
    reverseScrolling: Boolean
): Modifier = drawScrollbar(
    orientation, reverseScrolling
) { reverseDirection, atEnd, thickness, color, alpha ->
    val layoutInfo = state.layoutInfo
    val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val items = layoutInfo.visibleItemsInfo
    val itemsSize = items.fastSumBy { it.size }
    val showScrollbar = items.size < layoutInfo.totalItemsCount || itemsSize > viewportSize
    val estimatedItemSize = if (items.isEmpty()) 0f else itemsSize.toFloat() / items.size
    val totalSize = estimatedItemSize * layoutInfo.totalItemsCount
    val canvasSize = if (orientation == Orientation.Horizontal) size.width else size.height
    val thumbSize = viewportSize / totalSize * canvasSize
    val firstItem = items.firstOrNull()
    val startOffset = if (firstItem == null) 0f else {
        with(firstItem) {
            (estimatedItemSize * index - offset) / totalSize * canvasSize
        }
    }
    val drawScrollbar = onDrawScrollbar(
        orientation, reverseDirection, atEnd, showScrollbar,
        thickness, color, alpha, thumbSize, startOffset
    )
    onDrawWithContent {
        drawContent()
        drawScrollbar()
    }
}.drawBehind {
    // drawWithCache + lazyListState is broken in compose 1.3.0+ - let's trigger a redraw when the scroll state changes
    state.firstVisibleItemIndex
    state.firstVisibleItemScrollOffset
    state.layoutInfo.totalItemsCount
    state.layoutInfo.viewportStartOffset
    state.layoutInfo.viewportEndOffset
}

private fun CacheDrawScope.onDrawScrollbar(
    orientation: Orientation,
    reverseDirection: Boolean,
    atEnd: Boolean,
    showScrollbar: Boolean,
    thickness: Float,
    color: Color,
    alpha: () -> Float,
    thumbSize: Float,
    startOffset: Float
): DrawScope.() -> Unit {
    val topLeft = if (orientation == Orientation.Horizontal) {
        Offset(
            if (reverseDirection) size.width - startOffset - thumbSize else startOffset,
            if (atEnd) size.height - thickness else 0f
        )
    } else {
        Offset(
            if (atEnd) size.width - thickness else 0f,
            if (reverseDirection) size.height - startOffset - thumbSize else startOffset
        )
    }
    val size = if (orientation == Orientation.Horizontal) {
        Size(thumbSize, thickness)
    } else {
        Size(thickness, thumbSize)
    }

    return {
        if (showScrollbar) {
            drawRect(
                color = color,
                topLeft = topLeft,
                size = size,
                alpha = alpha()
            )
        }
    }
}

private fun Modifier.drawScrollbar(
    orientation: Orientation,
    reverseScrolling: Boolean,
    onBuildDrawCache: CacheDrawScope.(
        reverseDirection: Boolean,
        atEnd: Boolean,
        thickness: Float,
        color: Color,
        alpha: () -> Float
    ) -> DrawResult
): Modifier = composed {
    val scrolled = remember {
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    val nestedScrollConnection = remember(orientation, scrolled) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = if (orientation == Orientation.Horizontal) consumed.x else consumed.y
                if (delta != 0f) scrolled.tryEmit(Unit)
                return Offset.Zero
            }
        }
    }

    val alpha = remember { Animatable(0f) }
    var initialFadeOutJob: Job? = null
    LaunchedEffect(scrolled, alpha) {
        scrolled.collectLatest {
            initialFadeOutJob?.cancel()
            alpha.snapTo(1f)
            delay(ViewConfiguration.getScrollDefaultDelay().toLong())
            alpha.animateTo(0f, animationSpec = FadeOutAnimationSpec)
        }
    }

    LaunchedEffect(true) {
        initialFadeOutJob = launch {
            alpha.snapTo(1f)
            delay(2000)
            scrolled.tryEmit(Unit)
        }
    }

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val reverseDirection = if (orientation == Orientation.Horizontal) {
        if (isLtr) reverseScrolling else !reverseScrolling
    } else reverseScrolling
    val atEnd = if (orientation == Orientation.Vertical) isLtr else true

    // Calculate thickness here to workaround https://issuetracker.google.com/issues/206972664
    val thickness = with(LocalDensity.current) { Thickness.toPx() }
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Modifier
        .nestedScroll(nestedScrollConnection)
        .drawWithCache {
            onBuildDrawCache(reverseDirection, atEnd, thickness, color, alpha::value)
        }
}

private val Thickness = 5.dp
private val FadeOutAnimationSpec =
    tween<Float>(durationMillis = ViewConfiguration.getScrollBarFadeDuration())

@Preview(widthDp = 400, heightDp = 400, showBackground = true)
@Composable
private fun ScrollbarPreview() {
    val state = rememberScrollState()
    Column(
        modifier = Modifier
            .drawVerticalScrollbar(state)
            .verticalScroll(state),
    ) {
        repeat(50) {
            Text(
                text = "Item ${it + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Preview(widthDp = 400, heightDp = 400, showBackground = true)
@Composable
private fun LazyListScrollbarPreview() {
    val state = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.drawVerticalScrollbar(state),
        state = state
    ) {
        items(50) {
            Text(
                text = "Item ${it + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Preview(widthDp = 400, showBackground = true)
@Composable
private fun HorizontalScrollbarPreview() {
    val state = rememberScrollState()
    Row(
        modifier = Modifier
            .drawHorizontalScrollbar(state)
            .horizontalScroll(state)
    ) {
        repeat(50) {
            Text(
                text = (it + 1).toString(),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 16.dp)
            )
        }
    }
}

@Preview(widthDp = 400, showBackground = true)
@Composable
private fun LazyListHorizontalScrollbarPreview() {
    val state = rememberLazyListState()
    LazyRow(
        modifier = Modifier.drawHorizontalScrollbar(state),
        state = state
    ) {
        items(50) {
            Text(
                text = (it + 1).toString(),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 16.dp)
            )
        }
    }
}