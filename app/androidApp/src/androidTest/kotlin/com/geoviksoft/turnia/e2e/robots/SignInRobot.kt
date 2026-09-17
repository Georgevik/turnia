package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.infra.scrollAndClick

internal class SignInRobot(compose: ComposeTestRule) : AppRobot(compose) {

    fun goThroughOnboarding() {
        click("Next")
        click("Next")
        click("Get started")
    }

    fun awaitSignIn() = awaitText("Continue with email")

    fun signIn(email: String, password: String) {
        click("Continue with email")
        type("Email", email)
        type("Password", password)
        click("Sign in")
    }

    fun createAccount(name: String, email: String, password: String) {
        click("Continue with email")
        click("Create account")
        type("Name", name)
        type("Email", email)
        type("Password", password)
        Espresso.closeSoftKeyboard()
        // Terms and privacy. The box itself, not its row: the row's centre is the link to the page.
        val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        compose.awaitNode(checkbox, useUnmergedTree = true)
        val boxes = compose.onAllNodes(checkbox, useUnmergedTree = true)
        boxes.fetchSemanticsNodes().indices.forEach { boxes[it].scrollAndClick() }
        // The form's own title says "Create account" too, but only the button can be clicked.
        click("Create account")
    }

    fun completeName(name: String) {
        awaitText("What's your name?")
        type("Name", name)
        click("Continue")
    }
}
