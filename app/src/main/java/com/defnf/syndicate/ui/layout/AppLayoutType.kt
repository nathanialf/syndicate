package com.defnf.syndicate.ui.layout

import androidx.compose.ui.unit.Dp
import com.defnf.syndicate.ui.common.LayoutConstants

/** How the top-level destinations are reached in a layout. */
enum class NavigationChrome {
    /** Navigation bar along the bottom edge. */
    BOTTOM_BAR,

    /** Navigation rail along the start edge. */
    RAIL,

    /** No top-level navigation: feeds stay visible in the sidebar and settings open from the article list. */
    NONE
}

/**
 * Top-level layout used for the current window size, following the Material 3 window size classes.
 * Derived from the actual window width so it adapts to rotation, foldables, split-screen and freeform windows.
 *
 * Each layout is described by what it shows rather than by its name: the panes ([resolvePanes]),
 * the navigation chrome and back navigation are all derived from these properties.
 */
enum class AppLayoutType(
    val navigationChrome: NavigationChrome,
    /** Whether the feeds list is always visible in a sidebar instead of being a top-level destination. */
    val hasFeedsSidebar: Boolean,
    /** Whether the article list stays visible next to the article being read. */
    val hasArticleListBesideDetail: Boolean
) {
    /** Compact width (< 600dp): single pane with a bottom navigation bar. */
    BOTTOM_NAVIGATION(
        navigationChrome = NavigationChrome.BOTTOM_BAR,
        hasFeedsSidebar = false,
        hasArticleListBesideDetail = false
    ),

    /** Medium width (600dp - 839dp): single pane with a navigation rail. */
    NAVIGATION_RAIL(
        navigationChrome = NavigationChrome.RAIL,
        hasFeedsSidebar = false,
        hasArticleListBesideDetail = false
    ),

    /** Expanded width (840dp - 1199dp): feeds sidebar + article content pane. */
    TWO_PANE(
        navigationChrome = NavigationChrome.NONE,
        hasFeedsSidebar = true,
        hasArticleListBesideDetail = false
    ),

    /** Large width (>= 1200dp): feeds sidebar + article list + article detail. */
    THREE_PANE(
        navigationChrome = NavigationChrome.NONE,
        hasFeedsSidebar = true,
        hasArticleListBesideDetail = true
    );

    /** Whether top-level destinations are reachable from a navigation bar or rail. */
    val hasNavigationChrome: Boolean
        get() = navigationChrome != NavigationChrome.NONE

    companion object {
        fun fromWindowWidth(width: Dp): AppLayoutType = when {
            width >= LayoutConstants.LargeWidthBreakpoint -> THREE_PANE
            width >= LayoutConstants.ExpandedWidthBreakpoint -> TWO_PANE
            width >= LayoutConstants.MediumWidthBreakpoint -> NAVIGATION_RAIL
            else -> BOTTOM_NAVIGATION
        }
    }
}
