package com.geoviksoft.turnia.core.data.datasource.firestorefunctions

/**
 * The numeric code a Cloud Function put at the front of its message, or null when the failure came
 * from somewhere else — no network, no session, a function that threw before it could name what
 * went wrong. Every error mapper in the data layer reads it the same way.
 */
internal val Throwable.callableErrorCode: Int?
    get() = message?.substringBefore(':')?.trim()?.toIntOrNull()
