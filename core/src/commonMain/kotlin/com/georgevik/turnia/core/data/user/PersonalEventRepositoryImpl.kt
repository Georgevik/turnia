package com.georgevik.turnia.core.data.user

import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.data.user.datasource.PersonalTypeFirestoreError
import com.georgevik.turnia.core.data.user.datasource.UserPathFirestore
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.mockDelay
import com.georgevik.turnia.core.system.mockPersonalEvent
import com.georgevik.turnia.core.system.mockUuid
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

    override suspend fun getPersonalEventTypes(): List<PersonalEventType> {
        val userId = userRepository.loggedUser?.firebaseUid ?: return emptyList()
        val typeResult = userPathFirestore.personalTypes(userId)

        typeResult.errorOrNull()?.let { error ->
            when (error) {
                is PersonalTypeFirestoreError.LoadFailed -> Logger.e(
                    TAG,
                    "Error fetching personal event types",
                    error.error
                )
            }
        }

        return typeResult.valueOrNull().orEmpty()
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

    override suspend fun update(typeId: String?, type: PersonalEventType): Outcome<Unit, Unit> {
        val userId = userRepository.loggedUser?.firebaseUid ?: return Unit.toFailure()
        val typeId = typeId ?: mockUuid()
        userPathFirestore.setPersonalType(userId, typeId, type)

        return Unit.toSuccess()
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

    companion object {
        private const val TAG = "PersonalEventRepository"
    }
}
