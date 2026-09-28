package com.geoviksoft.turnia.ui.components.daydetail

import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.repository.AdRepository
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.demo.DemoGroupRepository
import com.geoviksoft.turnia.demo.DemoPeople
import com.geoviksoft.turnia.demo.DemoPersonalEventRepository
import com.geoviksoft.turnia.demo.DemoTeamPromptRepository
import com.geoviksoft.turnia.demo.DemoUserRepository
import com.geoviksoft.turnia.demo.DemoWorld
import com.geoviksoft.turnia.ui.components.calendar.model.toUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import com.geoviksoft.turnia.core.domain.model.EventKind
import com.geoviksoft.turnia.core.domain.model.SharePrompt
import com.geoviksoft.turnia.core.domain.model.SharePromptAnswer
import com.geoviksoft.turnia.core.domain.repository.SharePromptRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A note on a group shift is saved as the user's own, and a failure reaches the sheet as state. */
@OptIn(ExperimentalCoroutinesApi::class)
class DayDetailNotesTest {

    private val world = DemoWorld(LocalDate(2026, 9, 28))
    private val demoGroups = DemoGroupRepository(world)
    private val event = world.groupEvents.first { it.assigneeId == DemoPeople.me.id }
    private val row = event.toUi(currentUserId = DemoPeople.me.id, notesEditable = true)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aGroupNoteIsSavedAsTheUsersOwn() = runTest {
        val viewModel = sheet(demoGroups)

        viewModel.saveNotes(row, "Parking B")

        assertEquals("Parking B", demoGroups.getMyEventNotes(event.date).first()[event.id])
        assertFalse(viewModel.noteError.value)
    }

    @Test
    fun aFailedGroupNoteSetsTheErrorUntilShown() = runTest {
        val failing = object : GroupRepository by demoGroups {
            override suspend fun saveEventNote(
                groupId: GroupId,
                eventId: EventId,
                eventDate: LocalDate,
                notes: String?,
            ): Outcome<Unit, Unit> = Unit.toFailure()
        }
        val viewModel = sheet(failing)

        viewModel.saveNotes(row, "Parking B")
        assertTrue(viewModel.noteError.value)

        viewModel.noteErrorShown()
        assertFalse(viewModel.noteError.value)
    }

    @Test
    fun aRowThatIsNotEditableSavesNothing() = runTest {
        val viewModel = sheet(demoGroups)

        viewModel.saveNotes(row.copy(notesEditable = false), "Parking B")

        assertTrue(demoGroups.getMyEventNotes(event.date).first().isEmpty())
    }

    private fun sheet(groups: GroupRepository) = DayDetailSheetViewModel(
        date = event.date,
        addMode = DayAddMode.Disabled,
        groupRepository = groups,
        personalRepository = DemoPersonalEventRepository(world),
        userRepository = DemoUserRepository(),
        adRepository = NoAds,
        eventAddedPrompts = EventAddedPrompts(NoSharePrompt, DemoTeamPromptRepository),
    )

    private object NoAds : AdRepository {
        override val bannerVisible: Flow<Boolean> = flowOf(false)
        override fun actionPerformed() = Unit
    }

    private object NoSharePrompt : SharePromptRepository {
        override val pending: StateFlow<SharePrompt?> = MutableStateFlow(null)
        override suspend fun eventAdded(kind: EventKind): Int = 0
        override suspend fun shown(prompt: SharePrompt) = Unit
        override fun answered(prompt: SharePrompt, answer: SharePromptAnswer) = false
    }
}
