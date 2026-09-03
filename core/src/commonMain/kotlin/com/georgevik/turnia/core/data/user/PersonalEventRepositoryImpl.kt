package com.georgevik.turnia.core.data.user

import com.georgevik.turnia.core.data.datasource.firestore.UserPathFirestore
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.mockDelay
import com.georgevik.turnia.core.system.mockPersonalEvent
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.valueOrNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class PersonalEventRepositoryImpl(
    private val userRepository: UserRepository,
    private val userPathFirestore: UserPathFirestore
) : PersonalEventRepository {

    private val mockedMonths = mutableMapOf<String, List<PersonalEvent>>()
    private var eventTypesCache = mutableMapOf<String, PersonalEventType>()

    override suspend fun getEventTypes(): List<PersonalEventType> {
        if (eventTypesCache.isNotEmpty()) {
            Logger.i(TAG, "Returning memory cached personal event types")
            return eventTypesCache.values.toList()
        }

        val userId = userRepository.loggedUser?.firebaseUid ?: return emptyList()
        val typeResult = userPathFirestore.getPersonalEventTypes(userId)

        typeResult.errorOrNull()?.let { error ->
            Logger.e(TAG, "Error fetching personal event types", error.error)
        }

        val eventTypes = typeResult.valueOrNull().orEmpty()
        eventTypesCache.clear()
        eventTypesCache.putAll(eventTypes.associateBy { it.id })
        return eventTypes
    }

    override suspend fun addEvent(event: PersonalEvent) {
        mockDelay()
        val existing = getEventsPerDate(event.date)
        mockedMonths[bucketKey(event.date)] = existing + event
    }

    override suspend fun deleteEvent(eventId: String) {
        mockDelay()
        mockedMonths.keys.toList().forEach { key ->
            mockedMonths[key] = mockedMonths.getValue(key).filterNot { it.id == eventId }
        }
    }

    override suspend fun saveEventType(type: PersonalEventType): Outcome<Unit, Unit> {
        val userId = userRepository.loggedUser?.firebaseUid ?: return Unit.toFailure()
        userPathFirestore.setPersonalEventType(userId, type)
        eventTypesCache[type.id] = type
        return Unit.toSuccess()
    }

    override suspend fun deleteEventType(typeId: String): Outcome<Unit, Unit> {
        val userId = userRepository.loggedUser?.firebaseUid ?: return Unit.toFailure()

        userPathFirestore.deletePersonalEventType(userId, typeId).errorOrNull()?.let { error ->
            Logger.e(TAG, "Error deleting personal event type", error.error)
            return Unit.toFailure()
        }

        eventTypesCache.remove(typeId)
        return Unit.toSuccess()
    }

    override suspend fun getEvents(
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
        val eventType = getEventTypes().find { it.id == typeId }


        return if (eventType != null) Result.success(eventType)
        else Result.failure(Exception("Type not found"))
    }

    companion object {
        private const val TAG = "PersonalEventRepository"
    }
}
