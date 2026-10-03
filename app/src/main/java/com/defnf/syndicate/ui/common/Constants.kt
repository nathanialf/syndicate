package com.defnf.syndicate.ui.common

import androidx.compose.ui.unit.dp

object LayoutConstants {
    // Padding constants
    val StandardPadding = 16.dp
    val FabBottomPadding = 24.dp
    val FabEndPadding = 16.dp
    val ContentBottomPaddingForFab = 88.dp
    
    // Top bar height
    val TopBarHeight = 64.dp
    val TopBarWithPadding = TopBarHeight + StandardPadding // 80dp
    
    // Content top padding - matches what Scaffold.calculateTopPadding() provides
    val ContentTopPadding = TopBarHeight // 64dp - space for top bar only
    
    // Window width breakpoints (Material 3 window size classes)
    val MediumWidthBreakpoint = 600.dp
    val ExpandedWidthBreakpoint = 840.dp
    val LargeWidthBreakpoint = 1200.dp
    
    // Pane widths for multi-pane layouts
    val SidebarWidth = 320.dp
    val ArticleListPaneWidth = 400.dp
    
    // Icon sizes
    val SwipeIconSize = 32.dp
}