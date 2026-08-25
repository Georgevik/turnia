package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.PersonalEventType
import kotlinx.coroutines.flow.StateFlow

/**
 * The current user's personal event templates. Unlike group types these belong to
 * no group and carry their own [PersonalEventType.color]; the user can create and
 * edit them freely.
 */
interface PersonalEventRepository {

    /** The user's personal event templates. */
    val personalEventTypes: StateFlow<List<PersonalEventType>>

    /** Adds a new personal event template (its [PersonalEventType.id] must be set). */
    fun create(type: PersonalEventType)

    /** Replaces the personal event template with the same id. */
    fun update(type: PersonalEventType)

    /** Convenience lookup by id. */
    fun byId(id: String): PersonalEventType? = personalEventTypes.value.firstOrNull { it.id == id }
}
