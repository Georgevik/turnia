package com.georgevik.turnia.core.data.datasource.firestorefunctions

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackFunction
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.AcceptJoinRequest
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.LeaveGroupRequest
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.RejectJoinRequest
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.RemoveMemberRequest
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.RequestToJoinGroup
import com.georgevik.turnia.core.data.datasource.firestorefunctions.responses.JoinGroupResponse
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.JoinGroupError
import com.georgevik.turnia.core.domain.model.JoinGroupStatus
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.functions.FirebaseFunctions

/**
 * Calls the membership functions: membership is never written by a client, neither granted
 * (`requestToJoinGroup` / `acceptJoinRequest`) nor withdrawn (`leaveGroup` / `removeMember`), and a
 * join request is never answered by one either (`rejectJoinRequest`).
 */
class GroupMembershipFunction(private val functions: FirebaseFunctions) {

    suspend fun acceptJoinRequest(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<Unit, GroupError> =
        outcomeCatching(TAG, { GroupError.SaveFailed }) {
            Logger.i(TAG, "Accept join request")
            trackFunction(FUNCTION_ACCEPT_JOIN_REQUEST)
            functions.httpsCallable(FUNCTION_ACCEPT_JOIN_REQUEST)(
                AcceptJoinRequest(groupId = groupId.value, uid = userId.value)
            )
        }

    /**
     * A function and not the delete it used to be: `status` is frozen against every client, and
     * deleting the request would answer it by destroying the only thing the requester can read to
     * learn the answer.
     */
    suspend fun rejectJoinRequest(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<Unit, GroupError> =
        outcomeCatching(TAG, { GroupError.SaveFailed }) {
            Logger.i(TAG, "Reject join request")
            trackFunction(FUNCTION_REJECT_JOIN_REQUEST)
            functions.httpsCallable(FUNCTION_REJECT_JOIN_REQUEST)(
                RejectJoinRequest(groupId = groupId.value, uid = userId.value)
            )
        }

    suspend fun requestToJoinGroup(code: String): Outcome<JoinGroupStatus, JoinGroupError> =
        outcomeCatching(TAG, { throwable -> throwable.toJoinGroupError() }) {
            Logger.i(TAG, "Request to join group")
            trackFunction(FUNCTION_REQUEST_TO_JOIN_GROUP)
            val result = functions.httpsCallable(FUNCTION_REQUEST_TO_JOIN_GROUP)(
                RequestToJoinGroup(code = code)
            )

            val status = result.data<JoinGroupResponse>().status
            status.toJoinGroupStatus() ?: run {
                Logger.e(TAG, "Unknown join status: $status")
                return Outcome.Failure(JoinGroupError.RequestFailed)
            }
        }

    suspend fun leaveGroup(groupId: GroupId): Outcome<Unit, GroupError> =
        outcomeCatching(TAG, { throwable -> throwable.toGroupError() }) {
            Logger.i(TAG, "Leave group")
            trackFunction(FUNCTION_LEAVE_GROUP)
            functions.httpsCallable(FUNCTION_LEAVE_GROUP)(
                LeaveGroupRequest(groupId = groupId.value)
            )
        }
    

    suspend fun removeMember(groupId: GroupId, userId: UserId): Outcome<Unit, GroupError> =
        outcomeCatching(TAG, { throwable -> throwable.toGroupError() }) {
            Logger.i(TAG, "Remove member")
            trackFunction(FUNCTION_REMOVE_MEMBER)
            functions.httpsCallable(FUNCTION_REMOVE_MEMBER)(
                RemoveMemberRequest(groupId = groupId.value, uid = userId.value)
            )
        }

    private fun String.toJoinGroupStatus(): JoinGroupStatus? = when (this) {
        JoinGroupResponse.STATUS_JOINED -> JoinGroupStatus.Joined
        JoinGroupResponse.STATUS_REQUESTED -> JoinGroupStatus.Requested
        JoinGroupResponse.STATUS_ALREADY_MEMBER -> JoinGroupStatus.AlreadyMember
        else -> null
    }

    private fun Throwable.toGroupError(): GroupError {
        val code = message?.substringBefore(':')?.trim()?.toIntOrNull()
        Logger.e(TAG, "Membership change failed with code $code", this)

        return if (code == CODE_LEAVE_GROUP_LAST_ADMIN) GroupError.LastAdmin
        else GroupError.SaveFailed
    }

    private fun Throwable.toJoinGroupError(): JoinGroupError {
        val code = message?.substringBefore(':')?.trim()?.toIntOrNull()
        Logger.e(TAG, "Request to join group failed with code $code", this)

        return when (code) {
            CODE_INVITATION_NOT_FOUND -> JoinGroupError.CodeNotFound
            CODE_INVITATION_NOT_ACTIVE -> JoinGroupError.InvitationInactive
            CODE_INVITATION_EXPIRED -> JoinGroupError.InvitationExpired
            else -> JoinGroupError.RequestFailed
        }
    }

    companion object {
        private const val TAG = "GroupMembershipFunction"
        private const val FUNCTION_ACCEPT_JOIN_REQUEST = "acceptJoinRequest"
        private const val FUNCTION_REJECT_JOIN_REQUEST = "rejectJoinRequest"
        private const val FUNCTION_REQUEST_TO_JOIN_GROUP = "requestToJoinGroup"
        private const val FUNCTION_LEAVE_GROUP = "leaveGroup"
        private const val FUNCTION_REMOVE_MEMBER = "removeMember"
        private const val CODE_INVITATION_NOT_FOUND = 1003
        private const val CODE_INVITATION_NOT_ACTIVE = 1004
        private const val CODE_INVITATION_EXPIRED = 1005
        private const val CODE_LEAVE_GROUP_LAST_ADMIN = 1013
    }
}
