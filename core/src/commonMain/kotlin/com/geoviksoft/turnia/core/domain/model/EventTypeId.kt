package com.geoviksoft.turnia.core.domain.model

import kotlin.jvm.JvmInline

/**
 * The id of the template an event is created from, matching the sealed [EventType]: one type for
 * both kinds, since a `GroupEventType` and a `PersonalEventType` are the same concept.
 */
@JvmInline
value class EventTypeId(val value: String) {
    override fun toString(): String = value
}
