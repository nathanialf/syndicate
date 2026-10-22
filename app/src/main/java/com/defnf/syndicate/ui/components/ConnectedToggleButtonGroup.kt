package com.defnf.syndicate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One option of a [ConnectedToggleButtonGroup]: its value, label and optional icon. */
data class ToggleOption<T>(val value: T, val label: String, val icon: ImageVector? = null)

/**
 * A Material 3 Expressive connected button group choosing exactly one of [options]. Each button
 * fills an equal share of the width and is announced as a radio button. [uncheckedContainerColor]
 * lifts unchecked buttons off a tinted container, such as a settings item.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConnectedToggleButtonGroup(
    options: List<ToggleOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    uncheckedContainerColor: Color = Color.Unspecified
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = selected == option.value,
                onCheckedChange = { onSelect(option.value) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = if (uncheckedContainerColor.isSpecified) {
                    ToggleButtonDefaults.colors(containerColor = uncheckedContainerColor)
                } else {
                    ToggleButtonDefaults.colors()
                },
                // Narrower than the default, so icon and label fit three to a row on a small phone
                contentPadding = if (option.icon != null) {
                    IconButtonContentPadding
                } else {
                    ToggleButtonDefaults.contentPaddingFor(ToggleButtonDefaults.size)
                },
                icon = option.icon?.let { icon ->
                    { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize)) }
                }
            ) {
                Text(text = option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private val IconButtonContentPadding = PaddingValues(horizontal = 8.dp)
