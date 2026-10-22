package com.defnf.syndicate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Colors of screens built from grouped lists, drawn like the Android Settings app: the items are
 * bright tiles (see [SegmentedItem]) on a tinted page, and a selected item takes the secondary
 * container color.
 */
object GroupedListDefaults {
    val containerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer

    val itemColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceBright

    val selectedItemColor: Color
        @Composable get() = MaterialTheme.colorScheme.secondaryContainer

    @Composable
    fun itemColor(selected: Boolean): Color = if (selected) selectedItemColor else itemColor
}

/** The title above a group of items, lined up with the content of the items. */
@Composable
fun ListSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

/** An item's leading icon set in a tonal [shape], like the icons of the Android Settings app. */
@Composable
fun IconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    size: Dp = 40.dp
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
            modifier = Modifier.size(size / 2)
        )
    }
}
