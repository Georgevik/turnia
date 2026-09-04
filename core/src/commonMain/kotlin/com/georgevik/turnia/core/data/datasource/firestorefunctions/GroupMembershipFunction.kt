package com.georgevik.turnia.core.data.datasource.firestorefunctions

import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.AcceptJoinRequest
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.functions.FirebaseFunctions

/**
 * Calls `acceptJoinRequest`: membership is never written by a client.
 */
class GroupMembershipFunction(private val functions: FirebaseFunctions) {

    suspend fun acceptJoinRequest(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<Unit, GroupError> =
        outcomeCatching(TAG, { GroupError.SaveFailed }) {
            Logger.i(TAG, "Accept join request")
            functions.httpsCallable(FUNCTION_ACCEPT_JOIN_REQUEST)(
                AcceptJoinRequest(groupId = groupId.value, uid = userId.value)
            )
        }

    companion object {
        private const val TAG = "GroupMembershipFunction"
        private const val FUNCTION_ACCEPT_JOIN_REQUEST = "acceptJoinRequest"
    }
}
