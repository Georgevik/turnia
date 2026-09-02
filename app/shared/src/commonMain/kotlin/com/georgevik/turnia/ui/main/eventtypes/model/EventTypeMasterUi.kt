package com.georgevik.turnia.ui.main.eventtypes.model

sealed interface EventTypeMasterUi {
    data object Loading : EventTypeMasterUi
    data class Success(
        val query: String = "",
        val sections: List<EventTypeMasterHeaderUi> = emptyList(),
    ) : EventTypeMasterUi

    data class Error(val error: EventTypeMasterError) : EventTypeMasterUi
}

enum class EventTypeMasterError { LoadFailed }
