package com.syndicate.rssreader.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.syndicate.rssreader.data.models.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableGroupItem(
    group: Group,
    onGroupClick: (Long) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onEditGroup: (Long) -> Unit,
    isSidebarMode: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Delete action (swipe right) - don't dismiss yet, wait for confirmation
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteGroup(group.id)
                    false // Don't dismiss, wait for dialog result
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Edit action (swipe left) - don't dismiss yet, wait for action completion
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEditGroup(group.id)
                    false // Don't dismiss, wait for edit dialog result
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
    )
    
    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            SwipeBackground(
                swipeDirection = swipeState.dismissDirection,
                isGroupMode = true
            )
        },
        modifier = modifier
    ) {
        GroupItem(
            group = group,
            onGroupClick = onGroupClick,
            isSidebarMode = isSidebarMode,
            isSelected = isSelected
        )
    }
}