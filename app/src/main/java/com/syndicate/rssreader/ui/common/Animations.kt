package com.syndicate.rssreader.ui.common

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * Reusable animation utilities for consistent transitions across the app
 */
object Animations {
    
    /**
     * Slide-over animation for article detail views
     */
    fun slideOverTransition(
        targetState: String,
        initialState: String
    ): ContentTransform {
        return when {
            targetState == "article_detail" -> {
                // Slide in from right when opening article
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth }
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth }
                )
            }
            initialState == "article_detail" -> {
                // Slide in from left when going back from article
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth }
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth }
                )
            }
            else -> {
                // Simple fade for other transitions
                fadeIn() togetherWith fadeOut()
            }
        }
    }
    
    /**
     * Content state transition for article navigation
     */
    fun contentTransition(
        targetState: String,
        initialState: String
    ): ContentTransform {
        return when {
            targetState == "article_detail" -> {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth }
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth }
                )
            }
            initialState == "article_detail" -> {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth }
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth }
                )
            }
            targetState == "settings" -> {
                fadeIn() togetherWith fadeOut()
            }
            initialState == "settings" -> {
                fadeIn() togetherWith fadeOut()
            }
            else -> {
                fadeIn() togetherWith fadeOut()
            }
        }
    }
}