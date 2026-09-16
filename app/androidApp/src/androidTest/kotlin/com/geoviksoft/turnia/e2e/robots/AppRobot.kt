package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.geoviksoft.turnia.e2e.infra.awaitAnyNode
import com.geoviksoft.turnia.e2e.infra.awaitNoNode
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.infra.scrollAndClick

/**
 * What every screen shares: tabs, buttons, fields, dialogs. Nodes are found by their English text
 * where it is unique on screen, and by the tags in `TestTags` where it is not.
 */
internal open class AppRobot(protected val compose: ComposeTestRule) {

    fun openTab(label: String) = click(label)

    fun click(text: String) {
        // Some Material buttons (the extended FAB) clear their label from the merged tree, so
        // the clickable is also looked for as the parent of the text in the unmerged one.
        val merged = hasText(text) and hasClickAction()
        val unmerged = hasClickAction() and hasAnyDescendant(hasText(text))
        compose.awaitAnyNode(merged, unmerged).scrollAndClick()
    }

    fun clickDescription(description: String) {
        compose.awaitNode(hasContentDescription(description) and hasClickAction()).scrollAndClick()
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
