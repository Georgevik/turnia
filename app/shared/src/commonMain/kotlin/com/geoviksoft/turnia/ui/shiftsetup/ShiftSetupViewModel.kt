package com.geoviksoft.turnia.ui.shiftsetup

import com.geoviksoft.turnia.ui.system.color.toHex
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.ShiftSetupVia
import com.geoviksoft.turnia.core.domain.repository.ShiftSetupRepository
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.ui.main.eventtypes.detail.EventTypeDetailViewModel
import com.geoviksoft.turnia.ui.system.components.time.toTimeInput
import com.geoviksoft.turnia.ui.system.components.time.toTimeOrNull
import com.geoviksoft.turnia.ui.system.createUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.shift_setup_save_error

class ShiftSetupViewModel(
    private val via: ShiftSetupVia,
    private val repository: ShiftSetupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftSetupUi(rows = ShiftPreset.entries.map { it.toRow() }))
    val uiState: StateFlow<ShiftSetupUi> = _uiState.asStateFlow()

    init {
        // One display, one report: the ViewModel outlives a rotation, and a new display is a new one.
        viewModelScope.launch { repository.shown(via) }
    }

    fun toggle(id: String) = touch {
        copy(rows = rows.map { if (it.id == id) it.copy(selected = !it.selected) else it })
    }

    fun startChanged(id: String, value: String) = touch {
        copy(rows = rows.map { if (it.id == id) it.copy(start = value.toTimeInput()) else it })
    }

    fun endChanged(id: String, value: String) = touch {
        copy(rows = rows.map { if (it.id == id) it.copy(end = value.toTimeInput()) else it })
    }

    fun openCustom() = touch { copy(custom = CustomShiftForm()) }

    fun cancelCustom() = _uiState.update { it.copy(custom = null) }

    fun customChanged(form: CustomShiftForm) = _uiState.update { state ->
        state.copy(
            custom = form.copy(
                acronym = form.acronym.uppercase().take(EventTypeDetailViewModel.MAX_ACRONYM_SIZE),
                start = form.start.toTimeInput(),
                end = form.end.toTimeInput(),
                nameMissing = state.custom?.nameMissing == true && form.name.isBlank(),
                acronymMissing = state.custom?.acronymMissing == true && form.acronym.isBlank(),
            )
        )
    }

    fun addCustom() = touch {
        val form = custom ?: return@touch this
        if (form.name.isBlank() || form.acronym.isBlank()) {
            return@touch copy(
                custom = form.copy(nameMissing = form.name.isBlank(), acronymMissing = form.acronym.isBlank())
            )
        }
        val row = ShiftRowUi(
            id = "custom-${createUuid()}",
            preset = null,
            name = form.name.trim(),
            acronym = form.acronym.trim(),
            start = form.start,
            end = form.end,
            color = nextColor(rows),
            selected = true,
        )
        copy(rows = rows + row, custom = null)
    }

    /**
     * [presetText] holds each preset's name and acronym in the language on screen, which is the one
     * the types are created in: from then on they are the user's words, not the app's.
     */
    fun confirm(presetText: Map<ShiftPreset, Pair<String, String>>) {
        val state = _uiState.value
        if (!state.canConfirm) return
        _uiState.update { it.copy(saving = true) }

        val selected = state.rows.filter { it.selected }
        val types = selected.map { row ->
            val (name, acronym) = row.preset?.let { presetText.getValue(it) } ?: (row.name to row.acronym)
            PersonalEventType(
                id = EventTypeId(createUuid()),
                name = name,
                color = row.color.toHex(),
                acronym = acronym,
                description = null,
                startTime = row.start.toTimeOrNull(),
                endTime = row.end.toTimeOrNull(),
            )
        }

        viewModelScope.launch {
            repository.complete(
                types = types,
                via = via,
                interacted = state.interacted,
                customCount = selected.count { it.isCustom },
            ).fold(
                onSuccess = { _uiState.update { it.copy(saving = false, closed = true) } },
                onFailure = {
                    _uiState.update { it.copy(saving = false, userMessage = Res.string.shift_setup_save_error) }
                },
            )
        }
    }

    fun skip() {
        if (_uiState.value.closed) return
        val interacted = _uiState.value.interacted
        // Settled before the screen leaves, or Main would find the setup still owed and open it again.
        viewModelScope.launch {
            repository.skipped(via, interacted)
            _uiState.update { it.copy(closed = true) }
        }
    }

    fun userMessageShown() = _uiState.update { it.copy(userMessage = null) }

    private fun touch(transform: ShiftSetupUi.() -> ShiftSetupUi) =
        _uiState.update { it.transform().copy(interacted = true) }

    private fun ShiftPreset.toRow() = ShiftRowUi(
        id = name,
        preset = this,
        name = "",
        acronym = "",
        start = start.toString(),
        end = end.toString(),
        color = color,
        selected = selectedByDefault,
    )

    private companion object {
        /** A shift of the user's own takes the first colour no other shift on the list wears. */
        fun nextColor(rows: List<ShiftRowUi>): Color {
            val used = rows.map { it.color }.toSet()
            return EntityPalette.firstOrNull { it !in used } ?: EntityPalette.first()
        }
    }
}
