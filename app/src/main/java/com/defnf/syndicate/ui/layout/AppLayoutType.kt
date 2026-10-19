package com.defnf.syndicate.ui.layout

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass
import com.defnf.syndicate.ui.navigation.TopLevelDestination

/**
 * Top-level layout for the current window, chosen from its Material 3 window size class so it
 * adapts to rotation, foldables, split-screen and freeform windows.
 *
 * Each layout is described by what it shows rather than by its name; the rest of the UI only
 * reads these properties.
 */
enum class AppLayoutType(
    /** How the top-level destinations are reached. [NavigationSuiteType.None] when there are none. */
    val navigationSuiteType: NavigationSuiteType,
    /** Whether the feeds list is always visible in a sidebar instead of being a top-level destination. */
    val hasFeedsSidebar: Boolean,
    /** Whether the article list stays visible next to the article being read. */
    val hasArticleListBesideDetail: Boolean
) {
    /** Compact width (< 600dp): single pane with a bottom navigation bar. */
    BOTTOM_NAVIGATION(
        navigationSuiteType = NavigationSuiteType.NavigationBar,
        hasFeedsSidebar = false,
        hasArticleListBesideDetail = false
    ),

    /** Medium width (600dp - 839dp): single pane with a navigation rail. */
    NAVIGATION_RAIL(
        navigationSuiteType = NavigationSuiteType.NavigationRail,
        hasFeedsSidebar = false,
        hasArticleListBesideDetail = false
    ),

    /** Expanded width (840dp - 1199dp): feeds sidebar + article content pane. */
    TWO_PANE(
        navigationSuiteType = NavigationSuiteType.None,
        hasFeedsSidebar = true,
        hasArticleListBesideDetail = false
    ),

    /** Large width (>= 1200dp): feeds sidebar + article list + article detail. */
    THREE_PANE(
        navigationSuiteType = NavigationSuiteType.None,
        hasFeedsSidebar = true,
        hasArticleListBesideDetail = true
    );

    /** Whether top-level destinations are reachable from a navigation bar or rail. */
    val hasNavigationChrome: Boolean
        get() = navigationSuiteType != NavigationSuiteType.None

    /** Whether the navigation bar slides away while an article is read, giving it the full height. */
    val hidesNavigationWhileReading: Boolean
        get() = navigationSuiteType == NavigationSuiteType.NavigationBar

    /** What fills the space beside the navigation chrome and the feeds sidebar for [destination]. */
    fun mainContentFor(destination: TopLevelDestination): MainContent = when (destination) {
        TopLevelDestination.ARTICLES -> MainContent.ARTICLES
        // The sidebar already shows the feeds (e.g. the window was widened on the feeds tab)
        TopLevelDestination.FEEDS -> if (hasFeedsSidebar) MainContent.ARTICLES else MainContent.FEEDS
        TopLevelDestination.SETTINGS -> MainContent.SETTINGS
    }

    companion object {
        /**
         * Lower bound of the Material 3 "large" width class. [WindowSizeClass] only defines a
         * constant for it from window-core 1.5, and needs `supportLargeAndXLargeWidth` to report it.
         */
        const val WIDTH_DP_LARGE_LOWER_BOUND = 1200

        fun fromWindowSizeClass(windowSizeClass: WindowSizeClass): AppLayoutType = when {
            windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_LARGE_LOWER_BOUND) -> THREE_PANE
            windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> TWO_PANE
            windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> NAVIGATION_RAIL
            else -> BOTTOM_NAVIGATION
        }
    }
}

/** What fills the space beside the navigation chrome and the feeds sidebar. */
enum class MainContent {
    FEEDS,

    /** The article list and the article being read, laid out by the list-detail pane scaffold. */
    ARTICLES,
    SETTINGS
}
