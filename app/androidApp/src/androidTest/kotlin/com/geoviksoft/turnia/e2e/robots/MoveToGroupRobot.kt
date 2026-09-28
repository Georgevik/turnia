package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.infra.scrollAndClick
import com.geoviksoft.turnia.ui.system.TestTags

/** The sheet a personal shift's "Move to a group" opens, from the day sheet it sits in. */
internal class MoveToGroupRobot(compose: ComposeTestRule) : AppRobot(compose) {

    fun startMove(eventId: String) {
        compose.awaitNode(
            hasTestTag(TestTags.MOVE_TO_GROUP) and hasAnyAncestor(hasTestTag(TestTags.dayEvent(EventId(eventId))))
        ).scrollAndClick()
    }

    fun pickType(typeId: String) {
        compose.awaitNode(hasTestTag(TestTags.moveType(EventTypeId(typeId)))).scrollAndClick()
    }

    fun onlyThisOne() = click(ONLY_THIS)

    fun allOfThem() = click(ALL)

    fun done() = click(DONE)

    companion object {
        const val ONLY_THIS = "Only this one"
        const val ALL = "All of them"
        const val DONE = "Done"
    }
}
