package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.e2e.infra.awaitNoNode
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.infra.scrollAndClick
import com.geoviksoft.turnia.ui.system.TestTags

/** The profile screen's avatar: its menu, the crop, and whose photo shows where. */
internal class ProfileRobot(compose: ComposeTestRule) : AppRobot(compose) {

    fun openProfile() {
        openTab(TAB_SETTINGS)
        click("My profile")
    }

    fun openAvatarMenu() {
        compose.awaitNode(hasTestTag(TestTags.PROFILE_AVATAR)).scrollAndClick()
    }

    /** Through the menu and the crop; the E2E app's picker hands over `photo-pick.webp` at once. */
    fun uploadPhoto() {
        openAvatarMenu()
        click("Upload photo")
        compose.awaitNode(hasTestTag(TestTags.PHOTO_CROP_USE)).scrollAndClick()
    }

    fun removePhoto() {
        openAvatarMenu()
        click("Remove photo")
    }

    fun awaitNotBusy(timeoutMs: Long) {
        compose.awaitNoNode(hasTestTag(TestTags.PROFILE_PHOTO_BUSY), useUnmergedTree = true, timeoutMs = timeoutMs)
    }

    /** The photo is drawn, not merely asked for: the tag only goes on once it has loaded. */
    fun awaitPhoto(uid: String) {
        compose.awaitNode(hasTestTag(TestTags.avatarPhoto(UserId(uid))), useUnmergedTree = true)
    }

    fun awaitNoPhoto(uid: String) {
        compose.awaitNoNode(hasTestTag(TestTags.avatarPhoto(UserId(uid))), useUnmergedTree = true)
    }

    fun renameGroup(name: String) {
        val field = compose.awaitNode(hasSetTextAction() and hasText("Group name"))
        field.performTextClearance()
        field.performTextInput(name)
        click("Save")
    }
}
