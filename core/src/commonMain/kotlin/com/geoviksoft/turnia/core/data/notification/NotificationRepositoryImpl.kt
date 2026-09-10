package com.geoviksoft.turnia.core.data.notification

import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.PushDestination
import com.geoviksoft.turnia.core.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads the `type` the Cloud Functions put on every push and turns it into a destination.
 */
class NotificationRepositoryImpl : NotificationRepository {

    private val _pendingDestination = MutableStateFlow<PushDestination?>(null)
    override val pendingDestination: StateFlow<PushDestination?> = _pendingDestination.asStateFlow()

    override fun opened(data: Map<String, String>) {
        val destination = destinationOf(data) ?: return
        Logger.i(TAG, "Notification opened, going to $destination")

        _pendingDestination.value = destination
    }

    override fun destinationHandled() {
        _pendingDestination.value = null
    }

    private fun destinationOf(data: Map<String, String>): PushDestination? =
        when (data[FIELD_TYPE]) {
            TYPE_JOIN_REQUESTED -> data[FIELD_GROUP_ID]
                ?.let { PushDestination.GroupDetail(GroupId(it)) }

            TYPE_JOIN_ACCEPTED -> PushDestination.Groups
            TYPE_CALENDAR_SHARED -> PushDestination.People
            TYPE_EVENT_ON_SWAP, TYPE_EVENT_TAKEN -> PushDestination.Swap
            else -> null
        }

    companion object {
        private const val TAG = "NotificationRepository"

        // The contract with `firebase/functions/src/notifications.ts`. Changing a value here
        // without changing it there silently stops the tap from navigating anywhere.
        private const val FIELD_TYPE = "type"
        private const val FIELD_GROUP_ID = "groupId"
        private const val TYPE_JOIN_REQUESTED = "join_requested"
        private const val TYPE_JOIN_ACCEPTED = "join_accepted"
        private const val TYPE_CALENDAR_SHARED = "calendar_shared"
        private const val TYPE_EVENT_ON_SWAP = "event_on_swap"
        private const val TYPE_EVENT_TAKEN = "event_taken"
    }
}
