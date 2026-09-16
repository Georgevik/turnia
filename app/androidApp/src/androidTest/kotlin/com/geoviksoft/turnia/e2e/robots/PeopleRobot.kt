package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTextInput
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

    private companion object {
        const val SHARE = "Share with someone"
    }

    fun showSharedByMe() = click("Shared by me")

    fun showSharedWithMe() = click("Shared with me")
}
