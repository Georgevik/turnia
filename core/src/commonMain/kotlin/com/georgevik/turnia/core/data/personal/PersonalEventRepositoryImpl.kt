package com.georgevik.turnia.core.data.personal

import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Mock [PersonalEventRepository] holding the user's personal event templates in
 * memory until the Firestore-backed impl exists.
 */
class PersonalEventRepositoryImpl : PersonalEventRepository {

    private val _personalEventTypes = MutableStateFlow(
        listOf(
            PersonalEventType(
                id = "pt-gym",
                name = "Gimnasio",
                color = "#FFDDB8",
                acronym = "GYM",
                description = "Rutina semanal.",
                startTime = "18:00",
                endTime = "19:00",
            ),
            PersonalEventType(
                id = "pt-doctor",
                name = "Cita médica",
                color = "#4F6D7A",
                acronym = null,
                description = "Revisión anual.",
                startTime = null,
                endTime = null,
            ),
        )
    )
    override val personalEventTypes: StateFlow<List<PersonalEventType>> =
        _personalEventTypes.asStateFlow()

    override fun create(type: PersonalEventType) {
        _personalEventTypes.update { it + type }
    }

    override fun update(type: PersonalEventType) {
        _personalEventTypes.update { list -> list.map { if (it.id == type.id) type else it } }
    }
}
