package com.geoviksoft.turnia.core.data.datasource.firestorefunctions

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackFunction
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.ReturnEventRequest
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.TakeEventRequest
import com.geoviksoft.turnia.core.data.group.mappers.GroupErrorMapper
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.SwapError
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.functions.FirebaseFunctions

/**
 * Calls the functions that write a group event the caller does not own.
 */
class GroupEventFunction(
    private val functions: FirebaseFunctions,
    private val errorMapper: GroupErrorMapper,
) {

    /**
     * Takes a shift somebody offered.
     *
     * A function and not a write because it reassigns a document belonging to someone else, and
     * because the check that keeps two members from taking the same shift has to happen in the same
     * transaction as the reassignment.
     */
    suspend fun takeEvent(groupId: GroupId, eventId: EventId): Outcome<Unit, SwapError> =
        outcomeCatching(TAG, errorMapper::mapTake) {
            Logger.i(TAG, "Take group event")
            trackFunction(FUNCTION_TAKE_EVENT)
            functions.httpsCallable(FUNCTION_TAKE_EVENT)(
                TakeEventRequest(groupId = groupId.value, eventId = eventId.value)
            )
        }

    /**
     * Gives a taken shift back to whoever held it before, offered for swap again. A function for
     * the same reason as [takeEvent]: it reassigns the shift and appends to its frozen history.
     */
    suspend fun returnEvent(groupId: GroupId, eventId: EventId): Outcome<Unit, SwapError> =
        outcomeCatching(TAG, errorMapper::mapReturn) {
            Logger.i(TAG, "Return group event")
            trackFunction(FUNCTION_RETURN_EVENT)
            functions.httpsCallable(FUNCTION_RETURN_EVENT)(
                ReturnEventRequest(groupId = groupId.value, eventId = eventId.value)
            )
        }

    companion object {
        private const val TAG = "GroupEventFunction"
        private const val FUNCTION_TAKE_EVENT = "takeEvent"
        private const val FUNCTION_RETURN_EVENT = "returnEvent"
    }
}
