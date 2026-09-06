package com.georgevik.turnia.core.data.datasource.firestorefunctions

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackFunction
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.DeleteGroupRequest
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.functions.FirebaseFunctions

/**
 * Calls the group lifecycle functions. Separate from [GroupMembershipFunction]: this is not about
 * who belongs to a group.
 */
class GroupFunction(private val functions: FirebaseFunctions) {

    suspend fun deleteGroup(groupId: GroupId): Outcome<Unit, GroupError> =
        outcomeCatching(TAG, { throwable -> throwable.toGroupError() }) {
            Logger.i(TAG, "Delete group")
            trackFunction(FUNCTION_DELETE_GROUP)
            functions.httpsCallable(FUNCTION_DELETE_GROUP)(
                DeleteGroupRequest(groupId = groupId.value)
            )
        }

    private fun Throwable.toGroupError(): GroupError {
        val code = message?.substringBefore(':')?.trim()?.toIntOrNull()
        Logger.e(TAG, "Delete group failed with code $code", this)

        return when (code) {
            CODE_DELETE_GROUP_NOT_EMPTY -> GroupError.NotEmpty
            CODE_DELETE_GROUP_NOT_FOUND -> GroupError.NotFound
            else -> GroupError.SaveFailed
        }
    }

    companion object {
        private const val TAG = "GroupFunction"
        private const val FUNCTION_DELETE_GROUP = "deleteGroup"
        private const val CODE_DELETE_GROUP_NOT_FOUND = 1022
        private const val CODE_DELETE_GROUP_NOT_EMPTY = 1024
    }
}
