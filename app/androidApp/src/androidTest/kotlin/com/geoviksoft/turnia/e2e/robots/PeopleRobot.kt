package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.e2e.infra.awaitNoNode
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.infra.scrollAndClick
import com.geoviksoft.turnia.ui.system.TestTags

internal class PeopleRobot(compose: ComposeTestRule) : AppRobot(compose) {

    fun shareWith(usernamePrefix: String, username: String) {
        openTab(TAB_PEOPLE)
        // The button lives on this list only: a labelled one while the list is empty, the FAB after.
        showSharedByMe()
        compose.awaitNode(
            (hasText(SHARE) or hasContentDescription(SHARE)) and hasClickAction()
        ).scrollAndClick()
        compose.awaitNode(hasTestTag(TestTags.USER_SEARCH_FIELD)).performTextInput(usernamePrefix)
        // The results list sits under a disabled clickable that merges it; the tap is geometric,
        // so the row's own text is as good a target as any.
        compose.awaitNode(hasText("@$username"), useUnmergedTree = true).scrollAndClick()
    }

    fun showSharedByMe() = click("Shared by me")

    fun showSharedWithMe() = click("Shared with me")

    fun showHidden(count: Int) = click("$HIDDEN_CHIP$count")

    fun awaitNoHiddenChip() = compose.awaitNoNode(hasText(HIDDEN_CHIP, substring = true))

    /** The shortcut: a swipe from the end of the row. */
    fun hideBySwipe(uid: String) = row(uid).performTouchInput { swipeLeft() }

    /** The accessible way in: a long press opens a menu with the same action the swipe runs. */
    fun hideByLongPress(uid: String) {
        row(uid).performTouchInput { longClick() }
        click(HIDE)
    }

    fun unhideBySwipe(uid: String) = row(uid).performTouchInput { swipeRight() }

    /** The row's own button; only meaningful while exactly one calendar is hidden. */
    fun unhideByButton() = clickDescription(SHOW)

    fun undo() = click(UNDO)

    private fun row(uid: String) = compose.awaitNode(hasTestTag(TestTags.personRow(UserId(uid))))

    companion object {
        private const val SHARE = "Share with someone"
        private const val HIDE = "Hide calendar"
        private const val SHOW = "Show calendar"
        private const val UNDO = "Undo"
        private const val HIDDEN_CHIP = "Hidden · "

        const val CALENDAR_HIDDEN = "Calendar hidden"
        const val ALL_HIDDEN = "You've hidden every calendar"
    }
}
