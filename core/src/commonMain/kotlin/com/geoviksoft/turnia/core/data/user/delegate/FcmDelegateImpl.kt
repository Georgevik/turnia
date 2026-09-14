package com.geoviksoft.turnia.core.data.user.delegate

import com.geoviksoft.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.FcmDelegate
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.errorOrNull
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.system.valueOrNull
import dev.gitlive.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FcmDelegateImpl(
    private val messaging: FirebaseMessaging,
    private val remotePrivate: UserPrivateFirestore,
) : FcmDelegate {

    private val _notificationsEnabled = MutableStateFlow(true)
    override val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    /**
     * A new token is registered from two places at once on iOS — the restored session and FCM's
     * refresh callback — and both read the account before either has written, so each would add it.
     */
    private val registration = Mutex()
    private var registered: Pair<UserId, String>? = null

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
        registration.withLock {
            val token = getFirebaseMessagingToken() ?: return

            remotePrivate.removeFcmToken(uid, token).errorOrNull()?.let { error ->
                Logger.e(TAG, "Could not unregister the push token: $error")
                return
            }
            registered = null

            outcomeCatching(TAG, { it }) { messaging.deleteToken() }
        }
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

    private suspend fun registerToken(uid: UserId, onAccount: List<String> = emptyList()) {
        registration.withLock {
            val token = getFirebaseMessagingToken() ?: return
            if (registered == uid to token || token in onAccount) {
                registered = uid to token
                Logger.d(TAG, "This device is already registered")
                return
            }

            remotePrivate.addFcmToken(uid, token).errorOrNull()?.let { error ->
                Logger.e(TAG, "Could not register the push token: $error")
                return
            }
            registered = uid to token
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
