package com.geoviksoft.turnia.ui.components.movetogroup

import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.MoveError
import com.geoviksoft.turnia.core.domain.model.MoveResult
import com.geoviksoft.turnia.core.domain.model.MoveScope
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.demo.DemoGroupRepository
import com.geoviksoft.turnia.demo.DemoPersonalEventRepository
import com.geoviksoft.turnia.demo.DemoWorld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class MoveToGroupViewModelTest {

    private val world = DemoWorld(LocalDate(2026, 9, 28))
    private val demoGroups = DemoGroupRepository(world)
    private val demoPersonal = DemoPersonalEventRepository(world)
    private val course = world.personalEvents.first()
    private val request = MoveRequest(course.id, course.date, course.type.id, course.notes)

    private val personal = RecordingPersonal(demoPersonal)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun twoGroupsAreOffered() {
        val viewModel = MoveToGroupViewModel(request, demoGroups, personal)

        assertEquals(2, assertIs<MoveStep.PickGroup>(viewModel.step.value).groups.size)
    }

    @Test
    fun aSingleGroupSkipsStraightToItsTypes() {
        val viewModel = MoveToGroupViewModel(request, onlyFirstGroup(), personal)

        assertIs<MoveStep.PickType>(viewModel.step.value)
    }

    @Test
    fun theOnlyEventOfItsTypeMovesWithNoScopeAsked() {
        val viewModel = MoveToGroupViewModel(request, demoGroups, personal)
        val type = pickFirstGroupAndType(viewModel)

        val done = assertIs<MoveStep.Done>(viewModel.step.value)
        assertEquals(1, done.moved)
        assertEquals(MoveScope.One, personal.moves.single().second)
        assertEquals(listOf(course.id), personal.moves.single().first.map { it.id })
        assertEquals(type.groupName, done.groupName)
    }

    @Test
    fun aTypeWithOtherEventsAsksAndMovesAll() {
        val other = course.copy(id = com.geoviksoft.turnia.core.domain.model.EventId("other"), date = course.date.plus(7, DateTimeUnit.DAY))
        personal.extraCandidates = listOf(other)
        val viewModel = MoveToGroupViewModel(request, demoGroups, personal)
        pickFirstGroupAndType(viewModel)

        assertEquals(2, assertIs<MoveStep.PickScope>(viewModel.step.value).count)

        viewModel.pickScope(MoveScope.All)

        assertEquals(MoveScope.All, personal.moves.single().second)
        assertEquals(2, personal.moves.single().first.size)
        assertIs<MoveStep.Done>(viewModel.step.value)
    }

    @Test
    fun aTakenDayIsShown() {
        personal.result = MoveError.DayTaken.toFailure()
        val viewModel = MoveToGroupViewModel(request, demoGroups, personal)
        pickFirstGroupAndType(viewModel)

        assertEquals(MoveError.DayTaken, assertIs<MoveStep.Failed>(viewModel.step.value).error)
    }

    @Test
    fun aFailureIsShown() {
        personal.result = MoveError.Failed.toFailure()
        val viewModel = MoveToGroupViewModel(request, demoGroups, personal)
        pickFirstGroupAndType(viewModel)

        assertEquals(MoveError.Failed, assertIs<MoveStep.Failed>(viewModel.step.value).error)
    }

    @Test
    fun theDoneStepCarriesTheCounts() {
        personal.result = MoveResult(moved = 12, skipped = 2).toSuccess()
        val viewModel = MoveToGroupViewModel(request, demoGroups, personal)
        pickFirstGroupAndType(viewModel)

        val done = assertIs<MoveStep.Done>(viewModel.step.value)
        assertEquals(12, done.moved)
        assertEquals(2, done.skipped)
    }

    private fun pickFirstGroupAndType(viewModel: MoveToGroupViewModel): GroupEventType {
        val group = world.groups.first()
        viewModel.pickGroup(group.id)
        val types = assertIs<MoveStep.PickType>(viewModel.step.value).types
        viewModel.pickType(types.first().id)
        return group.moveTargetTypes.first()
    }

    private fun onlyFirstGroup() = object : GroupRepository by demoGroups {
        override fun getGroups(): Flow<List<Group>> = flowOf(listOf(world.groups.first()))
    }

    private class RecordingPersonal(private val delegate: PersonalEventRepository) :
        PersonalEventRepository by delegate {
        var extraCandidates: List<PersonalTypedEvent> = emptyList()
        var result: Outcome<MoveResult, MoveError>? = null
        val moves = mutableListOf<Pair<List<PersonalTypedEvent>, MoveScope>>()

        override suspend fun moveCandidates(event: PersonalTypedEvent): Outcome<List<PersonalTypedEvent>, MoveError> =
            (listOf(event) + extraCandidates).toSuccess()

        override suspend fun moveToGroup(
            events: List<PersonalTypedEvent>,
            target: GroupEventType,
            scope: MoveScope,
        ): Outcome<MoveResult, MoveError> {
            moves += events to scope
            return result ?: MoveResult(moved = events.size, skipped = 0).toSuccess()
        }
    }
}
