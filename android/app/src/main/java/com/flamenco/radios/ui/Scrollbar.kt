package com.flamenco.radios.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Géométrie de la liste, estimée à partir de la taille moyenne des éléments visibles. */
private data class ScrollMetrics(
    val contentPx: Float,
    val viewportPx: Float,
    val scrolledPx: Float,
    val avgItemPx: Float,
) {
    val scrollablePx get() = (contentPx - viewportPx).coerceAtLeast(1f)
    val fraction get() = (scrolledPx / scrollablePx).coerceIn(0f, 1f)
}

/**
 * Ascenseur vertical pour une [androidx.compose.foundation.lazy.LazyColumn] :
 * le curseur se fait glisser, et un appui sur la piste saute à cet endroit.
 * Masqué quand tout le contenu tient à l'écran.
 */
@Composable
fun VerticalScrollbar(state: LazyListState, modifier: Modifier = Modifier) {
    val metrics by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty() || info.totalItemsCount == 0) return@derivedStateOf null
            val avg = visible.sumOf { it.size }.toFloat() / visible.size
            val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            val content = avg * info.totalItemsCount + info.beforeContentPadding + info.afterContentPadding
            if (content <= viewport) return@derivedStateOf null
            ScrollMetrics(
                contentPx = content,
                viewportPx = viewport,
                scrolledPx = state.firstVisibleItemIndex * avg + state.firstVisibleItemScrollOffset,
                avgItemPx = avg,
            )
        }
    }
    val m = metrics ?: return

    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val thumbWidth by animateDpAsState(if (dragging) 10.dp else 6.dp, label = "thumbWidth")
    val minThumbPx = with(LocalDensity.current) { 48.dp.toPx() }

    BoxWithConstraints(modifier.fillMaxHeight().width(28.dp)) {
        val trackPx = constraints.maxHeight.toFloat()
        val thumbPx = (trackPx * m.viewportPx / m.contentPx).coerceIn(minThumbPx, trackPx)
        val thumbTopPx = m.fraction * (trackPx - thumbPx)

        // Valeurs lues au moment du geste : les gestes ne sont pas relancés quand la liste défile.
        val geometry by rememberUpdatedState(Triple(trackPx, thumbPx, m))

        /** Fait défiler la liste pour que le curseur soit centré sur [y] (pixels de la piste). */
        fun scrollToTrackY(y: Float) {
            val (track, thumb, metrics) = geometry
            val fraction = ((y - thumb / 2) / (track - thumb).coerceAtLeast(1f)).coerceIn(0f, 1f)
            if (fraction >= 0.999f) {
                // Bas de la piste : aller réellement au dernier élément (l'estimation peut s'arrêter avant).
                scope.launch { state.scrollToItem(state.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1) }
                return
            }
            val targetPx = fraction * metrics.scrollablePx
            val index = (targetPx / metrics.avgItemPx).toInt()
            val offset = (targetPx - index * metrics.avgItemPx).roundToInt()
            scope.launch { state.scrollToItem(index, offset) }
        }

        Box(
            Modifier
                .fillMaxHeight()
                .width(28.dp)
                .pointerInput(state) {
                    detectTapGestures { scrollToTrackY(it.y) }
                }
                .pointerInput(state) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true; scrollToTrackY(it.y) },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ ->
                        change.consume()
                        scrollToTrackY(change.position.y)
                    }
                },
        ) {
            // Piste
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxHeight()
                    .width(thumbWidth)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(50)),
            )
            // Curseur
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, thumbTopPx.roundToInt()) }
                    .height(with(LocalDensity.current) { thumbPx.toDp() })
                    .width(thumbWidth)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (dragging) 1f else 0.7f),
                        RoundedCornerShape(50),
                    ),
            )
        }
    }
}
