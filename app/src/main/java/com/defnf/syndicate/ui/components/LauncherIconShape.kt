package com.defnf.syndicate.ui.components

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * The mask the system puts on adaptive launcher icons (circle, squircle, rounded square, ... depending
 * on the device and its icon shape setting), so feed and group icons match the home screen's.
 */
object LauncherIconShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val mask = AdaptiveIconDrawable(ColorDrawable(), ColorDrawable()).apply {
            setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
        }.iconMask
        return Outline.Generic(mask.asComposePath())
    }
}
