package com.geoviksoft.turnia.e2e.infra

import android.content.Intent
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
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * One test's world: both emulators wiped and seeded from [fixtures], the app signed in as
 * [signedInAs] (or signed out, when null), and [MainActivity] launched with [intent].
 *
 * Every test runs in a process of its own (the orchestrator clears the app's data between them),
 * so nothing the previous test cached — Koin singletons, Firestore's disk cache — leaks in.
 */
class E2eRule(
    private val signedInAs: String?,
    private val fixtures: List<String> = listOf("base"),
    private val intent: () -> Intent = { launchIntent() },
) : ExternalResource() {

    /** The day the fixture's relative dates were resolved against. */
    lateinit var today: LocalDate
        private set

    private lateinit var scenario: ActivityScenario<MainActivity>

    override fun before() {
        today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        // Firestore keeps microseconds at most; a whole millisecond compares the same on both sides.
        val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        val fixture = Fixtures.load(fixtures, today, now)

        FirestoreRest.wipe()
        AuthRest.wipe()
        fixture.users.values.forEach(AuthRest::create)
        FirestoreRest.write(fixture.documents)

        signedInAs?.let { uid ->
            val user = fixture.users.getValue(uid)
            runBlocking { Firebase.auth.signInWithEmailAndPassword(user.email, user.password) }
        }

        scenario = ActivityScenario.launch(intent())
    }

    override fun after() {
        scenario.close()
    }

    /** The day [days] from the one the fixture was seeded on, as `$date(+N)` names it. */
    fun day(days: Int): LocalDate = today.plus(days, DateTimeUnit.DAY)

    companion object {
        fun launchIntent(): Intent {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            return Intent(context, MainActivity::class.java)
        }
    }
}
