package com.geoviksoft.turnia.navigation.root.routes

import androidx.navigation3.runtime.NavKey
import com.geoviksoft.turnia.core.domain.model.ShiftSetupVia
import com.geoviksoft.turnia.navigation.routes.EventTypeDetailData
import kotlinx.serialization.Serializable

@Serializable
sealed interface RootRoute : NavKey {
    @Serializable
    data object SplashKey : RootRoute

    @Serializable
    data object OnboardingKey : RootRoute

    @Serializable
    data object SignInKey : RootRoute

    @Serializable
    data object CreateAccountKey : RootRoute

    @Serializable
    data object MainKey : RootRoute

    @Serializable
    data class EventTypeDetailKey(val data: EventTypeDetailData) : RootRoute

    @Serializable
    data object PersonalEventTypesKey : RootRoute

    @Serializable
    data object MyProfileKey : RootRoute

    @Serializable
    data object PreferencesKey : RootRoute

    @Serializable
    data object AboutKey : RootRoute

    @Serializable
    data class ShiftSetupKey(val via: ShiftSetupVia) : RootRoute

}
