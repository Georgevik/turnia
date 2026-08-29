package com.georgevik.turnia.core.data.personal

import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class PersonalEventRepositoryImpl : PersonalEventRepository {

    private val _personalEvents = MutableStateFlow<List<PersonalEvent>>(emptyList())
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

    override val personalEvents: StateFlow<List<PersonalEvent>> = _personalEvents.asStateFlow()

    override fun update(typeId: String?, type: PersonalEventType) {
        // typeId == null -> create new type and save it locally and network (ignore type.id)
        // typeId != null -> update type
        // TODO("Not yet implemented")
    }

    override suspend fun refreshPersonalEvents(date: LocalDate): Result<Unit> {
        // TODO Update _personalEvents based on +-1 month from [date]

        return Result.success(Unit)
    }

    override suspend fun getEventType(typeId: String): Result<PersonalEventType> {
        // TODO If personal type is empty, then refresh. Otherwise we don't need to refresh
        val eventType = _personalEventTypes.value.find { it.id == typeId }


        return if (eventType != null) Result.success(eventType)
        else Result.failure(Exception("Type not found"))
    }
}
