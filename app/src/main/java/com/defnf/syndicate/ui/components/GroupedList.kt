package com.defnf.syndicate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Grouped lists (feeds, settings, the option sheets) are built from Material's
 * [androidx.compose.material3.SegmentedListItem]: [ListItemDefaults.segmentedShapes] gives each
 * item large outer and tight inner corners, a selected item morphs to fully rounded, and items are
 * [ItemGap] apart. Items sit on [MaterialTheme.colorScheme.surfaceContainer], since the default
 * segmented container blends into the page.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object GroupedListDefaults {
    val ItemGap: Dp = ListItemDefaults.SegmentedGap

    /** Colors of a selectable item: a selected one takes the secondary container. */
    @Composable
    fun colors(containerColor: Color = MaterialTheme.colorScheme.surfaceContainer): ListItemColors =
        ListItemDefaults.segmentedColors(containerColor = containerColor)

    /**
     * Colors of a toggleable item. It reports itself as selected while on, but its switch or
     * checkbox already shows that, so it keeps its normal colors.
     */
    @Composable
    fun toggleColors(containerColor: Color = MaterialTheme.colorScheme.surfaceContainer): ListItemColors {
        val colors = MaterialTheme.colorScheme
        return ListItemDefaults.segmentedColors(
            containerColor = containerColor,
            selectedContainerColor = containerColor,
            selectedContentColor = colors.onSurface,
            selectedLeadingContentColor = colors.onSurfaceVariant,
            selectedTrailingContentColor = colors.onSurfaceVariant,
            selectedOverlineContentColor = colors.onSurfaceVariant,
            selectedSupportingContentColor = colors.onSurfaceVariant
        )
    }

    @Composable
    fun shapes(index: Int, count: Int): ListItemShapes = ListItemDefaults.segmentedShapes(index = index, count = count)

    /**
     * Shapes of a toggleable item. Like [toggleColors], it keeps its shape while on rather than
     * morphing to the selected shape and breaking away from its group.
     */
    @Composable
    fun toggleShapes(index: Int, count: Int): ListItemShapes = shapes(index, count).let { it.copy(selectedShape = it.shape) }

    /** The shape item [index] of [count] has now, for what is drawn behind it, such as swipe actions. */
    @Composable
    fun currentShape(index: Int, count: Int, selected: Boolean): Shape =
        shapes(index, count).let { if (selected) it.selectedShape else it.shape }
}

/**
 * Announces a toggle row as a switch. The checked [androidx.compose.material3.SegmentedListItem]
 * says checkbox; an outer semantics modifier overrides the role it sets inside.
 */
fun Modifier.switchRole(): Modifier = semantics { role = Role.Switch }

/**
 * Announces a selectable row that opens something (a feed, a group) as a button that is selected,
 * rather than the radio button the selected [androidx.compose.material3.SegmentedListItem] says.
 */
fun Modifier.navigationItemRole(): Modifier = semantics { role = Role.Button }

/** Separates item [index] of a group from the one above it. */
fun Modifier.groupedItemGap(index: Int): Modifier = padding(top = if (index == 0) 0.dp else GroupedListDefaults.ItemGap)

/** The title above a group of items, lined up with the content of the items. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

/** An item's leading icon on a tonal [shape], by default the device's launcher icon shape. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = LauncherIconShape,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
