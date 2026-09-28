package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.geoviksoft.turnia.e2e.infra.FixedAppConfigRepository
import com.geoviksoft.turnia.e2e.infra.UI_TIMEOUT_MS
import com.geoviksoft.turnia.e2e.infra.awaitNoNode
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.infra.scrollAndClick
import com.geoviksoft.turnia.ui.system.TestTags

/** The "What shifts do you work?" screen. Rows are found by their position: presets come in a fixed order. */
internal class ShiftSetupRobot(compose: ComposeTestRule) : AppRobot(compose) {

    fun awaitSetup() {
        compose.awaitNode(hasTestTag(TestTags.SHIFT_SETUP))
    }

    fun awaitNoSetup() {
        compose.awaitNoNode(hasTestTag(TestTags.SHIFT_SETUP))
    }

    /**
     * The device has decided the setup is not owed. Settling is the last thing the decision does, so
     * a setup that was going to open would already have been asked for.
     */
    fun awaitSettled() {
        compose.waitUntil(UI_TIMEOUT_MS) { FixedAppConfigRepository.shiftSetupSettled }
        compose.waitForIdle()
    }

    fun toggle(row: Int) {
        val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        compose.awaitNode(checkbox and hasAnyAncestor(hasTestTag(TestTags.shiftSetupRow(row))), useUnmergedTree = true)
            .scrollAndClick()
    }

    fun times(row: Int, start: String, end: String) {
        compose.awaitNode(hasTestTag(TestTags.shiftSetupStart(row))).apply {
            performTextClearance()
            performTextInput(start)
        }
        compose.awaitNode(hasTestTag(TestTags.shiftSetupEnd(row))).apply {
            performTextClearance()
            performTextInput(end)
        }
    }

    fun addCustom(name: String, acronym: String) {
        click(ADD_ANOTHER)
        compose.awaitNode(hasTestTag(TestTags.SHIFT_SETUP_CUSTOM_NAME)).performTextInput(name)
        compose.awaitNode(hasTestTag(TestTags.SHIFT_SETUP_CUSTOM_ACRONYM)).performTextInput(acronym)
        click(ADD)
    }

    fun confirm() {
        compose.awaitNode(hasTestTag(TestTags.SHIFT_SETUP_CONFIRM)).scrollAndClick()
    }

    fun skip() = click(SKIP)

    companion object {
        const val SKIP = "Skip"
        const val ADD_ANOTHER = "Add another shift"
        const val ADD = "Add"
        const val HINT = "Tap a day to add one of your shifts"
        const val CREATE_MY_SHIFTS = "Create my shifts"
    }
}
