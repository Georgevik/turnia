package com.geoviksoft.turnia.ui.components.daydetail

import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.core.domain.model.EventKind
import com.geoviksoft.turnia.core.domain.model.SharePrompt
import com.geoviksoft.turnia.core.domain.model.SharePromptAnswer
import com.geoviksoft.turnia.core.domain.repository.AdRepository
import com.geoviksoft.turnia.core.domain.repository.SharePromptRepository
import com.geoviksoft.turnia.demo.DemoGroupRepository
import com.geoviksoft.turnia.demo.DemoPeople
import com.geoviksoft.turnia.demo.DemoPersonalEventRepository
import com.geoviksoft.turnia.demo.DemoTeamPromptRepository
import com.geoviksoft.turnia.demo.DemoUserRepository
import com.geoviksoft.turnia.demo.DemoWorld
import com.geoviksoft.turnia.ui.components.daydetail.components.EventTypeChipUi
import com.geoviksoft.turnia.ui.components.daydetail.model.EventTypeUi
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Picking a shift closes the add pane, so a visit adds one event however many taps land while it
 * is closing. No clock is involved: the guard is a latch, not a time window.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DayDetailAddEventTest {

    private val world = DemoWorld(LocalDate(2026, 9, 28))
    private val date = LocalDate(2026, 9, 28)
    private val personal = DemoPersonalEventRepository(world)
    private val prompts = CountingSharePrompt()
    private val ads = CountingAds()

    private val first = EventTypeUi(EventTypeChipUi("F", Color.Red), world.personalTypes.first())
    private val second = EventTypeUi(EventTypeChipUi("S", Color.Blue), world.personalTypes.last())

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun threeTapsOnTheSameShiftAddOneEvent() = runTest {
        val viewModel = sheet()
        val before = countOf(first)

        repeat(3) { viewModel.addEventOfType(first) }

        assertEquals(before + 1, countOf(first))
    }

    @Test
    fun aSecondShiftTappedWhileThePaneClosesIsIgnored() = runTest {
        val viewModel = sheet()
        val beforeFirst = countOf(first)
        val beforeSecond = countOf(second)

        viewModel.addEventOfType(first)
        viewModel.addEventOfType(second)

        assertEquals(beforeFirst + 1, countOf(first))
        assertEquals(beforeSecond, countOf(second), "The second shift was tapped after the first was accepted")
    }

    @Test
    fun repeatedTapsCountAsOneAddedEvent() = runTest {
        val viewModel = sheet()

        repeat(3) { viewModel.addEventOfType(first) }

        assertEquals(1, prompts.counted, "The share prompt's counter must rise by one, not three")
        assertEquals(1, ads.actions, "An ad action is one accepted add, not one tap")
    }

    @Test
    fun theSheetClosingAllowsTheNextVisitToAdd() = runTest {
        val viewModel = sheet()

        val beforeFirst = countOf(first)
        val beforeSecond = countOf(second)

        viewModel.addEventOfType(first)
        // The view model is keyed by date and outlives one opening of the sheet.
        viewModel.sheetClosed()
        viewModel.addEventOfType(second)

        assertEquals(beforeFirst + 1, countOf(first))
        assertEquals(beforeSecond + 1, countOf(second))
        assertEquals(2, prompts.counted)
    }

    private suspend fun countOf(type: EventTypeUi) =
        personal.getEvents(DemoPeople.me.id, date, monthDelta = 0).first()
            .count { it.date == date && it.type.id == type.eventType.id }

    private fun sheet() = DayDetailSheetViewModel(
        date = date,
        addMode = DayAddMode.Full,
        groupRepository = DemoGroupRepository(world),
        personalRepository = personal,
        userRepository = DemoUserRepository(),
        adRepository = ads,
        eventAddedPrompts = EventAddedPrompts(prompts, DemoTeamPromptRepository),
    )

    private class CountingAds : AdRepository {
        var actions = 0
        override val bannerVisible: Flow<Boolean> = flowOf(false)
        override fun actionPerformed() {
            actions++
        }
    }

    private class CountingSharePrompt : SharePromptRepository {
        var counted = 0
        override val pending: StateFlow<SharePrompt?> = MutableStateFlow(null)
        override suspend fun eventAdded(kind: EventKind): Int = ++counted
        override suspend fun shown(prompt: SharePrompt) = Unit
        override fun answered(prompt: SharePrompt, answer: SharePromptAnswer) = false
    }
}
