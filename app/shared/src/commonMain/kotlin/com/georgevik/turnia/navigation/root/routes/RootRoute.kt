package com.georgevik.turnia.navigation.root.routes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface RootRoute : NavKey {
    @Serializable
    data object SplashKey : RootRoute

    @Serializable
    data object SignInKey : RootRoute

    @Serializable
    data object MainKey : RootRoute
}
