package com.georgevik.turnia.core.data.user.delegate

import com.georgevik.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.repository.FcmDelegate
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.valueOrNull
import dev.gitlive.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FcmDelegateImpl(
    private val messaging: FirebaseMessaging,
    private val remotePrivate: UserPrivateFirestore,
) : FcmDelegate {

    private val _notificationsEnabled = MutableStateFlow(true)
    override val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    /**
     * Reads the account, then registers this device unless the user has switched notifications off.
     */
    override suspend fun registerFcmToken(uid: UserId) {
        val account = remotePrivate.fetchAccount(uid).valueOrNull()
        _notificationsEnabled.value = account?.notificationsEnabled != false

        if (!_notificationsEnabled.value) {
            Logger.i(TAG, "Notifications are off for this account, leaving the device unregistered")
            return
        }

        registerToken(uid, account?.fcmTokens.orEmpty())
    }

    /**
     * Called before signing out, while the uid is still known.
     */
    override suspend fun unregisterFcmToken(uid: UserId) {
        val token = getFirebaseMessagingToken() ?: return

        remotePrivate.removeFcmToken(uid, token).errorOrNull()?.let { error ->
            Logger.e(TAG, "Could not unregister the push token: $error")
            return
        }

        outcomeCatching(TAG, { it }) { messaging.deleteToken() }
    }

    override suspend fun setNotificationsEnabled(
        uid: UserId,
        enabled: Boolean
    ): Outcome<Unit, Unit> {
        val previous = _notificationsEnabled.value
        _notificationsEnabled.value = enabled

        remotePrivate.setNotificationsEnabled(uid, enabled).errorOrNull()?.let { error ->
            Logger.e(TAG, "Could not save the notification setting: $error")
            _notificationsEnabled.value = previous
            return Unit.toFailure()
        }

        // The preference is the account's answer; the token is what makes it true of this device.
        // Second, and unchecked: a token left behind only means a notification the user asked not
        // to get, while a preference that did not save would put the switch back on its own.
        if (enabled) registerToken(uid) else unregisterFcmToken(uid)

        return Unit.toSuccess()
    }

    private suspend fun registerToken(uid: UserId, registered: List<String> = emptyList()) {
        val token = getFirebaseMessagingToken() ?: return
        if (token in registered) {
            Logger.d(TAG, "This device is already registered")
            return
        }

        remotePrivate.addFcmToken(uid, token).errorOrNull()?.let { error ->
            Logger.e(TAG, "Could not register the push token: $error")
        }
    }

    /**
     * On iOS this fails until APNs has handed Firebase its own token, which only happens once the
     * user has allowed notifications; on Android it fails where Play Services is missing.
     */
    private suspend fun getFirebaseMessagingToken(): String? =
        outcomeCatching(TAG, { it }) { messaging.getToken() }
            .also { outcome ->
                outcome.errorOrNull()?.let { Logger.w(TAG, "No push token yet: ${it.message}") }
            }
            .valueOrNull()

    companion object {
        private const val TAG = "FcmDelegate"
    }
}
