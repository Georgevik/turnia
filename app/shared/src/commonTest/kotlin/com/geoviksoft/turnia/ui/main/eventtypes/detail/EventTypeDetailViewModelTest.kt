package com.geoviksoft.turnia.ui.main.eventtypes.detail

import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.demo.DemoGroupRepository
import com.geoviksoft.turnia.demo.DemoPersonalEventRepository
import com.geoviksoft.turnia.demo.DemoWorld
import com.geoviksoft.turnia.navigation.routes.EventTypeDetailData
import com.geoviksoft.turnia.ui.main.eventtypes.detail.model.EventTypeDetailUi
import com.geoviksoft.turnia.ui.main.eventtypes.detail.model.EventTypeFieldError
import com.geoviksoft.turnia.ui.main.eventtypes.detail.model.EventTypeToastError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A repeated save must not create a second type: a new one mints a fresh id per call, so every
 * extra tap would be a new entity. No clock is involved — the guard is a latch, not a time window.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventTypeDetailViewModelTest {

    private val world = DemoWorld(LocalDate(2026, 9, 28))
    private val groups = DemoGroupRepository(world)
    private val personal = DemoPersonalEventRepository(world)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun twoSavesOfANewShiftCreateOneType() = runTest {
        val viewModel = form(EventTypeDetailData.NewPersonal)
        viewModel.fillIn("Guardia extra", "GX")
        val before = typeCount()

        viewModel.onSave()
        viewModel.onSave()

        assertEquals(before + 1, typeCount())
        assertEquals(1, named("Guardia extra"))
    }

    @Test
    fun aSaveAfterSuccessCreatesNothingMore() = runTest {
        val viewModel = form(EventTypeDetailData.NewPersonal)
        viewModel.fillIn("Guardia extra", "GX")

        viewModel.onSave()
        assertTrue(viewModel.success.isSaved, "The screen is closing")
        // The button must not come back to life while the screen animates away.
        assertTrue(viewModel.success.saveButtonLoading)

        viewModel.onSave()

        assertEquals(1, named("Guardia extra"))
    }

    @Test
    fun twoSavesOfANewGroupTypeLeaveOnePending() = runTest {
        val viewModel = form(EventTypeDetailData.NewGroup(groupId = null))
        viewModel.fillIn("Refuerzo", "RF")

        viewModel.onSave()
        viewModel.onSave()

        assertEquals(1, groups.pendingEventTypes.value.count { it.name == "Refuerzo" })
    }

    @Test
    fun twoSavesOfAnEditUpdateOneType() = runTest {
        val existing = world.personalTypes.first()
        val viewModel = form(EventTypeDetailData.EditPersonal(existing.id.value))
        val before = typeCount()
        viewModel.fillIn("Formación larga", "FL")

        viewModel.onSave()
        viewModel.onSave()

        assertEquals(before, typeCount(), "An edit overwrites; it never adds")
        assertEquals(1, named("Formación larga"))
    }

    @Test
    fun aRejectedSaveStaysRetryable() = runTest {
        val failing = FailFirstSave(personal)
        val viewModel = form(EventTypeDetailData.NewPersonal, personal = failing)
        viewModel.fillIn("Guardia extra", "GX")

        viewModel.onSave()

        assertEquals(EventTypeToastError.SavePersonal, viewModel.success.toastError)
        assertFalse(viewModel.success.isSaved, "The form stays open")
        assertFalse(viewModel.success.saveButtonLoading, "The action is available again")
        assertEquals("Guardia extra", viewModel.success.form.name, "The input is intact")

        viewModel.onSave()

        assertTrue(viewModel.success.isSaved)
        assertEquals(1, named("Guardia extra"), "The retry created exactly one type")
    }

    @Test
    fun anIncompleteFormMarksTheFieldAndDoesNotLatch() = runTest {
        val viewModel = form(EventTypeDetailData.NewPersonal)
        viewModel.fillIn("Guardia extra", "")

        viewModel.onSave()
        assertEquals(EventTypeFieldError.Required, viewModel.success.formErrors.acronymError)
        assertEquals(0, named("Guardia extra"))

        // The refused save must not have spent the action.
        viewModel.fillIn("Guardia extra", "GX")
        viewModel.onSave()

        assertTrue(viewModel.success.isSaved)
        assertEquals(1, named("Guardia extra"))
    }

    private fun EventTypeDetailViewModel.fillIn(name: String, acronym: String) {
        onFieldChanged(EventTypeField.Name, name)
        onFieldChanged(EventTypeField.Acronym, acronym)
    }

    private val EventTypeDetailViewModel.success get() = uiState.value as EventTypeDetailUi.Success

    private suspend fun typeCount() = personal.getMyEventTypes().first().size

    private suspend fun named(name: String) =
        personal.getMyEventTypes().first().count { it.name == name }

    private fun form(
        key: EventTypeDetailData,
        personal: PersonalEventRepository = this.personal,
    ) = EventTypeDetailViewModel(key, groups, personal)

    /** Rejects the first save only, so the retry after a failure can be observed. */
    private class FailFirstSave(
        private val delegate: PersonalEventRepository,
    ) : PersonalEventRepository by delegate {
        private var rejected = false
        override suspend fun saveEventType(
            type: PersonalEventType,
            isNew: Boolean,
        ): Outcome<Unit, Unit> {
            if (!rejected) {
                rejected = true
                return Unit.toFailure()
            }
            return delegate.saveEventType(type, isNew)
        }
    }
}
