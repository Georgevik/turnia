package com.geoviksoft.turnia.core.data.config

import com.geoviksoft.turnia.core.data.user.GroupMembership
import com.geoviksoft.turnia.core.domain.model.Membership
import com.geoviksoft.turnia.core.domain.model.TeamPromptChoice
import com.geoviksoft.turnia.core.domain.model.User
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.fakes.FakeAppConfigRepository
import com.geoviksoft.turnia.core.fakes.InMemoryDataStore
import com.geoviksoft.turnia.core.fakes.RecordingAnalytics
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TeamPromptRepositoryImplTest {

    private val membership = FakeGroupMembership()
    private val analytics = RecordingAnalytics()
    private val dataStore = InMemoryDataStore()

    private fun repository(
        enabled: Boolean = true,
        threshold: Int = 5,
        dataStore: InMemoryDataStore = this.dataStore,
    ) = TeamPromptRepositoryImpl(
        session = MutableStateFlow(UserSession.Authenticated(user)),
        dataStore = dataStore,
        appConfigRepository = FakeAppConfigRepository(
            FakeAppConfigRepository.defaultFlags.copy(
                teamPromptEnabled = enabled,
                teamPromptThreshold = threshold,
            )
        ),
        membership = membership,
        analytics = analytics,
    )

    @Test
    fun belowTheThresholdNothingIsDueAndNothingIsRead() = runTest {
        val repository = repository()

        repository.eventsAdded(4)

        assertFalse(repository.pending.value)
        assertEquals(0, membership.reads)
    }

    @Test
    fun reachingTheThresholdWithNoGroupMakesItDue() = runTest {
        val repository = repository()

        repository.eventsAdded(5)

        assertTrue(repository.pending.value)
    }

    @Test
    fun aGroupMemberSettlesAndIsNeverAskedAgain() = runTest {
        membership.answer = true.toSuccess()
        val repository = repository()

        repository.eventsAdded(5)
        membership.answer = false.toSuccess()
        repository.eventsAdded(6)

        assertFalse(repository.pending.value)
        assertEquals(1, membership.reads)
    }

    @Test
    fun withoutTheServerItWaitsForTheNextEvent() = runTest {
        membership.answer = Unit.toFailure()
        val repository = repository()

        repository.eventsAdded(5)
        assertFalse(repository.pending.value)

        membership.answer = false.toSuccess()
        repository.eventsAdded(6)
        assertTrue(repository.pending.value)
    }

    @Test
    fun switchedOffNothingIsDue() = runTest {
        val repository = repository(enabled = false)

        repository.eventsAdded(50)

        assertFalse(repository.pending.value)
        assertEquals(0, membership.reads)
    }

    @Test
    fun shownIsLoggedOnceAndSettlesTheDevice() = runTest {
        val repository = repository()
        repository.eventsAdded(5)

        repository.shown()
        repository.shown()

        assertEquals(1, analytics.named("onboard_team_shown").size)

        // Same device, a new process: the prompt is not owed again.
        val relaunched = repository()
        relaunched.eventsAdded(9)
        assertFalse(relaunched.pending.value)
    }

    @Test
    fun aDoubleAnswerIsLoggedOnce() = runTest {
        val repository = repository()
        repository.eventsAdded(5)

        assertTrue(repository.answered(TeamPromptChoice.Create))
        assertFalse(repository.answered(TeamPromptChoice.Create))

        val answered = analytics.named("onboard_team_answered").single()
        assertEquals("create", answered.parameters["choice"])
        assertFalse(repository.pending.value)
    }

    @Test
    fun aDueButUnshownPromptComesBackAfterARelaunch() = runTest {
        repository().eventsAdded(5)

        val relaunched = repository()
        relaunched.eventsAdded(6)

        assertTrue(relaunched.pending.value)
    }

    private class FakeGroupMembership : GroupMembership {
        var answer: Outcome<Boolean, Unit> = false.toSuccess()
        var reads = 0

        override suspend fun serverHasAnyGroup(uid: UserId): Outcome<Boolean, Unit> {
            reads++
            return answer
        }
    }

    private companion object {
        val user = User(
            id = UserId("solo"),
            email = null,
            displayName = "Solo",
            username = "solo",
            membership = Membership.entries.first(),
        )
    }
}
