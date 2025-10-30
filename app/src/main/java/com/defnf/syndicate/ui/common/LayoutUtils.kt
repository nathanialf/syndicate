package com.defnf.syndicate.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable layout utilities for consistent spacing and system bar handling
 */
object LayoutUtils {
    
    /**
     * Get the system bar top padding as Dp
     */
    @Composable
    fun getSystemBarTopPadding(): Dp {
        return with(LocalDensity.current) {
            WindowInsets.systemBars.getTop(this).toDp()
        }
    }
    
    /**
     * Create a spacer with system bar top padding
     */
    @Composable
    fun SystemBarTopSpacer() {
        Spacer(
            modifier = Modifier.padding(
                top = getSystemBarTopPadding()
            )
        )
    }
    
    /**
     * Create a modifier with system bar top padding
     */
    @Composable
    fun Modifier.systemBarTopPadding(): Modifier {
        return this.padding(top = getSystemBarTopPadding())
    }
}