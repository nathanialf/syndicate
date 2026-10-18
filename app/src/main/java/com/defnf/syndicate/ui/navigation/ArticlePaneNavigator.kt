package com.defnf.syndicate.ui.navigation

/**
 * Opens and closes the article being read.
 *
 * The open article is owned by the list-detail pane scaffold's navigator rather than by
 * [NavigationState], so the scaffold can animate between the list and the article and drive
 * predictive back. [NavigationState] goes through this interface whenever it changes the article.
 */
interface ArticlePaneNavigator {
    /** The article currently open, or null while only the article list is shown. */
    val openArticleId: String?

    fun openArticle(articleId: String)

    fun closeArticle()
}
