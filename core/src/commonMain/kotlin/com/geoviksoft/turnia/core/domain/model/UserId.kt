package com.geoviksoft.turnia.core.domain.model

import kotlin.jvm.JvmInline

/**
 * A user's Firebase Auth uid.
 *
 * Every id in Turnia is a string — a group, an event, an event type, a user — and nothing but the
 * parameter name stops one from being passed where another belongs. `grantCalendarAccess(groupId)`
 * compiles today.
 *
 * Deliberately **not** used inside the `doc` classes or the navigation keys: those are wire
 * formats, and their encoders are the one place where wrapping a string is not free.
 */
@JvmInline
value class UserId(val value: String) {
    override fun toString(): String = value
}

fun String.toUserId(): UserId = UserId(this)
