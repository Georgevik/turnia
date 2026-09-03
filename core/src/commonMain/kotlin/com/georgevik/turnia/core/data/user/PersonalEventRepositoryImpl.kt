package com.georgevik.turnia.core.data.user

import com.georgevik.turnia.core.data.datasource.firestore.PersonalEventFirestore
import com.georgevik.turnia.core.data.datasource.firestore.PersonalEventTypesFirestore
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.domain.session.SessionEvents
import com.georgevik.turnia.core.domain.session.clearOnSignOut
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.isFailure
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toInstant
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.valueOrNull
import kotlinx.coroutines.CoroutineScope
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
    sessionEvents: SessionEvents,
    scope: CoroutineScope,
) : PersonalEventRepository {

    private var eventTypesCache = mutableMapOf<String, PersonalEventType>()

    init {
        sessionEvents.clearOnSignOut(scope) { eventTypesCache.clear() }
    }

    private val _onEventsChanged = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    override val onEventsChanged = _onEventsChanged.asSharedFlow()

    private val _onEventTypeChanged = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    override val onEventTypeChanged = _onEventTypeChanged.asSharedFlow()

    override suspend fun getEventTypes(): List<PersonalEventType> =
        allEventTypes().filterNot { it.isDeleted }

    override suspend fun addEvent(event: PersonalEvent) {
        val uid = userRepository.loggedUser?.firebaseUid ?: return
        personalEventFirestore.set(uid, event)
        _onEventsChanged.emit(Random.nextInt())
    }

    override suspend fun deleteEvent(eventId: String) {
        val uid = userRepository.loggedUser?.firebaseUid ?: return
        personalEventFirestore.delete(uid, eventId)
        _onEventsChanged.emit(Random.nextInt())
    }

    override suspend fun saveEventType(type: PersonalEventType): Outcome<Unit, Unit> {
        val userId = userRepository.loggedUser?.firebaseUid ?: return Unit.toFailure()
        personalEventTypesFirestore.set(userId, type)
        eventTypesCache[type.id] = type
        _onEventTypeChanged.emit(Random.nextInt())
        return Unit.toSuccess()
    }

    override suspend fun deleteEventType(typeId: String): Outcome<Unit, Unit> {
        val userId = userRepository.loggedUser?.firebaseUid ?: return Unit.toFailure()

        personalEventTypesFirestore.delete(userId, typeId).errorOrNull()?.let { error ->
            Logger.e(TAG, "Error deleting personal event type", error.error)
            return Unit.toFailure()
        }

        eventTypesCache.remove(typeId)
        _onEventTypeChanged.emit(Random.nextInt())
        return Unit.toSuccess()
    }

    override suspend fun getEvents(
        uid: String,
        date: LocalDate,
        monthDelta: Int
    ): Outcome<List<PersonalEvent>, Unit> {
        val eventsDocResult = personalEventFirestore.get(
            uid,
            from = date.minus(monthDelta, DateTimeUnit.MONTH).toInstant(),
            until = date.plus(monthDelta, DateTimeUnit.MONTH).toInstant()
        )

        if (eventsDocResult.isFailure) {
            return Unit.toFailure()
        }

        val eventDocs = eventsDocResult.valueOrNull().orEmpty()

        val types = allEventTypes().associateBy { it.id }
        return eventDocs.mapNotNull { personalEventMapper.map(it, types) }.toSuccess()
    }

    private suspend fun allEventTypes(): List<PersonalEventType> {
        if (eventTypesCache.isNotEmpty()) {
            Logger.i(TAG, "Returning memory cached personal event types")
            return eventTypesCache.values.toList()
        }

        val userId = userRepository.loggedUser?.firebaseUid ?: return emptyList()
        val typeResult = personalEventTypesFirestore.get(userId, isHostUser = true)

        typeResult.errorOrNull()?.let { error ->
            Logger.e(TAG, "Error fetching personal event types", error.error)
        }

        val eventTypes = typeResult.valueOrNull().orEmpty()
        eventTypesCache.clear()
        eventTypesCache.putAll(eventTypes.associateBy { it.id })
        return eventTypes
    }

    override suspend fun getEventType(typeId: String): Result<PersonalEventType> {
        val eventType = allEventTypes().find { it.id == typeId }
        return if (eventType != null) Result.success(eventType)
        else Result.failure(Exception("Type not found"))
    }

    companion object {
        private const val TAG = "PersonalEventRepository"
    }
}
