package com.georgevik.turnia.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.ui.main.changes.ChangesScreen
import com.georgevik.turnia.ui.main.group.GroupScreen
import com.georgevik.turnia.ui.main.mycalendar.MyCalendarScreen
import com.georgevik.turnia.ui.main.profile.ProfileScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainNavBarScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = koinViewModel(),
) {
    val featureFlags by viewModel.featureFlags.collectAsStateWithLifecycle()
    // The Swap ("Cambios") tab is gated behind a feature flag.
    val tabs = MainTab.entries.filter { it != MainTab.CHANGES || featureFlags.showSwapTab }

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.MY_CALENDAR) }
    // If the selected tab gets hidden (flag off), fall back to the calendar.
    if (selectedTab !in tabs) selectedTab = MainTab.MY_CALENDAR

    Scaffold(
        contentWindowInsets = WindowInsets(0,0,0,0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(text = tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // innerPadding aplica automáticamente el offset para que el contenido
        // no quede oculto detrás de la barra inferior
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.MY_CALENDAR -> MyCalendarScreen()
                MainTab.GROUPS -> GroupScreen()
                MainTab.CHANGES -> ChangesScreen()
                MainTab.PROFILE -> ProfileScreen()
            }
        }
    }
}
