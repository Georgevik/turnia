package com.geoviksoft.turnia.ui.main.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.navigation.LocalRootNavigator
import com.geoviksoft.turnia.navigation.root.routes.RootRoute
import com.geoviksoft.turnia.ui.system.LocalPaddings
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.components.ConfirmationDialog
import com.geoviksoft.turnia.ui.system.components.UserAvatar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.profile_logout
import turnia.app.shared.generated.resources.profile_logout_dialog_message
import turnia.app.shared.generated.resources.profile_logout_dialog_title
import turnia.app.shared.generated.resources.profile_my_events
import turnia.app.shared.generated.resources.settings_about
import turnia.app.shared.generated.resources.settings_my_profile
import turnia.app.shared.generated.resources.settings_plan_free
import turnia.app.shared.generated.resources.settings_plan_free_body
import turnia.app.shared.generated.resources.settings_plan_premium
import turnia.app.shared.generated.resources.settings_plan_premium_body
import turnia.app.shared.generated.resources.settings_preferences

/**
 * Settings tab
 */

@Composable
fun SettingsMenuScreen(vm: SettingsMenuViewModel = koinViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    SettingsMenuScreenContent(uiState, vm::onLogoutClicked)
}

@Composable
private fun SettingsMenuScreenContent(uiState: SettingsMenuUi, onLogoutClicked: () -> Unit = {}) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val rootNavigator = LocalRootNavigator.current
    val details = uiState.userDetails

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { rootNavigator.goTo(RootRoute.MyProfileKey) },
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            UserAvatar(
                avatar = details?.avatar ?: UserProfile.AnimalAvatar.NONE,
                modifier = Modifier.size(64.dp),
            )

            Column {
                Text(
                    text = details?.displayName.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                details?.username?.takeIf { it.isNotBlank() }?.let { username ->
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
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
                    if (details?.isPremium == true) {
                        Text(
                            stringResource(Res.string.settings_plan_premium),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(Res.string.settings_plan_premium_body),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            stringResource(Res.string.settings_plan_free),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(Res.string.settings_plan_free_body),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        ProfileRow(Icons.Default.Person, stringResource(Res.string.settings_my_profile)) {
            rootNavigator.goTo(RootRoute.MyProfileKey)
        }
        ProfileRow(Icons.Default.Event, stringResource(Res.string.profile_my_events)) {
            rootNavigator.goTo(RootRoute.PersonalEventTypesKey)
        }
        ProfileRow(Icons.Default.Tune, stringResource(Res.string.settings_preferences)) {
            rootNavigator.goTo(RootRoute.PreferencesKey)
        }
        ProfileRow(Icons.Default.Info, stringResource(Res.string.settings_about)) {
            rootNavigator.goTo(RootRoute.AboutKey)
        }
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
                onLogoutClicked()
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

@Preview
@Composable
fun SettingsMenuScreenPreview() {
    PreviewTurniaTheme {
        Box(modifier = Modifier.background(Color.White)) {
            SettingsMenuScreenContent(
                uiState = SettingsMenuUi(
                    userDetails = SettingsMenuUi.UserDetails(
                        displayName = "John Due",
                        username = "Georgevik",
                        isPremium = false,
                        avatar = UserProfile.AnimalAvatar(animal = "duck", background = "#F4B400"),
                    )
                )
            )
        }
    }
}
