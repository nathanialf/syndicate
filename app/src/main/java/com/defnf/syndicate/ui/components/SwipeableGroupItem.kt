package com.defnf.syndicate.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.defnf.syndicate.data.models.Group

/** A group row with the same gestures as a feed row: swipe to delete or toggle notifications, long press for options. */
@Composable
fun SwipeableGroupItem(
    group: Group,
    onGroupClick: (Long) -> Unit,
    onGroupLongClick: (Long) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onNotificationToggle: (Long) -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    SwipeActionBox(
        startAction = deleteSwipeAction { onDeleteGroup(group.id) },
        endAction = notificationsSwipeAction(group.notificationsEnabled) { onNotificationToggle(group.id) },
        modifier = modifier
    ) { actionsModifier ->
        GroupItem(
            group = group,
            onGroupClick = onGroupClick,
            onGroupLongClick = onGroupLongClick,
            modifier = actionsModifier,
            isSelected = isSelected
        )
    }
}
