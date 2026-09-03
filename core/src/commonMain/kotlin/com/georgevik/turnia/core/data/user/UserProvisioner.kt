package com.georgevik.turnia.core.data.user

import com.georgevik.turnia.core.data.datasource.firestore.UserPathFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UsernameFirestore
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserPrivateDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.domain.username.UsernameFactory
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.core.system.isSuccess
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toSuccess
import dev.gitlive.firebase.auth.FirebaseUser


class UserProvisioner(
    private val remoteProfiles: UserPathFirestore,
    private val remotePrivate: UserPrivateFirestore,
    private val remoteUsernames: UsernameFirestore,
    private val usernameFactory: UsernameFactory,
) {

    suspend fun create(
        firebaseUser: FirebaseUser
    ): Outcome<UserProfile, UserProfileError> {
        Logger.w(TAG, "Empty users/${firebaseUser.uid}. New user")

        remotePrivate.updateAccount(
            firebaseUser.uid, UserPrivateDocument(
                email = firebaseUser.email.orEmpty(),
                fcmTokens = emptyList(),
            )
        ).errorOrNull()?.let { error ->
            Logger.e(TAG, "Could not create the private account: $error")
        }

        val name = firebaseUser.displayName.orEmpty()
        val username = claimUsername(firebaseUser.uid, name).orEmpty()

        return remoteProfiles.update(
            firebaseUser.uid, UserDocument(name = name, username = username)
        ).fold(
            onSuccess = {
                Logger.i(TAG, "Created users/${firebaseUser.uid}")
                UserProfile(id = firebaseUser.uid, name = name, username = username).toSuccess()
            },
            onFailure = { error ->
                Logger.e(TAG, "Could not create users/${firebaseUser.uid}: $error")
                // Nothing points at the reservation now, so hand the username back.
                if (username.isNotBlank()) remoteUsernames.release(username)
                error.toFailure()
            },
        )
    }

    fun isValidUsername(username:String) : Boolean {
        return usernameFactory.isValid(username)
    }

    suspend fun backfillUsername(uid: String, profile: UserProfile): UserProfile {
        val generated = usernameFactory.createForBackfill(profile) ?: return profile
        val username = claimUsername(uid, profile.name, generated) ?: return profile

        if (!remoteProfiles.updateUsername(uid, username).isSuccess) {
            Logger.w(TAG, "Could not backfill username for users/$uid")
            remoteUsernames.release(username)
            return profile
        }

        Logger.i(TAG, "Backfilled username for users/$uid")
        return profile.copy(username = username)
    }

    private suspend fun claimUsername(uid: String, name: String, first: String? = null): String? {
        var candidate = first ?: usernameFactory.create(name)

        repeat(UsernameFactory.CLAIM_ATTEMPTS) {
            if (remoteUsernames.claim(candidate, uid, name).isSuccess) return candidate
            candidate = usernameFactory.create(name)
        }

        Logger.e(
            TAG,
            "No free username for users/$uid after ${UsernameFactory.CLAIM_ATTEMPTS} tries"
        )
        return null
    }

    companion object {
        private const val TAG = "UserProvisioner"
    }
}
