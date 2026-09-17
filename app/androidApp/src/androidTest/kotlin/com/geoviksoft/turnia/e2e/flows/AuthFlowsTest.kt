package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.string
import com.geoviksoft.turnia.e2e.robots.SignInRobot
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Paths 1–3: getting into the app. */
@RunWith(AndroidJUnit4::class)
class AuthFlowsTest {

    private val compose = createEmptyComposeRule()

    private val signedOut = E2eRule(compose)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(signedOut)

    private val robot = SignInRobot(compose)

    @Test
    fun signInWithEmail_landsOnCalendar() {
        robot.signIn("alice@e2e.turnia.club", PASSWORD)

        robot.awaitMain()
    }

    @Test
    fun createAccountWithEmail_provisionsTheProfile() {
        robot.createAccount("Nuria Nueva", "nuria@e2e.turnia.club", PASSWORD)

        robot.awaitMain()
        val uid = checkNotNull(Firebase.auth.currentUser?.uid)
        val profile = Documents.await("users/$uid") { it.string("name") == "Nuria Nueva" }
        val username = checkNotNull(profile.string("username"))
        assertEquals(uid, Documents.get("usernames/$username").string("uid"))
        assertEquals("nuria@e2e.turnia.club", Documents.get("users/$uid/private/account").string("email"))
    }

    private companion object {
        const val PASSWORD = "Turnia-e2e-1"
    }
}

/** Path 3 needs a session from the start, so it cannot share the signed-out rule above. */
@RunWith(AndroidJUnit4::class)
class CompleteNameFlowTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(E2eRule(compose, signedInAs = "nameless"))

    private val robot = SignInRobot(compose)

    @Test
    fun namelessAccount_isAskedForAName() {
        robot.completeName("Nora Nameless")

        robot.awaitNoText("What's your name?")
        Documents.await("users/nameless") { it.string("name") == "Nora Nameless" }
    }
}
