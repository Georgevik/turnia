package com.geoviksoft.turnia

import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Catches a token the system rotates while the app is running.
 *
 * Without this the new token is only picked up on the next launch, and until then every push goes
 * to an address that no longer exists. Displaying the notifications is left to the SDK's own
 * default handling — overriding `onMessageReceived` would take that over for every message.
 */
class TurniaMessagingService : FirebaseMessagingService(), KoinComponent {

    private val users: UserRepository by inject()
    private val scope: CoroutineScope by inject()

    override fun onNewToken(token: String) {
        val uid = users.loggedUser?.id ?: return

        // The registrar reads the token itself, which is now the new one. The old one stays in the
        // list until a send reports it dead and the server drops it.
        scope.launch { users.registerFcmToken(uid) }
    }
}
