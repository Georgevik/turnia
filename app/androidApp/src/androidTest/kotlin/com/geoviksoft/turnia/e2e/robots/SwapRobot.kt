package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.infra.scrollAndClick
import com.geoviksoft.turnia.ui.system.TestTags

internal class SwapRobot(compose: ComposeTestRule) : AppRobot(compose) {

    fun openMine() {
        openTab(TAB_SWAPS)
        click("My requests")
    }

    fun openColleagues() {
        openTab(TAB_SWAPS)
        click("From colleagues")
    }

    fun awaitEvent(eventId: String) {
        compose.awaitNode(row(eventId))
    }

    fun take(eventId: String) {
        compose.awaitNode(hasText("I'll cover it") and hasClickAction() and hasAnyAncestor(row(eventId)))
            .scrollAndClick()
        click("Cover it")
    }

    private fun row(eventId: String) = hasTestTag(TestTags.swapEvent(EventId(eventId)))
}
