package com.georgevik.turnia.core.data.user

import com.georgevik.turnia.core.data.datasource.firestore.PersonalEventFirestore
import com.georgevik.turnia.core.data.datasource.firestore.PersonalEventTypesFirestore
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.isFailure
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toInstant
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.valueOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.random.Random

class PersonalEventRepositoryImpl(
    private val userRepository: UserRepository,
    private val personalEventMapper: PersonalEventMapper,
    private val personalEventFirestore: PersonalEventFirestore,
    private val personalEventTypesFirestore: PersonalEventTypesFirestore,
) : PersonalEventRepository {

    private val _onEventsChanged = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    override val onEventsChanged = _onEventsChanged.asSharedFlow()

    private val _onEventTypeChanged = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    override val onEventTypeChanged = _onEventTypeChanged.asSharedFlow()

    override suspend fun getMyEventTypes(includeDeleted: Boolean): List<PersonalEventType> {
        val userId = userRepository.loggedUser?.firebaseUid ?: return emptyList()
        val types = getAllEventTypes(userId)
        return if (!includeDeleted) types.filterNot { it.isDeleted } else types
    }

    override suspend fun addEvent(event: PersonalEvent) {
        val uid = userRepository.loggedUser?.firebaseUid ?: return
        personalEventFirestore.set(uid, event)
        _onEventsChanged.emit(Random.nextInt())
    }

    override suspend fun deleteEvent(eventId: String, eventDate: LocalDate) {
        val uid = userRepository.loggedUser?.firebaseUid ?: return
        personalEventFirestore.delete(uid, eventId, eventDate)
        _onEventsChanged.emit(Random.nextInt())
    }

    override suspend fun saveEventType(type: PersonalEventType): Outcome<Unit, Unit> {
        val userId = userRepository.loggedUser?.firebaseUid ?: return Unit.toFailure()
        personalEventTypesFirestore.set(userId, type)
        _onEventTypeChanged.emit(Random.nextInt())
        return Unit.toSuccess()
    }

    override suspend fun deleteEventType(typeId: String): Outcome<Unit, Unit> {
        val userId = userRepository.loggedUser?.firebaseUid ?: return Unit.toFailure()

        personalEventTypesFirestore.delete(userId, typeId).errorOrNull()?.let { error ->
            Logger.e(TAG, "Error deleting personal event type", error.error)
            return Unit.toFailure()
        }

        _onEventTypeChanged.emit(Random.nextInt())
        return Unit.toSuccess()
    }

    override suspend fun getEvents(
        userId: String,
        date: LocalDate,
        monthDelta: Int
    ): Outcome<List<PersonalEvent>, Unit> {
        val eventsDocResult = personalEventFirestore.get(
            userId,
            from = date.minus(monthDelta, DateTimeUnit.MONTH).toInstant(),
            until = date.plus(monthDelta, DateTimeUnit.MONTH).toInstant()
        )

        if (eventsDocResult.isFailure) {
            return Unit.toFailure()
        }

        val eventDocs = eventsDocResult.valueOrNull().orEmpty()

        val types = getAllEventTypes(userId).associateBy { it.id }
        return eventDocs.mapNotNull { personalEventMapper.map(it, types) }.toSuccess()
    }

    private suspend fun getAllEventTypes(uid: String): List<PersonalEventType> {
        val hostUserId = userRepository.loggedUser?.firebaseUid ?: return emptyList()
        val typeResult = personalEventTypesFirestore.get(uid, isHostUser = uid == hostUserId)

        typeResult.errorOrNull()?.let { error ->
            Logger.e(TAG, "Error fetching personal event types", error.error)
        }

        return typeResult.valueOrNull().orEmpty()
    }

    companion object {
        private const val TAG = "PersonalEventRepository"
    }
}
