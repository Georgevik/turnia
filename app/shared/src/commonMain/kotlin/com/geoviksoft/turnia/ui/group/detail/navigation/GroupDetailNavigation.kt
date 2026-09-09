package com.geoviksoft.turnia.ui.group.detail.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.ui.group.detail.GroupDetailScreen
import com.geoviksoft.turnia.ui.group.detail.GroupDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<MainRoute>.groupDetailNavigation() {
    entry<MainRoute.GroupDetail> { key ->
        GroupDetailScreen(
            viewModel = koinViewModel<GroupDetailViewModel> { parametersOf(key) },
        )
    }
}
