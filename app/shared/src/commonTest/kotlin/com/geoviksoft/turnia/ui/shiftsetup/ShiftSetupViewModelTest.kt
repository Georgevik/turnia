package com.geoviksoft.turnia.ui.shiftsetup

import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.ShiftSetupVia
import com.geoviksoft.turnia.core.domain.repository.ShiftSetupRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.ui.shiftsetup.model.CustomShiftForm
import com.geoviksoft.turnia.ui.shiftsetup.model.ShiftPreset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftSetupViewModelTest {

    private val repository = FakeShiftSetupRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(via: ShiftSetupVia = ShiftSetupVia.Onboarding) = ShiftSetupViewModel(via, repository, ShiftRowFactory())

    @Test
    fun opensWithThreeOfTheFivePresetsSelected() {
        val state = viewModel().uiState.value

        assertEquals(ShiftPreset.entries, state.rows.map { it.preset })
        assertEquals(
            listOf(ShiftPreset.Morning, ShiftPreset.Afternoon, ShiftPreset.Night),
            state.rows.filter { it.selected }.map { it.preset },
        )
        assertFalse(state.interacted)
        assertEquals(listOf(ShiftSetupVia.Onboarding), repository.shown)
    }

    @Test
    fun togglingIsAnInteraction() {
        val viewModel = viewModel()

        viewModel.toggle(ShiftPreset.Night.name)

        val state = viewModel.uiState.value
        assertFalse(state.rows.single { it.preset == ShiftPreset.Night }.selected)
        assertTrue(state.interacted)
    }

    @Test
    fun expandingShowsTheHoursWithoutCountingAsAnInteraction() {
        val viewModel = viewModel()

        viewModel.expand(ShiftPreset.Morning.name)

        assertTrue(viewModel.uiState.value.rows.first().expanded)
        assertFalse(viewModel.uiState.value.interacted)
    }

    @Test
    fun editingATimeIsAnInteraction() {
        val viewModel = viewModel()

        viewModel.startChanged(ShiftPreset.Morning.name, "0700")

        assertEquals("07:00", viewModel.uiState.value.rows.first().start)
        assertTrue(viewModel.uiState.value.interacted)
    }

    @Test
    fun nothingSelectedCannotBeConfirmed() {
        val viewModel = viewModel()

        listOf(ShiftPreset.Morning, ShiftPreset.Afternoon, ShiftPreset.Night).forEach { viewModel.toggle(it.name) }

        assertFalse(viewModel.uiState.value.canConfirm)
    }

    @Test
    fun aCustomShiftNeedsANameAndAnAcronym() {
        val viewModel = viewModel()
        viewModel.openCustom()
        assertTrue(viewModel.uiState.value.interacted, "Opening the form is already touching the panel")

        viewModel.addCustom()

        val form = assertNotNull(viewModel.uiState.value.custom)
        assertTrue(form.nameMissing)
        assertTrue(form.acronymMissing)
        assertEquals(5, viewModel.uiState.value.rows.size)
    }

    @Test
    fun aCustomShiftJoinsTheListSelectedWithAColourOfItsOwn() {
        val viewModel = viewModel()
        viewModel.openCustom()
        viewModel.customChanged(CustomShiftForm(name = "On call", acronym = "oc"))

        viewModel.addCustom()

        val state = viewModel.uiState.value
        val custom = state.rows.last()
        assertNull(state.custom)
        assertEquals("On call", custom.name)
        assertEquals("OC", custom.acronym)
        assertTrue(custom.selected)
        assertTrue(custom.color !in state.rows.dropLast(1).map { it.color })
    }

    @Test
    fun confirmingCreatesTheSelectedShiftsInTheLanguageOnScreen() {
        val viewModel = viewModel()
        viewModel.startChanged(ShiftPreset.Morning.name, "0700")
        viewModel.openCustom()
        viewModel.customChanged(CustomShiftForm(name = "On call", acronym = "OC"))
        viewModel.addCustom()
        viewModel.toggle(ShiftPreset.Night.name)

        viewModel.confirm(spanish)

        val call = repository.completed.single()
        assertEquals(listOf("Mañana", "Tarde", "On call"), call.types.map { it.name })
        assertEquals(listOf("M", "T", "OC"), call.types.map { it.acronym })
        assertEquals("07:00", call.types.first().startTime)
        assertNull(call.types.last().startTime)
        assertTrue(call.interacted)
        assertEquals(1, call.customCount)
        assertTrue(viewModel.uiState.value.closed)
    }

    @Test
    fun confirmingTwiceCreatesOnlyOnce() {
        val viewModel = viewModel()

        viewModel.confirm(spanish)
        viewModel.confirm(spanish)

        assertEquals(1, repository.completed.size)
    }

    @Test
    fun aTapAfterASuccessfulConfirmCreatesNothingExtra() {
        val viewModel = viewModel()

        viewModel.confirm(spanish)
        assertFalse(viewModel.uiState.value.canConfirm, "The setup already closed; confirm should be disabled")

        viewModel.confirm(spanish)

        assertEquals(1, repository.completed.size)
    }

    @Test
    fun confirmingUntouchedReportsNoInteraction() {
        val viewModel = viewModel()

        viewModel.confirm(spanish)

        assertFalse(repository.completed.single().interacted)
        assertEquals(3, repository.completed.single().types.size)
    }

    @Test
    fun aFailedWriteKeepsTheSelectionAndSaysSo() {
        repository.fails = true
        val viewModel = viewModel()
        viewModel.toggle(ShiftPreset.Duty24.name)

        viewModel.confirm(spanish)

        val state = viewModel.uiState.value
        assertFalse(state.closed)
        assertNotNull(state.userMessage)
        assertTrue(state.rows.single { it.preset == ShiftPreset.Duty24 }.selected)

        viewModel.userMessageShown()
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun skippingReportsWhetherThePanelWasTouched() {
        val viewModel = viewModel()
        viewModel.toggle(ShiftPreset.Night.name)

        viewModel.skip()

        assertEquals(listOf(ShiftSetupVia.Onboarding to true), repository.skipped)
        assertTrue(viewModel.uiState.value.closed)
    }

    @Test
    fun skippingTwiceBeforeTheScreenClosesReportsOnce() {
        val gate = CompletableDeferred<Unit>()
        repository.skipGate = gate
        val viewModel = viewModel()

        viewModel.skip()
        viewModel.skip()
        gate.complete(Unit)

        assertEquals(1, repository.skipped.size)
        assertTrue(viewModel.uiState.value.closed)
    }

    private companion object {
        val spanish = mapOf(
            ShiftPreset.Morning to ("Mañana" to "M"),
            ShiftPreset.Afternoon to ("Tarde" to "T"),
            ShiftPreset.Night to ("Noche" to "N"),
            ShiftPreset.MorningAfternoon to ("Mañana y tarde" to "MT"),
            ShiftPreset.Duty24 to ("Guardia 24h" to "24H"),
        )
    }
}

private class FakeShiftSetupRepository : ShiftSetupRepository {
    data class Completed(val types: List<PersonalEventType>, val interacted: Boolean, val customCount: Int)

    val shown = mutableListOf<ShiftSetupVia>()
    val skipped = mutableListOf<Pair<ShiftSetupVia, Boolean>>()
    val completed = mutableListOf<Completed>()
    var fails = false
    var skipGate: CompletableDeferred<Unit>? = null

    override val shouldShow: StateFlow<Boolean> = MutableStateFlow(true)
    override val hintPending: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun shown(via: ShiftSetupVia) {
        shown += via
    }

    override suspend fun skipped(via: ShiftSetupVia, interacted: Boolean) {
        skipped += via to interacted
        skipGate?.await()
    }

    override suspend fun complete(
        types: List<PersonalEventType>,
        via: ShiftSetupVia,
        interacted: Boolean,
        customCount: Int,
    ): Outcome<Unit, Unit> {
        if (fails) return Unit.toFailure()
        completed += Completed(types, interacted, customCount)
        return Unit.toSuccess()
    }

    override fun hintShown() = Unit
}
