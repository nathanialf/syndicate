package com.defnf.syndicate.ui.common

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner sizes for lists drawn as one connected group of items (Material 3 Expressive "segmented"
 * lists): the outer corners of the group are large, the corners between items are tight and the
 * items are separated by a small gap.
 */
object SegmentedShapes {
    val OuterCorner = 24.dp
    val InnerCorner = 4.dp
    val ItemGap = 2.dp

    /** Top corner size of item [index] in a group; a selected item is rounded all around. */
    fun topCorner(index: Int, selected: Boolean = false): Dp = if (selected || index == 0) OuterCorner else InnerCorner

    /** Bottom corner size of item [index] of [count] items; a selected item is rounded all around. */
    fun bottomCorner(index: Int, count: Int, selected: Boolean = false): Dp =
        if (selected || index == count - 1) OuterCorner else InnerCorner
}
