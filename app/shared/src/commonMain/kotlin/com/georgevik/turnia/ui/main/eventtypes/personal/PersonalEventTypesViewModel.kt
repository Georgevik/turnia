package com.georgevik.turnia.ui.main.eventtypes.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.ui.main.eventtypes.personal.model.PersonalEventTypeRowUi
import com.georgevik.turnia.ui.main.eventtypes.personal.model.PersonalEventTypesMessage
import com.georgevik.turnia.ui.main.eventtypes.personal.model.PersonalEventTypesUi
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toComposeColorOr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonalEventTypesViewModel(
    private val personalRepository: PersonalEventRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PersonalEventTypesUi>(PersonalEventTypesUi.Loading)
    val uiState: StateFlow<PersonalEventTypesUi> = _uiState.asStateFlow()

    init {
        refreshEvents()
        viewModelScope.launch {
            personalRepository.onEventTypeChanged.collect { refreshEvents() }
        }
    }

    fun refreshEvents() {
        viewModelScope.launch {
            val types = personalRepository.getMyEventTypes().map { it.toUi() }.sortedBy { it.name }
            _uiState.update { state ->
                val message = (state as? PersonalEventTypesUi.Success)?.userMessage
                PersonalEventTypesUi.Success(types = types, userMessage = message)
            }
        }
    }

    fun onDelete(typeId: String) {
        viewModelScope.launch {
            personalRepository.deleteEventType(typeId).fold(
                onSuccess = { refreshEvents() },
                onFailure = {
                    updateSuccess { it.copy(userMessage = PersonalEventTypesMessage.DeleteFailed) }
                },
            )
        }
    }

    fun userMessageShown() = updateSuccess { it.copy(userMessage = null) }

    private fun PersonalEventType.toUi() = PersonalEventTypeRowUi(
        typeId = id,
        name = name,
        acronym = acronym,
        startTime = startTime,
        endTime = endTime,
        color = color.toComposeColorOr(entityColor(id)),
    )

    private fun updateSuccess(block: (PersonalEventTypesUi.Success) -> PersonalEventTypesUi.Success) =
        _uiState.update { if (it is PersonalEventTypesUi.Success) block(it) else it }
}
