package com.geoviksoft.turnia.navigation.main

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
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import com.geoviksoft.turnia.navigation.main.routes.MainRoute

@Composable
fun rememberMainNavigationState(
    startRoute: MainRoute,
    topLevelRoutes: Set<MainRoute>
): MainNavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        stateSerializer = MainRoute.serializer(),
    ) { mutableStateOf(startRoute) }

    val backStacks: Map<MainRoute, NavBackStack<MainRoute>> = topLevelRoutes.associateWith { key ->
        rememberSerializable(
            serializer = NavBackStackSerializer(MainRoute.serializer()),
        ) { NavBackStack(key) }
    }

    return remember(startRoute, topLevelRoutes) {
        MainNavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }
}

class MainNavigationState(
    val startRoute: MainRoute,
    topLevelRoute: MutableState<MainRoute>,
    val backStacks: Map<MainRoute, NavBackStack<MainRoute>>,
) {
    var topLevelRoute: MainRoute by topLevelRoute

    @Composable
    fun toDecoratedEntries(entryProvider: (MainRoute) -> NavEntry<MainRoute>): List<NavEntry<MainRoute>> {
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

    private fun topLevelRoutesInUse(): List<MainRoute> =
        if (topLevelRoute == startRoute) listOf(startRoute) else listOf(startRoute, topLevelRoute)
}
