package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.geoviksoft.turnia.e2e.infra.awaitNode

internal class SignInRobot(compose: ComposeTestRule) : AppRobot(compose) {

    fun signIn(email: String, password: String) {
        click("Continue with email")
        type("Email", email)
        type("Password", password)
        click("Sign in")
    }

    fun createAccount(name: String, email: String, password: String) {
        click("Continue with email")
        click("Create account")
        // The form's own title says "Create account" too, but only the button can be clicked.
        compose.awaitNode(hasText(name.let { "Name" }))
        type("Name", name)
        type("Email", email)
        type("Password", password)
        // Terms and privacy: the only two checkboxes on the form.
        compose.awaitNode(isToggleable())
        compose.onAllNodes(isToggleable()).fetchSemanticsNodes().indices.forEach { index ->
            compose.onAllNodes(isToggleable())[index].performClick()
        }
        compose.awaitNode(hasText("Create account") and hasClickAction()).performClick()
    }

    fun completeName(name: String) {
        awaitText("What's your name?")
        type("Name", name)
        click("Continue")
    }
}
