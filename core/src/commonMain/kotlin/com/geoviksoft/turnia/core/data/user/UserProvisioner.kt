package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.data.datasource.firestore.UserPathFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.UsernameFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.core.domain.username.UsernameFactory
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.errorOrNull
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.core.system.isSuccess
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import dev.gitlive.firebase.auth.FirebaseUser


class UserProvisioner(
    private val remoteProfiles: UserPathFirestore,
    private val remotePrivate: UserPrivateFirestore,
    private val remoteUsernames: UsernameFirestore,
    private val usernameFactory: UsernameFactory,
    private val analytics: Analytics,
) {

    suspend fun create(
        firebaseUser: FirebaseUser,
        name: String? = null,
    ): Outcome<UserProfile, UserProfileError> {
        val userId = UserId(firebaseUser.uid)
        Logger.w(TAG, "Empty users/${firebaseUser.uid}. New user")

        remotePrivate.createAccount(userId, firebaseUser.email.orEmpty()).errorOrNull()?.let { error ->
            Logger.e(TAG, "Could not create the private account: $error")
        }

        val name = name?.takeIf { it.isNotBlank() } ?: firebaseUser.displayName.orEmpty()
        val username = claimUsername(userId, name).orEmpty()

        return remoteProfiles.update(
            userId, UserDocument(name = name, username = username)
        ).fold(
            onSuccess = {
                Logger.i(TAG, "Created users/${firebaseUser.uid}")
                analytics.log(AnalyticsEvent.SignUp(signUpMethod(firebaseUser)))
                UserProfile(id = userId, name = name, username = username).toSuccess()
            },
            onFailure = { error ->
                Logger.e(TAG, "Could not create users/${firebaseUser.uid}: $error")
                error.toFailure()
            },
        )
    }

    /**
     * The provider the account was made with, in the words Google's `sign_up` report expects.
     * Android lists Firebase's own `firebase` entry first, so it is skipped.
     */
    private fun signUpMethod(firebaseUser: FirebaseUser): String =
        when (firebaseUser.providerData.firstOrNull { it.providerId != "firebase" }?.providerId) {
            "google.com" -> "google"
            "apple.com" -> "apple"
            "password" -> "email"
            else -> "other"
        }

    fun isValidUsername(username:String) : Boolean {
        return usernameFactory.isValid(username)
    }

    suspend fun backfillUsername(uid: UserId, profile: UserProfile): UserProfile {
        val generated = usernameFactory.createForBackfill(profile) ?: return profile
        val username = claimUsername(uid, profile.name, generated) ?: return profile

        if (!remoteProfiles.updateUsername(uid, username).isSuccess) {
            Logger.w(TAG, "Could not backfill username for users/$uid")
            return profile
        }

        Logger.i(TAG, "Backfilled username for users/$uid")
        return profile.copy(username = username)
    }

    private suspend fun claimUsername(uid: UserId, name: String, first: String? = null): String? {
        var candidate = first ?: usernameFactory.create(name)

        repeat(UsernameFactory.CLAIM_ATTEMPTS) {
            if (remoteUsernames.claim(candidate, uid).isSuccess) return candidate
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
