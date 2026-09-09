package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.data.datasource.firestore.PersonalEventFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.PersonalEventTypesFirestore
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.mappers.PersonalEventMapper
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.PersonalEvent
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.errorOrNull
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toInstant
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.system.valueOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class PersonalEventRepositoryImpl(
    private val userRepository: UserRepository,
    private val personalEventMapper: PersonalEventMapper,
    private val personalEventFirestore: PersonalEventFirestore,
    private val personalEventTypesFirestore: PersonalEventTypesFirestore,
) : PersonalEventRepository {

    override fun getMyEventTypes(includeDeleted: Boolean): Flow<List<PersonalEventType>> {
        return userRepository.loggedUserFlow
            .flatMapLatest { user -> getAllEventTypes(user.id) }
            .map { types -> types.filterNot { !includeDeleted && it.isDeleted } }
    }

    override suspend fun addEvent(event: PersonalEvent) {
        val uid = userRepository.loggedUser?.id ?: return
        personalEventFirestore.set(uid, event)
    }

    override suspend fun deleteEvent(eventId: EventId, eventDate: LocalDate) {
        val uid = userRepository.loggedUser?.id ?: return
        personalEventFirestore.delete(uid, eventId, eventDate)
    }

    override suspend fun saveNotes(
        eventId: EventId,
        eventDate: LocalDate,
        notes: String?
    ): Outcome<Unit, Unit> {
        val uid = userRepository.loggedUser?.id ?: return Unit.toFailure()

        personalEventFirestore.updateNotes(uid, eventId, eventDate, notes?.trim()?.ifBlank { null })
            .errorOrNull()?.let { error ->
                Logger.e(TAG, "Error saving personal event notes", error.error)
                return Unit.toFailure()
            }

        return Unit.toSuccess()
    }

    override suspend fun saveEventType(type: PersonalEventType): Outcome<Unit, Unit> {
        val userId = userRepository.loggedUser?.id ?: return Unit.toFailure()
        personalEventTypesFirestore.set(userId, type)
        return Unit.toSuccess()
    }

    override suspend fun deleteEventType(typeId: EventTypeId): Outcome<Unit, Unit> {
        val userId = userRepository.loggedUser?.id ?: return Unit.toFailure()

        personalEventTypesFirestore.delete(userId, typeId).errorOrNull()?.let { error ->
            Logger.e(TAG, "Error deleting personal event type", error.error)
            return Unit.toFailure()
        }

        return Unit.toSuccess()
    }

    override fun getEvents(
        uid: UserId,
        date: LocalDate,
        monthDelta: Int
    ): Flow<List<PersonalEvent>> = combine(
        getAllEventTypes(uid), personalEventFirestore.get(
            uid,
            from = date.minus(monthDelta, DateTimeUnit.MONTH).toInstant(),
            until = date.plus(monthDelta, DateTimeUnit.MONTH).toInstant(),
        )
    ) { types, eventsDocs ->
        val eventList = eventsDocs.valueOrNull() ?: return@combine emptyList()

        val typesMap = types.associateBy { it.id }
        eventList.mapNotNull { personalEventMapper.map(it, typesMap) }
    }

    private fun getAllEventTypes(uid: UserId): Flow<List<PersonalEventType>> =
        personalEventTypesFirestore.observe(uid)


    companion object {
        private const val TAG = "PersonalEventRepository"
    }
}
