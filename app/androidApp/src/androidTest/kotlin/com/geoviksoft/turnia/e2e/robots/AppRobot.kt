package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.geoviksoft.turnia.e2e.infra.awaitNoNode
import com.geoviksoft.turnia.e2e.infra.awaitNode

/**
 * What every screen shares: tabs, buttons, fields, dialogs. Nodes are found by their English text
 * where it is unique on screen, and by the tags in `TestTags` where it is not.
 */
internal open class AppRobot(protected val compose: ComposeTestRule) {

    fun openTab(label: String) = click(label)

    fun click(text: String) {
        compose.awaitNode(hasText(text) and hasClickAction()).performClick()
    }

    fun clickDescription(description: String) {
        compose.awaitNode(hasContentDescription(description) and hasClickAction()).performClick()
    }

    fun type(label: String, value: String) {
        compose.awaitNode(hasSetTextAction() and hasText(label)).performTextInput(value)
    }

    fun awaitText(text: String, substring: Boolean = false) {
        compose.awaitNode(hasText(text, substring = substring), useUnmergedTree = true)
    }

    fun awaitNoText(text: String) {
        compose.awaitNoNode(hasText(text), useUnmergedTree = true)
    }

    fun await(matcher: SemanticsMatcher) {
        compose.awaitNode(matcher)
    }

    /** Closes whatever is on top: a bottom sheet, a dialog, or the screen itself. */
    fun back() = Espresso.pressBack()

    /** The main screen is up once its tab bar is. */
    fun awaitMain() {
        compose.awaitNode(hasText(TAB_CALENDAR) and hasClickAction())
        compose.awaitNode(hasText(TAB_SETTINGS) and hasClickAction())
    }

    companion object {
        const val TAB_CALENDAR = "Calendar"
        const val TAB_PEOPLE = "People"
        const val TAB_GROUPS = "Groups"
        const val TAB_SWAPS = "Swaps"
        const val TAB_SETTINGS = "Settings"
    }
}
