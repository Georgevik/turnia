package com.geoviksoft.turnia.ui.main.mycalendar

import com.geoviksoft.turnia.ui.components.calendar.model.EventSource
import com.geoviksoft.turnia.core.domain.repository.AdRepository
import com.geoviksoft.turnia.demo.DemoAnalytics
import com.geoviksoft.turnia.demo.DemoGroupRepository
import com.geoviksoft.turnia.demo.DemoPeople
import com.geoviksoft.turnia.demo.DemoPersonalEventRepository
import com.geoviksoft.turnia.demo.DemoSharedCalendarRepository
import com.geoviksoft.turnia.demo.DemoShiftSetupRepository
import com.geoviksoft.turnia.demo.DemoUserRepository
import com.geoviksoft.turnia.demo.DemoWorld
import com.geoviksoft.turnia.navigation.main.routes.ExternalCalendarData
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.main.group.externalcalendar.ExternalCalendarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

/** A user's note on a group shift shows on their own calendars and never on a colleague's. */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupEventNotesOnCalendarsTest {

    private val world = DemoWorld(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    private val groups = DemoGroupRepository(world)
    private val personal = DemoPersonalEventRepository(world)
    private val mine = world.groupEvents.first { it.assigneeId == DemoPeople.me.id && it.date >= world.today }

    private val noAds = object : AdRepository {
        override val bannerVisible: Flow<Boolean> = flowOf(false)
        override fun actionPerformed() = Unit
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun myCalendarShowsMyNoteOnAGroupShift() = runTest {
        groups.saveEventNote(mine, "  Parking B ")

        val viewModel = MyCalendarViewModel(DemoUserRepository(), groups, personal, DemoShiftSetupRepository(personal))
        val row = (viewModel.uiState.value as MyCalendarUiState.Success).eventsByDate.row(mine.id.value)

        assertEquals("Parking B", row.notes)
        assertTrue(row.notesEditable)
    }

    @Test
    fun aGroupCalendarShowsMyNoteAndLetsMeEditIt() = runTest {
        groups.saveEventNote(mine, "Parking B")

        val viewModel = externalCalendar(ExternalCalendarData.Group(mine.groupId.value, mine.groupName))
        val rows = viewModel.uiState.first { !it.loading }.events.values.flatten()

        assertEquals("Parking B", rows.single { it.id == mine.id }.notes)
        assertTrue(rows.filter { it.source == EventSource.GROUP }.all { it.notesEditable })
    }

    @Test
    fun aColleaguesCalendarShowsNoNoteAndOffersNone() = runTest {
        val colleague = world.groupEvents.first { it.assigneeId != DemoPeople.me.id }
        groups.saveEventNote(colleague, "Parking B")

        val viewModel = externalCalendar(ExternalCalendarData.Personal(colleague.assigneeId.value, "Colleague"))
        val rows = viewModel.uiState.first { !it.loading }.events.values.flatten()

        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.notes == null })
        assertFalse(rows.any { it.notesEditable })
    }

    private fun kotlinx.coroutines.test.TestScope.externalCalendar(data: ExternalCalendarData) =
        ExternalCalendarViewModel(
            data,
            groups,
            DemoSharedCalendarRepository(world),
            DemoUserRepository(),
            noAds,
            DemoAnalytics,
        ).also { it.uiState.launchIn(backgroundScope) }

    private fun Map<*, List<DayEventUi>>.row(id: String) = values.flatten().single { it.id.value == id }
}
