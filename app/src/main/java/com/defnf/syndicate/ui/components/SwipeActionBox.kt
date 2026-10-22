package com.defnf.syndicate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.ui.common.LayoutConstants
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** An action revealed by swiping an item. [containerColor] fills the background once the swipe will trigger it. */
data class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val onSwipe: () -> Unit
)

/**
 * Swipe [content] sideways to run [startAction] (swiping from the start edge towards the end) or
 * [endAction] (swiping from the end edge towards the start). The item never leaves the list: it
 * follows the finger, resists once the action is armed, then springs back while the action runs.
 *
 * Crossing the threshold arms the action (haptic tick, background takes the action's color, icon
 * grows); dragging back below it disarms it, so a swipe can always be cancelled before release.
 * Both actions are also exposed as accessibility actions, since swiping isn't available to everyone:
 * [content] must apply the modifier it is given to its clickable element, the node accessibility
 * services focus, so the actions are offered there. The action's background is clipped to
 * [backgroundShape], usually the item's own shape.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SwipeActionBox(
    modifier: Modifier = Modifier,
    startAction: SwipeAction? = null,
    endAction: SwipeAction? = null,
    backgroundShape: Shape = RectangleShape,
    content: @Composable (actionsModifier: Modifier) -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val scope = rememberCoroutineScope()
    val currentStartAction by rememberUpdatedState(startAction)
    val currentEndAction by rememberUpdatedState(endAction)

    // Offset along the layout direction: positive towards the end edge, whatever the language
    var offset by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var width by remember { mutableIntStateOf(0) }
    val maxThreshold by rememberUpdatedState(with(density) { MaxSwipeThreshold.toPx() })
    // Derived from state rather than keyed on it, so the haptic below follows the measured width
    val threshold by remember {
        derivedStateOf { (width * SwipeThresholdFraction).coerceAtMost(maxThreshold) }
    }
    val flingVelocity = with(density) { FlingVelocity.toPx() }

    val activeAction by remember {
        derivedStateOf {
            when {
                offset > 0f -> currentStartAction
                offset < 0f -> currentEndAction
                else -> null
            }
        }
    }
    val isArmed by remember {
        derivedStateOf { threshold > 0f && abs(offset) >= threshold }
    }

    LaunchedEffect(Unit) {
        // Only dragging can arm an action; springing back after release only disarms it
        snapshotFlow { isArmed }
            .filter { it }
            .collect { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) }
    }

    val springBack = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val dragState = rememberDraggableState { delta ->
        offset = swipeDragOffset(
            offset = offset,
            delta = delta,
            threshold = threshold,
            maxOffset = width.toFloat(),
            canSwipeToEnd = currentStartAction != null,
            canSwipeToStart = currentEndAction != null
        )
    }

    // On the content's own (merged) node: a wrapper node without a label is never focused by TalkBack
    val actionsModifier = Modifier.semantics {
        customActions = listOfNotNull(startAction, endAction).distinct().map { action ->
            CustomAccessibilityAction(action.label) {
                action.onSwipe()
                true
            }
        }
    }

    Box(modifier = modifier.onSizeChanged { width = it.width }) {
        if (offset != 0f) {
            activeAction?.let { action ->
                SwipeActionBackground(
                    action = action,
                    isArmed = isArmed,
                    progress = { if (threshold > 0f) (abs(offset) / threshold).coerceIn(0f, 1f) else 0f },
                    alignToStart = offset > 0f,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(backgroundShape)
                )
            }
        }

        Box(
            modifier = Modifier
                // Offset mirrors in right-to-left layouts, matching the reversed drag direction
                .offset { IntOffset(offset.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    reverseDirection = isRtl,
                    onDragStarted = { settleJob?.cancel() },
                    // The velocity is already reversed for right-to-left layouts, like the deltas
                    onDragStopped = { velocity ->
                        val action = when (swipeReleaseResult(offset, velocity, threshold, flingVelocity)) {
                            SwipeRelease.ToEnd -> currentStartAction
                            SwipeRelease.ToStart -> currentEndAction
                            SwipeRelease.Cancel -> null
                        }
                        if (action != null) {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            action.onSwipe()
                        }
                        val swipedTowardsEnd = offset > 0f
                        settleJob = scope.launch {
                            animate(offset, 0f, animationSpec = springBack) { value, _ ->
                                // Stop at rest rather than overshoot and flash the other side's action
                                offset = if (swipedTowardsEnd) value.coerceAtLeast(0f) else value.coerceAtMost(0f)
                            }
                        }
                    }
                )
        ) {
            content(actionsModifier)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwipeActionBackground(
    action: SwipeAction,
    isArmed: Boolean,
    progress: () -> Float,
    alignToStart: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isArmed) action.containerColor else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "swipe_action_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isArmed) action.contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "swipe_action_content"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isArmed) 1.2f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "swipe_action_icon_scale"
    )

    Box(
        modifier = modifier
            .background(containerColor)
            .padding(horizontal = 24.dp),
        contentAlignment = if (alignToStart) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            // Fades in as the item is dragged, fully visible once the action is armed
            modifier = Modifier.graphicsLayer { alpha = 0.4f + 0.6f * progress() }
        ) {
            if (alignToStart) {
                SwipeActionIcon(action, contentColor, iconScale)
                Text(text = action.label, style = MaterialTheme.typography.labelLarge, color = contentColor)
            } else {
                Text(text = action.label, style = MaterialTheme.typography.labelLarge, color = contentColor)
                SwipeActionIcon(action, contentColor, iconScale)
            }
        }
    }
}

@Composable
private fun SwipeActionIcon(action: SwipeAction, tint: Color, scale: Float) {
    Icon(
        imageVector = action.icon,
        // The label next to the icon already describes the action
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(LayoutConstants.SwipeIconSize)
            .scale(scale)
    )
}

/** What releasing a swipe does: run the action towards the end or the start, or cancel. */
internal enum class SwipeRelease { ToEnd, ToStart, Cancel }

