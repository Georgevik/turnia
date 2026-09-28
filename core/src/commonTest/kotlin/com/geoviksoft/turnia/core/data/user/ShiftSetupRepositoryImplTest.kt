package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.Membership
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.PersonalOneOffEvent
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.domain.model.ShiftSetupVia
import com.geoviksoft.turnia.core.domain.model.User
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.fakes.FakeAppConfigRepository
import com.geoviksoft.turnia.core.fakes.RecordingAnalytics
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftSetupRepositoryImplTest {

    private val config = FakeAppConfigRepository()
    private val invitations = FakeInvitations()
    private val contents = FakeAccountContents()
    private val personal = FakePersonalEventRepository()
    private val analytics = RecordingAnalytics()
    private val session = MutableStateFlow<UserSession>(UserSession.Authenticated(user))

    private fun TestScope.repository(): ShiftSetupRepositoryImpl {
        val repository =
            ShiftSetupRepositoryImpl(session, config, invitations, contents, personal, analytics, backgroundScope)
        // Background work is not what advanceUntilIdle waits for: run it explicitly.
        runCurrent()
        return repository
    }

    @Test
    fun aSettledDeviceIsNeverAskedAndReadsNothing() = runTest {
        config.shiftSetupSettled = true

        assertFalse(repository().due.value)
        assertEquals(0, contents.cacheReads + contents.serverReads)
    }

    @Test
    fun aPendingInvitationIsNotAsked() = runTest {
        invitations.pendingCode.value = "ABC123"

        assertFalse(repository().due.value)
        assertEquals(0, contents.serverReads)
    }

    @Test
    fun aCachedTypeOrGroupSettlesWithoutAServerRead() = runTest {
        contents.cached = true

        assertFalse(repository().due.value)
        assertTrue(config.shiftSetupSettled)
        assertEquals(0, contents.serverReads)
    }

    @Test
    fun anEmptyAccountOnTheServerIsAsked() = runTest {
        contents.server = false.toSuccess()

        assertTrue(repository().due.value)
        assertFalse(config.shiftSetupSettled, "Only an answer from the user settles it")
    }

    @Test
    fun anAccountWithSomethingOnTheServerSettles() = runTest {
        contents.server = true.toSuccess()

        assertFalse(repository().due.value)
        assertTrue(config.shiftSetupSettled)
    }

    @Test
    fun noServerAnswerIsNotAnEmptyAccount() = runTest {
        contents.server = Unit.toFailure()

        assertFalse(repository().due.value)
        assertFalse(config.shiftSetupSettled, "The next launch has to ask again")
    }

    @Test
    fun signedOutIsNeverAsked() = runTest {
        session.value = UserSession.Unauthenticated

        assertFalse(repository().due.value)
        assertEquals(0, contents.cacheReads)
    }

    @Test
    fun anInvitationArrivingWhileAskedSettlesIt() = runTest {
        val repository = repository()
        assertTrue(repository.due.value)

        invitations.pendingCode.value = "ABC123"
        runCurrent()

        assertFalse(repository.due.value)
        assertTrue(config.shiftSetupSettled)
    }

    @Test
    fun skippingAfterSignInSettlesAndReports() = runTest {
        val repository = repository()

        repository.skipped(ShiftSetupVia.Onboarding, interacted = true)

        assertFalse(repository.due.value)
        assertTrue(config.shiftSetupSettled)
        val skipped = analytics.named("onboard_shift_skipped").single()
        assertEquals("true", skipped.parameters["interacted"])
        assertEquals("onboarding", skipped.parameters["via"])
    }

    @Test
    fun closingFromTheAddPaneSettlesNothing() = runTest {
        config.shiftSetupSettled = false
        contents.server = true.toSuccess()
        val repository = repository()
        config.shiftSetupSettled = false

        repository.skipped(ShiftSetupVia.AddPane, interacted = false)

        assertFalse(config.shiftSetupSettled)
    }

    @Test
    fun completingWritesSettlesReportsAndOwesTheHint() = runTest {
        val repository = repository()

        val outcome = repository.complete(types(3), ShiftSetupVia.Onboarding, interacted = false, customCount = 0)

        assertEquals(Unit.toSuccess(), outcome)
        assertEquals(3, personal.created.size)
        assertFalse(repository.due.value)
        assertTrue(config.shiftSetupSettled)
        assertTrue(repository.hintPending.value)
        val completed = analytics.named("onboard_shift_completed").single()
        assertEquals(3L, completed.parameters["type_count"])
        assertEquals(0L, completed.parameters["custom_type_count"])

        repository.hintShown()
        assertFalse(repository.hintPending.value)
    }

    @Test
    fun aFailedWriteReportsNothingAndOwesNoHint() = runTest {
        personal.fails = true
        val repository = repository()

        val outcome = repository.complete(types(3), ShiftSetupVia.Onboarding, interacted = true, customCount = 1)

        assertTrue(outcome is Outcome.Failure)
        assertTrue(repository.due.value, "The setup stays owed")
        assertFalse(config.shiftSetupSettled)
        assertFalse(repository.hintPending.value)
        assertTrue(analytics.named("onboard_shift_completed").isEmpty())
    }

    @Test
    fun completingFromTheAddPaneOwesNoHint() = runTest {
        val repository = repository()

        repository.complete(types(1), ShiftSetupVia.AddPane, interacted = true, customCount = 0)

        assertFalse(repository.hintPending.value)
        assertEquals("add_pane", analytics.named("onboard_shift_completed").single().parameters["via"])
    }

    private fun types(count: Int) = List(count) { index ->
        PersonalEventType(
            id = EventTypeId("t$index"),
            name = "Type $index",
            color = "#000000",
            acronym = "T$index",
            description = null,
            startTime = null,
            endTime = null,
        )
    }

    private companion object {
        val user = User(
            id = UserId("nuevo"),
            email = null,
            displayName = "Nuevo",
            username = "nuevo",
            membership = Membership.entries.first(),
        )
    }
}

