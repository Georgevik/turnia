package com.geoviksoft.turnia.ui.group.detail

import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.GroupError
import com.geoviksoft.turnia.core.domain.model.NewGroup
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.demo.DemoAnalytics
import com.geoviksoft.turnia.demo.DemoGroupRepository
import com.geoviksoft.turnia.demo.DemoUserRepository
import com.geoviksoft.turnia.demo.DemoWorld
import com.geoviksoft.turnia.ui.group.detail.model.GroupDetailUi
import com.geoviksoft.turnia.ui.shiftsetup.model.ShiftPreset
import com.geoviksoft.turnia.ui.system.color.toHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModelTest {

    private val groups = RecordingGroupRepository(DemoGroupRepository(DemoWorld(LocalDate(2026, 9, 28))))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun newGroup() = GroupDetailViewModel(null, groups, DemoUserRepository(), DemoAnalytics)

    private val GroupDetailViewModel.success get() = uiState.value as GroupDetailUi.Success

    @Test
    fun aNewGroupIsProposedTheFourUsualShifts() {
        val viewModel = newGroup()

        viewModel.proposeTypes(texts)

        val rows = viewModel.success.eventTypes
        assertEquals(listOf("Morning", "Afternoon", "Night", "Morning & afternoon"), rows.map { it.name })
        assertEquals(listOf("M", "A", "N", "MA"), rows.map { it.acronym })
        assertEquals("08:00", rows.first().startTime)
        assertEquals("15:00", rows.first().endTime)
    }

    @Test
    fun proposingTwiceDoesNotDuplicate() {
        val viewModel = newGroup()

        viewModel.proposeTypes(texts)
        viewModel.proposeTypes(texts)

        assertEquals(4, viewModel.success.eventTypes.size)
    }

    @Test
    fun aProposedTypeCanBeRemoved() {
        val viewModel = newGroup()
        viewModel.proposeTypes(texts)
        val night = viewModel.success.eventTypes.single { it.name == "Night" }

        viewModel.onRemoveType(night.typeId)

        assertEquals(listOf("Morning", "Afternoon", "Morning & afternoon"), viewModel.success.eventTypes.map { it.name })
    }

    @Test
    fun withEveryTypeRemovedTheGroupIsNotSaved() {
        val viewModel = newGroup()
        viewModel.proposeTypes(texts)
        viewModel.onNameChanged("Quirófano")

        viewModel.success.eventTypes.forEach { viewModel.onRemoveType(it.typeId) }
        viewModel.onSave()

        assertNull(groups.created)
    }

    @Test
    fun savingCreatesTheGroupWithExactlyTheListedTypes() {
        val viewModel = newGroup()
        viewModel.proposeTypes(texts)
        viewModel.onNameChanged("Quirófano")
        viewModel.onRemoveType(viewModel.success.eventTypes.single { it.name == "Night" }.typeId)

        viewModel.onSave()

        val created = groups.created
        assertTrue(created != null)
        assertEquals(listOf("Morning", "Afternoon", "Morning & afternoon"), created.types.map { it.name })
        assertEquals(ShiftPreset.Morning.color.toHex(), created.types.first().defaultColor)
    }

    @Test
    fun aCreatedGroupAsksToInviteBeforeClosing() {
        groups.createSucceeds = true
        val viewModel = newGroup()
        viewModel.proposeTypes(texts)
        viewModel.onNameChanged("Quirófano")

        viewModel.onSave()

        val state = viewModel.success
        assertTrue(state.created)
        assertFalse(state.isSaved)
        assertTrue(state.form.canPassOnCode)

        viewModel.onCreatedDone()
        assertTrue(viewModel.success.isSaved)
    }

    @Test
    fun aFailedCreateShowsNoInviteStep() {
        val viewModel = newGroup()
        viewModel.proposeTypes(texts)
        viewModel.onNameChanged("Quirófano")

        viewModel.onSave()

        assertFalse(viewModel.success.created)
        assertFalse(viewModel.success.isSaved)
    }

    @Test
    fun savingAnExistingGroupClosesStraightAway() {
        val demo = DemoGroupRepository(DemoWorld(LocalDate(2026, 9, 28)))
        val viewModel = GroupDetailViewModel(GroupId("demo-urgencias"), demo, DemoUserRepository(), DemoAnalytics)
        viewModel.onNameChanged(" renamed")

        viewModel.onSave()

        assertTrue(viewModel.success.isSaved)
        assertFalse(viewModel.success.created)
    }

    private class RecordingGroupRepository(
        private val delegate: GroupRepository,
    ) : GroupRepository by delegate {
        var created: NewGroup? = null
        var createSucceeds = false

        override suspend fun createGroup(group: NewGroup): Outcome<Group, GroupError> {
            created = group
            if (!createSucceeds) return GroupError.SaveFailed.toFailure()
            return Group(
                id = GroupId("quirofano"),
                name = group.name,
                types = group.types,
                members = emptyList(),
                memberCount = 1,
                invitationCode = group.invitationCode,
                autoApprove = group.autoApprove,
                isAdmin = true,
            ).toSuccess()
        }
    }

    private companion object {
        val texts = mapOf(
            ShiftPreset.Morning to ("Morning" to "M"),
            ShiftPreset.Afternoon to ("Afternoon" to "A"),
            ShiftPreset.Night to ("Night" to "N"),
            ShiftPreset.MorningAfternoon to ("Morning & afternoon" to "MA"),
            ShiftPreset.Duty24 to ("24h duty" to "24H"),
        )
    }
}
