package com.defnf.syndicate.ui.layout

import com.defnf.syndicate.ui.navigation.TopLevelDestination

/** Where a pane sits in the layout. Panes are laid out from start to end in this order. */
enum class PaneRole {
    /** Fixed-width feeds list along the start edge. */
    SIDEBAR,

    /** Fixed-width article list next to the article being read. */
    LIST,

    /** Fills the remaining width. Always present and always last. */
    MAIN
}

/** What a pane displays. */
sealed interface PaneContent {
    data object Feeds : PaneContent

    data object ArticleList : PaneContent

    data class ArticleDetail(val articleId: String) : PaneContent

    /** Placeholder for the detail pane while the article list is shown beside it and nothing is open. */
    data object NoArticleSelected : PaneContent

    data object Settings : PaneContent
}

data class Pane(val role: PaneRole, val content: PaneContent)

/**
 * Resolves the panes to show, from start to end, for [layoutType] and the current navigation.
 *
 * This is the only place that decides what each layout displays; the composables just render the
 * result. A pane keeps its [PaneRole] while its content changes, so changes are animated in place.
 */
fun resolvePanes(
    layoutType: AppLayoutType,
    destination: TopLevelDestination,
    openArticleId: String?
): List<Pane> {
    val mainContent = when {
        destination == TopLevelDestination.SETTINGS -> PaneContent.Settings
        // Without a sidebar the feeds list is a destination of its own
        destination == TopLevelDestination.FEEDS && !layoutType.hasFeedsSidebar -> PaneContent.Feeds
        openArticleId != null -> PaneContent.ArticleDetail(openArticleId)
        layoutType.hasArticleListBesideDetail -> PaneContent.NoArticleSelected
        else -> PaneContent.ArticleList
    }

    return buildList {
        if (layoutType.hasFeedsSidebar) {
            add(Pane(PaneRole.SIDEBAR, PaneContent.Feeds))
        }
        // Settings take the space of both the list and the detail pane
        if (layoutType.hasArticleListBesideDetail && mainContent != PaneContent.Settings) {
            add(Pane(PaneRole.LIST, PaneContent.ArticleList))
        }
        add(Pane(PaneRole.MAIN, mainContent))
    }
}