private class FakeInvitations : InvitationLinkRepository {
    override val pendingCode = MutableStateFlow<String?>(null)
    override fun opened(link: String) = Unit
    override fun referred(code: String) = Unit
    override fun codeHandled() {
        pendingCode.value = null
    }
}

private class FakeAccountContents : AccountContents {
    var cached = false
    var server: Outcome<Boolean, Unit> = false.toSuccess()
    var cacheReads = 0
    var serverReads = 0

    override suspend fun cachedHasAny(uid: UserId): Boolean {
        cacheReads++
        return cached
    }

    override suspend fun serverHasAny(uid: UserId): Outcome<Boolean, Unit> {
        serverReads++
        return server
    }
}

private class FakePersonalEventRepository : PersonalEventRepository {
    val created = mutableListOf<PersonalEventType>()
    var fails = false

    override suspend fun createEventTypes(types: List<PersonalEventType>): Outcome<Unit, Unit> {
        if (fails) return Unit.toFailure()
        created += types
        return Unit.toSuccess()
    }

    override fun getMyEventTypes(includeDeleted: Boolean): Flow<List<PersonalEventType>> = emptyFlow()
    override suspend fun addEvent(event: PersonalTypedEvent) = Unit
    override suspend fun deleteEvent(eventId: EventId, eventDate: LocalDate) = Unit
    override suspend fun saveNotes(eventId: EventId, eventDate: LocalDate, notes: String?): Outcome<Unit, Unit> =
        Unit.toSuccess()
    override suspend fun saveEventType(type: PersonalEventType, isNew: Boolean): Outcome<Unit, Unit> =
        Unit.toSuccess()
    override suspend fun deleteEventType(typeId: EventTypeId): Outcome<Unit, Unit> = Unit.toSuccess()
    override fun getEvents(uid: UserId, date: LocalDate, monthDelta: Int): Flow<List<PersonalTypedEvent>> =
        emptyFlow()
    override fun getOneOffEvents(uid: UserId, date: LocalDate, monthDelta: Int): Flow<List<PersonalOneOffEvent>> =
        emptyFlow()
    override suspend fun addOneOffEvent(event: PersonalOneOffEvent): Outcome<Unit, Unit> = Unit.toSuccess()
    override suspend fun updateOneOffEvent(previous: PersonalOneOffEvent, event: PersonalOneOffEvent): Outcome<Unit, Unit> =
        Unit.toSuccess()
    override suspend fun deleteOneOffEvent(event: PersonalOneOffEvent): Outcome<Unit, Unit> = Unit.toSuccess()
}
