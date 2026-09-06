package com.georgevik.turnia.core.data.user.delegate

import com.georgevik.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.repository.FcmDelegate
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.valueOrNull
import dev.gitlive.firebase.messaging.FirebaseMessaging

class FcmDelegateImpl(
    private val messaging: FirebaseMessaging,
    private val remotePrivate: UserPrivateFirestore,
) : FcmDelegate {

    override suspend fun registerFcmToken(uid: UserId) {
        val token = token() ?: return

        remotePrivate.addFcmToken(uid, token).errorOrNull()?.let { error ->
            Logger.e(TAG, "Could not register the push token: $error")
        }
    }

    /**
     * Called before signing out, while the uid is still known.
     *
     * The token has to go, or this device keeps receiving pushes meant for an account no longer on
     * it. `deleteToken` after Firestore and not before: a token dropped locally but left in the
     * list is one nobody can ever clean up from here again.
     */
    override suspend fun unregisterFcmToken(uid: UserId) {
        val token = token() ?: return

        remotePrivate.removeFcmToken(uid, token).errorOrNull()?.let { error ->
            Logger.e(TAG, "Could not unregister the push token: $error")
            return
        }

        outcomeCatching(TAG, { it }) { messaging.deleteToken() }
    }

    /**
     * On iOS this fails until APNs has handed Firebase its own token, which only happens once the
     * user has allowed notifications; on Android it fails where Play Services is missing.
     */
    private suspend fun token(): String? =
        outcomeCatching(TAG, { it }) { messaging.getToken() }
            .also { outcome ->
                outcome.errorOrNull()?.let { Logger.w(TAG, "No push token yet: ${it.message}") }
            }
            .valueOrNull()

    companion object {
        private const val TAG = "PushTokenRegistrar"
    }
}
