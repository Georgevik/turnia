package com.geoviksoft.turnia.core.data.datasource.firestorefunctions

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackFunction
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.UpdateProfileRequest
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.mappers.UsernameErrorMapper
import com.geoviksoft.turnia.core.domain.model.DeleteAccountError
import com.geoviksoft.turnia.core.domain.model.UsernameError
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.functions.FirebaseFunctions

/**
 * Calls `updateProfile` and `deleteAccount`.
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

    suspend fun deleteAccount(): Outcome<Unit, DeleteAccountError> =
        outcomeCatching(TAG, ::mapDeleteAccountError) {
            Logger.i(TAG, "Delete account")
            trackFunction(FUNCTION_DELETE_ACCOUNT)
            functions.httpsCallable(FUNCTION_DELETE_ACCOUNT)()
        }

    private fun mapDeleteAccountError(throwable: Throwable): DeleteAccountError {
        val code = throwable.callableErrorCode
        Logger.e(TAG, "Delete account failed with code $code", throwable)

        return when (code) {
            CODE_DELETE_ACCOUNT_LAST_ADMIN -> DeleteAccountError.LastAdmin
            else -> DeleteAccountError.Failed
        }
    }

    companion object {
        private const val TAG = "UserProfileFunction"
        private const val FUNCTION_UPDATE_PROFILE = "updateProfile"
        private const val FUNCTION_DELETE_ACCOUNT = "deleteAccount"
        private const val CODE_DELETE_ACCOUNT_LAST_ADMIN = 5003
    }
}
