package com.georgevik.turnia.ui.main.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.navigation.LocalRootNavigator
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.ui.system.LocalPaddings
import com.georgevik.turnia.ui.system.components.ConfirmationDialog
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.profile_logout
import turnia.app.shared.generated.resources.profile_logout_dialog_message
import turnia.app.shared.generated.resources.profile_logout_dialog_title
import turnia.app.shared.generated.resources.profile_my_events

/**
 * "Perfil" tab — a draft account screen: the signed-in user header plus entry
 * points to groups, shared calendars, subscription and settings.
 */
@Composable
fun ProfileScreen(vm: ProfileViewModel = koinViewModel()) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val rootNavigator = LocalRootNavigator.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
            Column {
                Text(
                    text = "Jorge Vik",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "jorgevik@gmail.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = LocalPaddings.current.S),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = null)
                Column {
                    Text("Plan gratuito", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Hazte Premium para quitar los anuncios.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        ProfileRow(Icons.Default.Event, stringResource(Res.string.profile_my_events)) {
            rootNavigator.goTo(RootRoute.PersonalEventTypesKey)
        }
        ProfileRow(Icons.Default.Group, "Grupos")
        ProfileRow(Icons.Default.CalendarMonth, "Calendarios compartidos")
        ProfileRow(Icons.Default.WorkspacePremium, "Suscripción")
        ProfileRow(Icons.Default.Settings, "Ajustes")
        ProfileRow(Icons.AutoMirrored.Filled.Logout, stringResource(Res.string.profile_logout)) {
            showLogoutDialog = true
        }
    }

    if (showLogoutDialog) {
        ConfirmationDialog(
            title = stringResource(Res.string.profile_logout_dialog_title),
            message = stringResource(Res.string.profile_logout_dialog_message),
            confirmText = stringResource(Res.string.profile_logout),
            dismissText = stringResource(Res.string.dialog_cancel),
            onConfirm = {
                showLogoutDialog = false
                vm.onLogoutClicked()
            },
            onDismissRequest = { showLogoutDialog = false },
        )
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
