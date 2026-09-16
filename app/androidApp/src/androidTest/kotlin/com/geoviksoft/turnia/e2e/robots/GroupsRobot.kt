package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTextInput
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.infra.scrollAndClick
import com.geoviksoft.turnia.ui.system.TestTags

/** The Groups tab, a group's own screen, and the event type form a group's types are made in. */
internal class GroupsRobot(compose: ComposeTestRule) : AppRobot(compose) {

    fun openFabMenu() {
        compose.awaitNode(hasTestTag(TestTags.GROUPS_FAB)).scrollAndClick()
    }

    fun join(code: String) {
        openFabMenu()
        click("Join a group")
        compose.awaitNode(hasTestTag(TestTags.JOIN_CODE_FIELD)).performTextInput(code)
        click("Send")
    }

    fun awaitJoinCode(code: String) {
        compose.awaitNode(hasTestTag(TestTags.JOIN_CODE_FIELD) and hasText(code))
    }

    /** Settings → My groups → the group: straight to its screen, past its calendar. */
    fun openGroupFromSettings(name: String) {
        openTab(TAB_SETTINGS)
        click("My groups")
        click(name)
    }

    fun openGroupCalendar(name: String) {
        openTab(TAB_GROUPS)
        compose.awaitNode(hasText(name) and hasClickAction()).scrollAndClick()
    }

    fun createGroup(name: String, typeName: String, typeAcronym: String) {
        openTab(TAB_GROUPS)
        openFabMenu()
        // The empty state offers the same button; both open the same form.
        click("Create group")
        type("Group name", name)
        click("Add event type")
        fillEventType(typeName, typeAcronym)
        awaitText(typeName)
        click("Create group")
    }

    fun fillEventType(name: String, acronym: String) {
        type("Name", name)
        type("Calendar abbreviation", acronym)
        click("Save")
    }

    fun leaveGroup() {
        clickDescription("Leave group")
        click("Leave")
    }
}
