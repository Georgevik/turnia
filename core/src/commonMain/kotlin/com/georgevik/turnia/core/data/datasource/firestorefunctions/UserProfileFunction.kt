package com.georgevik.turnia.core.data.datasource.firestorefunctions

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackFunction
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.UpdateProfileRequest
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UsernameError
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.functions.FirebaseFunctions

/**
 * Calls `updateProfile`.
 */
class UserProfileFunction(
    private val functions: FirebaseFunctions
) {

    suspend fun updateProfile(name: String, username: String): Outcome<Unit, UsernameError> =
        outcomeCatching(TAG, { throwable -> throwable.toProfileError() }) {
            Logger.i(TAG, "Update profile")
            trackFunction(FUNCTION_UPDATE_PROFILE)
            functions.httpsCallable(FUNCTION_UPDATE_PROFILE)(
                UpdateProfileRequest(name = name, username = username)
            )
        }

    private fun Throwable.toProfileError(): UsernameError {
        val code = message?.substringBefore(':')?.trim()?.toIntOrNull()
        Logger.e(TAG, "Update profile failed with code $code", this)

        return when (code) {
            CODE_USERNAME_TAKEN -> UsernameError.Taken
            CODE_USERNAME_INVALID -> UsernameError.Invalid
            else -> UsernameError.SaveFailed
        }
    }

    companion object {
        private const val TAG = "UserProfileFunction"
        private const val FUNCTION_UPDATE_PROFILE = "updateProfile"
        private const val CODE_USERNAME_INVALID = 2003
        private const val CODE_USERNAME_TAKEN = 2004
    }
}
