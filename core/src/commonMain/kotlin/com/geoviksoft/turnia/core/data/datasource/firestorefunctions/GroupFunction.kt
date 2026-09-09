package com.geoviksoft.turnia.core.data.datasource.firestorefunctions

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackFunction
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.DeleteGroupRequest
import com.geoviksoft.turnia.core.data.group.mappers.GroupErrorMapper
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.GroupError
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.functions.FirebaseFunctions

/**
 * Calls the group lifecycle functions. Separate from [GroupMembershipFunction]: this is not about
 * who belongs to a group.
 */
class GroupFunction(
    private val functions: FirebaseFunctions,
    private val errorMapper: GroupErrorMapper,
) {

    suspend fun deleteGroup(groupId: GroupId): Outcome<Unit, GroupError> =
        outcomeCatching(TAG, errorMapper::map) {
            Logger.i(TAG, "Delete group")
            trackFunction(FUNCTION_DELETE_GROUP)
            functions.httpsCallable(FUNCTION_DELETE_GROUP)(
                DeleteGroupRequest(groupId = groupId.value)
            )
        }

    companion object {
        private const val TAG = "GroupFunction"
        private const val FUNCTION_DELETE_GROUP = "deleteGroup"
    }
}
