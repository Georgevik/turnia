package com.georgevik.turnia.core.domain.model

import kotlin.jvm.JvmInline

/** A group's document id. See [UserId] for why these ids are wrapped. */
@JvmInline
value class GroupId(val value: String) {
    override fun toString(): String = value
}
