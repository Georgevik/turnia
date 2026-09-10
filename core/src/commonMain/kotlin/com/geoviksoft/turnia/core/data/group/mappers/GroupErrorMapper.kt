package com.geoviksoft.turnia.core.data.group.mappers

import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.callableErrorCode
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.GroupError
import com.geoviksoft.turnia.core.domain.model.JoinGroupError
import com.geoviksoft.turnia.core.domain.model.SwapError

/**
 * What the group's Cloud Functions throw, as the errors the domain speaks. Each code belongs to one
 * function, so they can all be read here: the callable that raised it is already in the log.
 */
class GroupErrorMapper {

    fun map(throwable: Throwable): GroupError {
        val code = throwable.callableErrorCode
        Logger.e(TAG, "Group function failed with code $code", throwable)

        return when (code) {
            CODE_GROUP_NOT_FOUND -> GroupError.NotFound
            CODE_GROUP_NOT_EMPTY -> GroupError.NotEmpty
            CODE_LEAVE_GROUP_LAST_ADMIN -> GroupError.LastAdmin
            else -> GroupError.SaveFailed
        }
    }

    fun mapJoin(throwable: Throwable): JoinGroupError {
        val code = throwable.callableErrorCode
        Logger.e(TAG, "Request to join group failed with code $code", throwable)

        return when (code) {
            CODE_INVITATION_NOT_FOUND -> JoinGroupError.CodeNotFound
            CODE_INVITATION_NOT_ACTIVE -> JoinGroupError.InvitationInactive
            CODE_INVITATION_EXPIRED -> JoinGroupError.InvitationExpired
            else -> JoinGroupError.RequestFailed
        }
    }

    fun mapTake(throwable: Throwable): SwapError {
        val code = throwable.callableErrorCode
        Logger.e(TAG, "Take event failed with code $code", throwable)

        return when (code) {
            CODE_TAKE_EVENT_NOT_MEMBER -> SwapError.NotMember
            CODE_TAKE_EVENT_SELF -> SwapError.OwnShift
            CODE_TAKE_EVENT_NOT_FOUND -> SwapError.NotFound
            CODE_TAKE_EVENT_NOT_ON_SWAP -> SwapError.TakenBySomeoneElse
            else -> SwapError.SaveFailed
        }
    }

    private companion object {
        const val TAG = "GroupErrorMapper"
        const val CODE_INVITATION_NOT_FOUND = 1003
        const val CODE_INVITATION_NOT_ACTIVE = 1004
        const val CODE_INVITATION_EXPIRED = 1005
        const val CODE_LEAVE_GROUP_LAST_ADMIN = 1013
        const val CODE_GROUP_NOT_FOUND = 1022
        const val CODE_GROUP_NOT_EMPTY = 1024
        const val CODE_TAKE_EVENT_NOT_MEMBER = 3003
        const val CODE_TAKE_EVENT_SELF = 3004
        const val CODE_TAKE_EVENT_NOT_FOUND = 3005
        const val CODE_TAKE_EVENT_NOT_ON_SWAP = 3006
    }
}
