package com.georgevik.turnia.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 back-stack keys. Each key identifies a destination and is
 * `@Serializable` so the back stack survives configuration changes and
 * process death via `rememberNavBackStack`.
 */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object SignInKey : Route

    @Serializable
    data object AboutKey : Route

    @Serializable
    data object SpashKey : Route
}
