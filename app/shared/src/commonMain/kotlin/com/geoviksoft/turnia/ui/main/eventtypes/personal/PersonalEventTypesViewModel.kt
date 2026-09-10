package com.geoviksoft.turnia.ui.main.eventtypes.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.ui.main.eventtypes.personal.model.PersonalEventTypeRowUi
import com.geoviksoft.turnia.ui.main.eventtypes.personal.model.PersonalEventTypesMessage
import com.geoviksoft.turnia.ui.main.eventtypes.personal.model.PersonalEventTypesUi
import com.geoviksoft.turnia.ui.system.color.entityColor
import com.geoviksoft.turnia.ui.system.color.toComposeColorOr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonalEventTypesViewModel(
    private val personalRepository: PersonalEventRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PersonalEventTypesUi>(PersonalEventTypesUi.Loading)
    val uiState: StateFlow<PersonalEventTypesUi> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            personalRepository.getMyEventTypes()
                .map { list -> list.map { it.toUi() }.sortedBy { it.name } }
                .collect { types ->
                    _uiState.update { state ->
                        val message = (state as? PersonalEventTypesUi.Success)?.userMessage
                        PersonalEventTypesUi.Success(types = types, userMessage = message)
                    }
                }
        }
    }


    fun onDelete(typeId: EventTypeId) {
        viewModelScope.launch {
            personalRepository.deleteEventType(typeId).fold(
                onSuccess = {  },
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
        color = color.toComposeColorOr(entityColor(id.value)),
    )

    private fun updateSuccess(block: (PersonalEventTypesUi.Success) -> PersonalEventTypesUi.Success) =
        _uiState.update { if (it is PersonalEventTypesUi.Success) block(it) else it }
}
