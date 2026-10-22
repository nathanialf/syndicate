package com.defnf.syndicate.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.defnf.syndicate.ui.theme.SyndicateTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The roles grouped rows announce, overriding the ones SegmentedListItem sets inside. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class GroupedListSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun `a switch row is announced as a switch and keeps its state`() {
        composeRule.setContent {
            SyndicateTheme(darkTheme = false) {
                SegmentedListItem(
                    checked = true,
                    onCheckedChange = {},
                    shapes = GroupedListDefaults.toggleShapes(0, 1),
                    modifier = Modifier.switchRole()
                ) { Text("Notifications") }
            }
        }
        composeRule.onNodeWithText("Notifications").assert(hasRole(Role.Switch)).assertIsOn()
    }

    @Test
    fun `a selected feed row is announced as a selected button`() {
        composeRule.setContent {
            SyndicateTheme(darkTheme = false) {
                SegmentedListItem(
                    selected = true,
                    onClick = {},
                    shapes = GroupedListDefaults.shapes(0, 1),
                    modifier = Modifier.navigationItemRole()
                ) { Text("Circuit Notes") }
            }
        }
        composeRule.onNodeWithText("Circuit Notes").assert(hasRole(Role.Button)).assertIsSelected()
    }
}
