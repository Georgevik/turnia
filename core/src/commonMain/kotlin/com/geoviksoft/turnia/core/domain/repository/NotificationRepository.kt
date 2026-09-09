package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.PushDestination
import kotlinx.coroutines.flow.StateFlow

/**
 * Where a tapped notification wants the app to go.
 *
 * State and not an event stream: the tap arrives from the platform whenever the system feels like
 * delivering it — often before the UI that has to act on it exists at all, on a cold start — so it
 * is held until somebody says they have handled it. A `Channel` would drop exactly those.
 */
interface NotificationRepository {

    /** The destination waiting to be navigated to, or `null` when there is nothing pending. */
    val pendingDestination: StateFlow<PushDestination?>

    /** The user tapped a notification. [data] is the message's raw FCM data payload. */
    fun opened(data: Map<String, String>)

    /** Called by the UI once it has navigated, so the destination is not opened twice. */
    fun destinationHandled()
}
