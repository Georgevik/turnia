package com.georgevik.turnia.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import kotlinx.serialization.PolymorphicSerializer

@Composable
fun rememberMainNavigationState(startRoute: NavKey, topLevelRoutes: Set<NavKey>): MainNavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        stateSerializer = PolymorphicSerializer(NavKey::class),
        configuration = mainRouteSavedStateConfiguration,
    ) { mutableStateOf(startRoute) }

    val backStacks = topLevelRoutes.associateWith { key ->
        rememberNavBackStack(mainRouteSavedStateConfiguration, key)
    }

    return remember(startRoute, topLevelRoutes) {
        MainNavigationState(startRoute = startRoute, topLevelRoute = topLevelRoute, backStacks = backStacks)
    }
}

class MainNavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute

    @Composable
    fun toDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
        val decoratedEntries = backStacks.mapValues { (_, stack) ->
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider,
            )
        }
        return topLevelRoutesInUse().flatMap { decoratedEntries[it] ?: emptyList() }
    }

    private fun topLevelRoutesInUse(): List<NavKey> =
        if (topLevelRoute == startRoute) listOf(startRoute) else listOf(startRoute, topLevelRoute)
}
