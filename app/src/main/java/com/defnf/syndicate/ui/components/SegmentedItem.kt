package com.defnf.syndicate.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.ui.common.SegmentedShapes

/**
 * Item [index] of [count] items drawn as one connected group (see [SegmentedShapes]). A selected
 * item morphs to a fully rounded shape, animated with the theme's expressive motion.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SegmentedItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    color: Color = GroupedListDefaults.itemColor,
    content: @Composable () -> Unit
) {
    val cornerSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val topCorner by animateDpAsState(
        targetValue = SegmentedShapes.topCorner(index, selected),
        animationSpec = cornerSpec,
        label = "segment_top_corner"
    )
    val bottomCorner by animateDpAsState(
        targetValue = SegmentedShapes.bottomCorner(index, count, selected),
        animationSpec = cornerSpec,
        label = "segment_bottom_corner"
    )
    // The spring can overshoot slightly below the inner corner size, never below zero
    val top = topCorner.coerceAtLeast(0.dp)
    val bottom = bottomCorner.coerceAtLeast(0.dp)

    Surface(
        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (index == 0) 0.dp else SegmentedShapes.ItemGap)
    ) {
        content()
    }
}