/**
 * The new offset after dragging [delta] from [offset]. Directions without an action don't move,
 * and beyond [threshold] the item only follows part of the movement so it feels armed.
 */
internal fun swipeDragOffset(
    offset: Float,
    delta: Float,
    threshold: Float,
    maxOffset: Float,
    canSwipeToEnd: Boolean,
    canSwipeToStart: Boolean
): Float {
    val proposed = offset + delta
    val allowed = when {
        proposed > 0f && !canSwipeToEnd -> 0f
        proposed < 0f && !canSwipeToStart -> 0f
        // Only when moving further out on the same side, not when the drag crosses back over the start
        abs(offset) >= threshold && proposed * offset > 0f && abs(proposed) > abs(offset) ->
            offset + delta * OverDragResistance
        else -> proposed
    }
    return allowed.coerceIn(-maxOffset, maxOffset)
}

/**
 * Decides what a swipe released at [offset] with [velocity] (both positive towards the end) does:
 * past [threshold] it runs the action, and so does a fast fling in the same direction from part way.
 * A fast fling back the other way always cancels.
 */
internal fun swipeReleaseResult(offset: Float, velocity: Float, threshold: Float, flingVelocity: Float): SwipeRelease {
    if (offset == 0f || threshold <= 0f) return SwipeRelease.Cancel
    val isFling = abs(velocity) >= flingVelocity
    val flungBack = isFling && velocity * offset < 0f
    val armed = abs(offset) >= threshold
    val flung = isFling && !flungBack && abs(offset) >= threshold * FlingThresholdFraction
    return when {
        flungBack || !(armed || flung) -> SwipeRelease.Cancel
        offset > 0f -> SwipeRelease.ToEnd
        else -> SwipeRelease.ToStart
    }
}

// Fraction of the item's width to drag before the action is armed, capped for wide items
private const val SwipeThresholdFraction = 0.35f
private val MaxSwipeThreshold = 160.dp

// How much of the finger's movement the item follows once the action is armed
internal const val OverDragResistance = 0.3f

// A quick fling triggers the action from part way to the threshold
private val FlingVelocity = 1200.dp
internal const val FlingThresholdFraction = 0.6f
