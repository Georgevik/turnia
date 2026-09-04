package com.georgevik.turnia.core.domain.model

import kotlin.jvm.JvmInline

/**
 * An event's document id, group or personal alike — everything on a calendar is an event, and the
 * two live in different collections but are the same concept.
 */
@JvmInline
value class EventId(val value: String) {
    override fun toString(): String = value
}
