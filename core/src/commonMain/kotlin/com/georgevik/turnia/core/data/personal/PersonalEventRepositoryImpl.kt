package com.georgevik.turnia.core.data.personal

import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.system.mockPersonalEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class PersonalEventRepositoryImpl : PersonalEventRepository {

    private val mockedMonths = mutableMapOf<String, List<PersonalEvent>>()

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

    override fun update(typeId: String?, type: PersonalEventType) {
        // typeId == null -> create new type and save it locally and network (ignore type.id)
        // typeId != null -> update type
        // TODO("Not yet implemented")
    }

    override suspend fun retrievePersonalEvents(
        userId: String,
        date: LocalDate,
        monthDelta: Int
    ): Result<List<PersonalEvent>> {
        val events = buildList {
            addAll(getEventsPerDate(date))

            (1..monthDelta).forEach { delta ->
                addAll(getEventsPerDate(date.plus(delta, DateTimeUnit.MONTH)))
                addAll(getEventsPerDate(date.minus(delta, DateTimeUnit.MONTH)))
            }
        }.distinctBy { it.id }

        return Result.success(events)
    }

    private fun getEventsPerDate(date: LocalDate): List<PersonalEvent> {
        return mockedMonths.getOrPut("${date.year}_${date.month}") {
            mockPersonalEvent(fromMonth = date)
        }
    }

    override suspend fun getEventType(typeId: String): Result<PersonalEventType> {
        // TODO If personal type is empty, then refresh. Otherwise we don't need to refresh
        val eventType = _personalEventTypes.value.find { it.id == typeId }


        return if (eventType != null) Result.success(eventType)
        else Result.failure(Exception("Type not found"))
    }
}
