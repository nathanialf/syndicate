package com.defnf.syndicate.ui.navigation

import androidx.compose.ui.unit.Dp
import com.defnf.syndicate.ui.common.LayoutConstants

/**
 * Top-level layout used for the current window size, following the Material 3 window size classes.
 * Derived from the actual window width so it adapts to rotation, foldables, split-screen and freeform windows.
 */
enum class AppLayoutType {
    /** Compact width (< 600dp): single pane with a bottom navigation bar. */
    BOTTOM_NAVIGATION,

    /** Medium width (600dp - 839dp): single pane with a navigation rail. */
    NAVIGATION_RAIL,

    /** Expanded width (840dp - 1199dp): feeds sidebar + article content pane. */
    TWO_PANE,

    /** Large width (>= 1200dp): feeds sidebar + article list + article detail. */
    THREE_PANE;

    val isMultiPane: Boolean
        get() = this == TWO_PANE || this == THREE_PANE

    companion object {
        fun fromWindowWidth(width: Dp): AppLayoutType = when {
            width >= LayoutConstants.LargeWidthBreakpoint -> THREE_PANE
            width >= LayoutConstants.ExpandedWidthBreakpoint -> TWO_PANE
            width >= LayoutConstants.MediumWidthBreakpoint -> NAVIGATION_RAIL
            else -> BOTTOM_NAVIGATION
        }
    }
}
