package com.defnf.syndicate.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

@Composable
fun LazyListScrollHandler(
    listState: LazyListState,
    onScrollDirectionChanged: (Boolean) -> Unit,
    threshold: Int = 100
) {
    var previousScrollOffset by remember { mutableStateOf(0) }
    
    LaunchedEffect(listState) {
        snapshotFlow { 
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset 
        }.collect { (index, offset) ->
            if (listState.isScrollInProgress) {
                val currentPosition = index * 1000 + offset
                val isScrollingDown = currentPosition > previousScrollOffset
                
                if (currentPosition > threshold) {
                    onScrollDirectionChanged(isScrollingDown)
                }
                previousScrollOffset = currentPosition
            }
        }
    }
}