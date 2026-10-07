package com.geoviksoft.turnia

import com.google.firebase.appcheck.debug.InternalDebugSecretProvider
import com.google.firebase.components.Component
import com.google.firebase.components.ComponentRegistrar

/**
 * Hands App Check the debug token the build was given, so it is registered in the console once per
 * developer, from `turnia.appCheckDebugToken` in `local.properties`. Left to itself the SDK mints
 * a random secret into the app's own data, which a clear-data or a reinstall throws away — and a
 * token the console has never seen is refused, which with enforcement on reads as
 * `PERMISSION_DENIED` on every query.
 *
 * `DebugAppCheckProvider` asks the component graph for this before it looks at its own storage, so
 * returning null is what keeps the SDK's behaviour for a developer who has set no token.
 *
 * Discovered through `ComponentDiscoveryService` in the debug manifest, the way every Firebase
 * library registers its own components.
 */
class DebugAppCheckSecretRegistrar : ComponentRegistrar {
    override fun getComponents(): List<Component<*>> = listOf(
        Component.builder(InternalDebugSecretProvider::class.java)
            .factory {
                InternalDebugSecretProvider { BuildConfig.APP_CHECK_DEBUG_TOKEN.ifBlank { null } }
            }
            .build(),
    )
}
