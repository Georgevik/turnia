package com.geoviksoft.turnia.core.data.datasource.firestorefunctions

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackFunction
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.UpdateProfileRequest
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.mappers.UsernameErrorMapper
import com.geoviksoft.turnia.core.domain.model.UsernameError
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.functions.FirebaseFunctions

/**
 * Calls `updateProfile`.
 */
class UserProfileFunction(
    private val functions: FirebaseFunctions,
    private val errorMapper: UsernameErrorMapper,
) {

    suspend fun updateProfile(name: String, username: String): Outcome<Unit, UsernameError> =
        outcomeCatching(TAG, errorMapper::map) {
            Logger.i(TAG, "Update profile")
            trackFunction(FUNCTION_UPDATE_PROFILE)
            functions.httpsCallable(FUNCTION_UPDATE_PROFILE)(
                UpdateProfileRequest(name = name, username = username)
            )
        }

    companion object {
        private const val TAG = "UserProfileFunction"
        private const val FUNCTION_UPDATE_PROFILE = "updateProfile"
    }
}
