package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

/**
 * The current user's personal event templates. Unlike group types these belong to
 * no group and carry their own [PersonalEventType.color]; the user can create and
 * edit them freely.
 */
interface PersonalEventRepository {

    val personalEventTypes: StateFlow<List<PersonalEventType>>

    val personalEvents: StateFlow<List<PersonalEvent>>

    fun update(typeId: String?, type: PersonalEventType)

    suspend fun refreshPersonalEvents(date: LocalDate): Result<Unit>


    suspend fun getEventType(typeId: String): Result<PersonalEventType>

    fun byId(id: String): PersonalEventType? = personalEventTypes.value.firstOrNull { it.id == id }
}
