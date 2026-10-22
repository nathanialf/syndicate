package com.defnf.syndicate.ui.components

import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.defnf.syndicate.data.models.Group

/** A group row with the same gestures as a feed row: swipe to delete or toggle notifications, long press for options. */
@Composable
fun SwipeableGroupItem(
    group: Group,
    isSelected: Boolean,
    shapes: ListItemShapes,
    swipeBackgroundShape: Shape,
    onGroupClick: (Long) -> Unit,
    onGroupLongClick: (Long) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onNotificationToggle: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeActionBox(
        startAction = deleteSwipeAction { onDeleteGroup(group.id) },
        endAction = notificationsSwipeAction(group.notificationsEnabled) { onNotificationToggle(group.id) },
        backgroundShape = swipeBackgroundShape,
        modifier = modifier
    ) { actionsModifier ->
        GroupItem(
            group = group,
            isSelected = isSelected,
            shapes = shapes,
            onGroupClick = onGroupClick,
            onGroupLongClick = onGroupLongClick,
            modifier = actionsModifier
        )
    }
}
