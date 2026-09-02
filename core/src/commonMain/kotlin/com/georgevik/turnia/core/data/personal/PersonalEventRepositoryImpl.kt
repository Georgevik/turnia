package com.georgevik.turnia.core.data.personal

import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.system.mockDelay
import com.georgevik.turnia.core.system.MOCK_PERSONAL_TYPES
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

    override suspend fun getPersonalEventTypes(): List<PersonalEventType> {
        mockDelay()
        return MOCK_PERSONAL_TYPES
    }

    override suspend fun addPersonalEvent(event: PersonalEvent) {
        mockDelay()
        val existing = getEventsPerDate(event.date)
        mockedMonths[bucketKey(event.date)] = existing + event
    }

    override suspend fun deletePersonalEvent(eventId: String) {
        mockDelay()
        mockedMonths.keys.toList().forEach { key ->
            mockedMonths[key] = mockedMonths.getValue(key).filterNot { it.id == eventId }
        }
    }

    override suspend fun update(typeId: String?, type: PersonalEventType) : Result<Unit>{
        // typeId == null -> create new type and save it locally and network (ignore type.id)
        // typeId != null -> update type
        mockDelay()
        return Result.success(Unit)
    }

    override suspend fun retrievePersonalEvents(
        userId: String,
        date: LocalDate,
        monthDelta: Int
    ): Result<List<PersonalEvent>> {
        mockDelay()
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
        return mockedMonths.getOrPut(bucketKey(date)) {
            mockPersonalEvent(fromMonth = date)
        }
    }

    private fun bucketKey(date: LocalDate) = "${date.year}_${date.month}"

    override suspend fun getEventType(typeId: String): Result<PersonalEventType> {
        // TODO If personal type is empty, then refresh. Otherwise we don't need to refresh
        val eventType = getPersonalEventTypes().find { it.id == typeId }


        return if (eventType != null) Result.success(eventType)
        else Result.failure(Exception("Type not found"))
    }
}
