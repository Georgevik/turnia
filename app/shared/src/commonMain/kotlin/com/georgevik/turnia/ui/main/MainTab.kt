package com.georgevik.turnia.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val title: String,
    val icon: ImageVector
) {
    MY_CALENDAR("Calendario", Icons.Default.CalendarMonth),
    GROUPS("Grupos", Icons.Default.Groups),
    CHANGES("Cambios", Icons.Default.SwapHoriz),
    PROFILE("Perfil", Icons.Default.Person)
}
