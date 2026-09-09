package com.georgevik.turnia.core.data.user.mappers

import com.georgevik.turnia.core.data.datasource.firestorefunctions.callableErrorCode
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UsernameError
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code

class UsernameErrorMapper {

    /** What `updateProfile` throws. */
    fun map(throwable: Throwable): UsernameError {
        val code = throwable.callableErrorCode
        Logger.e(TAG, "Update profile failed with code $code", throwable)

        return when (code) {
            CODE_USERNAME_TAKEN -> UsernameError.Taken
            CODE_USERNAME_INVALID -> UsernameError.Invalid
            else -> UsernameError.SaveFailed
        }
    }

    /**
     * Claiming the reservation. Uniqueness is the rules refusing a second `create`, so a denial
     * here is the handle being taken and not a permission problem to report as one.
     */
    fun mapClaim(throwable: Throwable, username: String): UsernameError =
        if (throwable is FirebaseFirestoreException &&
            throwable.code == FirestoreExceptionCode.PERMISSION_DENIED
        ) {
            Logger.i(TAG, "Username '$username' already taken")
            UsernameError.Taken
        } else {
            Logger.e(TAG, "Could not claim username '$username'", throwable)
            UsernameError.SaveFailed
        }

    private companion object {
        const val TAG = "UsernameErrorMapper"
        const val CODE_USERNAME_INVALID = 2003
        const val CODE_USERNAME_TAKEN = 2004
    }
}
