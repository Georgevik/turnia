package com.georgevik.turnia.notification

import com.georgevik.turnia.core.domain.repository.FcmDelegate
import com.georgevik.turnia.core.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * What `MessagingDelegate` calls when APNs hands Firebase a new token — the iOS counterpart of
 * `TurniaMessagingService` on Android.
 *
 * Exposed as `PushTokenBridgeKt.onPushTokenRefreshed()` in the `Shared` framework. It reaches into
 * Koin rather than taking arguments because Swift has no container of its own to resolve from.
 */
fun onPushTokenRefreshed() {
    val koin = KoinPlatform.getKoin()
    val uid = koin.get<UserRepository>().loggedUser?.id ?: return

    koin.get<CoroutineScope>().launch { koin.get<FcmDelegate>().registerFcmToken(uid) }
}
