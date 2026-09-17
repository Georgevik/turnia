package com.geoviksoft.turnia.e2e.infra

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.geoviksoft.turnia.MainActivity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.time.Clock
import kotlin.time.Instant

/** Signs a single test in as [uid], overriding the class's [E2eRule.signedInAs]. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SignedInAs(val uid: String)

/**
 * One test's world: both emulators wiped and seeded from [fixtures], the app signed in as
 * [signedInAs] or the test's [SignedInAs] (signed out when neither says), and [MainActivity]
 * launched with [intent]. Onboarding counts as already seen unless [onboardingSeen] says otherwise,
 * so only the test about it pays for its pages.
 *
 * Every test runs in a process of its own (the orchestrator clears the app's data between them),
 * so nothing the previous test cached — Koin singletons, Firestore's disk cache — leaks in.
 */
class E2eRule(
    private val compose: ComposeTestRule,
    private val signedInAs: String? = null,
    private val fixtures: List<String> = listOf("base"),
    private val onboardingSeen: Boolean = true,
    private val intent: () -> Intent = { launchIntent() },
) : ExternalResource() {

    /** The day the fixture's relative dates were resolved against. */
    lateinit var today: LocalDate
        private set

    private lateinit var scenario: ActivityScenario<MainActivity>
    private var user: String? = null

    override fun apply(base: Statement, description: Description): Statement {
        user = description.getAnnotation(SignedInAs::class.java)?.uid ?: signedInAs
        // Inside the resource, so the activity is still up when the failure is described.
        val test = object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } catch (failure: Throwable) {
                    // The report is all CI keeps: say what was on screen when it failed.
                    val steps = failure.stackTrace
                        .filter { it.className.startsWith(TEST_PACKAGE) }
                        .joinToString("\n") { "  at $it" }
                    throw AssertionError("${failure.message}\n$steps\n\nOn screen:\n${screen()}", failure)
                }
            }
        }
        return super.apply(test, description)
    }

    private fun screen(): String = runCatching {
        val roots = compose.onAllNodes(isRoot(), useUnmergedTree = true)
        roots.fetchSemanticsNodes().indices.joinToString("\n") { roots[it].printToString() }
    }.getOrElse { "(no Compose hierarchy: ${it.message})" }

    override fun before() {
        today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        // Firestore keeps microseconds at most; a whole millisecond compares the same on both sides.
        val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        val fixture = Fixtures.load(fixtures, today, now)

        FirestoreRest.wipe()
        AuthRest.wipe()
        fixture.users.values.forEach(AuthRest::create)
        FirestoreRest.write(fixture.documents)

        user?.let { uid ->
            val user = fixture.users.getValue(uid)
            runBlocking { Firebase.auth.signInWithEmailAndPassword(user.email, user.password) }
        }

        FixedAppConfigRepository.onboardingSeen = onboardingSeen
        grantNotifications()
        scenario = ActivityScenario.launch(intent())
    }

    override fun after() {
        scenario.close()
    }

    // The system's permission dialog would cover the app, and the orchestrator's data clear
    // takes the grant away before every test.
    private fun grantNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    /** The day [days] from the one the fixture was seeded on, as `$date(+N)` names it. */
    fun day(days: Int): LocalDate = today.plus(days, DateTimeUnit.DAY)

    companion object {
        private const val TEST_PACKAGE = "com.geoviksoft.turnia.e2e"

        fun launchIntent(): Intent {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            return Intent(context, MainActivity::class.java)
        }
    }
}
