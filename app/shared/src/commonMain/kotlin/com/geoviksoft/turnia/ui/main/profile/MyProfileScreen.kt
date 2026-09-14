package com.geoviksoft.turnia.ui.main.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.ui.main.profile.components.AvatarPickerSheet
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.components.ConfirmationDialog
import com.geoviksoft.turnia.ui.system.components.ConfirmationStatus
import com.geoviksoft.turnia.ui.system.components.TReadOnlyField
import com.geoviksoft.turnia.ui.system.components.UserAvatar
import com.geoviksoft.turnia.ui.system.keyboardAware
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.profile_avatar_change
import turnia.app.shared.generated.resources.profile_delete_account
import turnia.app.shared.generated.resources.profile_delete_account_confirm
import turnia.app.shared.generated.resources.profile_delete_account_error
import turnia.app.shared.generated.resources.profile_delete_account_error_last_admin
import turnia.app.shared.generated.resources.profile_delete_account_message
import turnia.app.shared.generated.resources.profile_delete_account_title
import turnia.app.shared.generated.resources.profile_error_name_required
import turnia.app.shared.generated.resources.profile_error_save
import turnia.app.shared.generated.resources.profile_error_username_invalid
import turnia.app.shared.generated.resources.profile_error_username_taken
import turnia.app.shared.generated.resources.profile_field_email
import turnia.app.shared.generated.resources.profile_field_name
import turnia.app.shared.generated.resources.profile_field_username
import turnia.app.shared.generated.resources.profile_save
import turnia.app.shared.generated.resources.profile_title

@Composable
fun MyProfileScreen(viewModel: MyProfileViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyProfileScreenContent(
        uiState = uiState,
        onUsernameChanged = viewModel::onUsernameChanged,
        onNameChanged = viewModel::onNameChanged,
        onSnackbarShown = viewModel::userMessageShown,
        onAvatarClicked = viewModel::onAvatarClicked,
        onAvatarPickerDismissed = viewModel::onAvatarPickerDismissed,
        onAnimalPicked = viewModel::onAnimalPicked,
        onBackgroundPicked = viewModel::onBackgroundPicked,
        onSave = viewModel::onSave,
        onDeleteAccountConfirmed = viewModel::onDeleteAccountConfirmed,
    )
}

@Composable
private fun MyProfileScreenContent(
    uiState: MyProfileUi,
    onNameChanged: (String) -> Unit = {},
    onUsernameChanged: (String) -> Unit = {},
    onSnackbarShown: () -> Unit = {},
    onAvatarClicked: () -> Unit = {},
    onAvatarPickerDismissed: () -> Unit = {},
    onAnimalPicked: (String) -> Unit = {},
    onBackgroundPicked: (String) -> Unit = {},
    onSave: () -> Unit = {},
    onDeleteAccountConfirmed: () -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current
    var confirmingDelete by remember { mutableStateOf(false) }

    uiState.userMessage?.let { message ->
        val text = message.message()
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            onSnackbarShown()
        }
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) navigator.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = navigator::goBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.calendar_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().keyboardAware(innerPadding).padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            UserAvatar(
                avatar = UserProfile.AnimalAvatar(
                    animal = uiState.animalIconId,
                    background = uiState.backgroundColor,
                ),
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable(
                        onClick = onAvatarClicked,
                        onClickLabel = stringResource(Res.string.profile_avatar_change),
                    ),
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                label = { Text(stringResource(Res.string.profile_field_name)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Words
                ),
                singleLine = true,
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it.message()) } },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.username,
                onValueChange = onUsernameChanged,
                label = { Text(stringResource(Res.string.profile_field_username)) },
                prefix = { Text("@") },
                singleLine = true,
                isError = uiState.usernameError != null,
                supportingText = uiState.usernameError?.let { { Text(it.message()) } },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            TReadOnlyField(
                label = stringResource(Res.string.profile_field_email),
                value = uiState.email,
            )

            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                shape = RoundedCornerShape(percent = 50),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (uiState.saving) {
                    CircularProgressIndicator()
                } else {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(Res.string.profile_save),
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            DeleteAccountButton(
                onClick = { confirmingDelete = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }

    // Kept up while the deletion runs, spinner and all: closing it would suggest it was over.
    if (confirmingDelete || uiState.deletingAccount) {
        ConfirmationDialog(
            title = stringResource(Res.string.profile_delete_account_title),
            message = stringResource(Res.string.profile_delete_account_message),
            confirmText = stringResource(Res.string.profile_delete_account_confirm),
            dismissText = stringResource(Res.string.dialog_cancel),
            onConfirm = {
                confirmingDelete = false
                onDeleteAccountConfirmed()
            },
            onDismissRequest = { confirmingDelete = false },
            status = if (uiState.deletingAccount) ConfirmationStatus.Running else ConfirmationStatus.Idle,
        )
    }

    if (uiState.pickingAvatar) {
        AvatarPickerSheet(
            animalIconId = uiState.animalIconId,
            backgroundColor = uiState.backgroundColor,
            onAnimalPicked = onAnimalPicked,
            onColorPicked = onBackgroundPicked,
            onDismiss = onAvatarPickerDismissed,
        )
    }
}

@Composable
private fun DeleteAccountButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.DeleteForever,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(Res.string.profile_delete_account),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun ProfileFieldError.message(): String = stringResource(
    when (this) {
        ProfileFieldError.NameRequired -> Res.string.profile_error_name_required
        ProfileFieldError.UsernameInvalid -> Res.string.profile_error_username_invalid
        ProfileFieldError.UsernameTaken -> Res.string.profile_error_username_taken
    }
)

@Composable
private fun ProfileMessage.message(): String = stringResource(
    when (this) {
        ProfileMessage.SaveFailed -> Res.string.profile_error_save
        ProfileMessage.DeleteAccountLastAdmin -> Res.string.profile_delete_account_error_last_admin
        ProfileMessage.DeleteAccountFailed -> Res.string.profile_delete_account_error
    }
)

@Preview
@Composable
fun MyProfileScreenPreview() {
    PreviewTurniaTheme {
        MyProfileScreenContent(
            uiState = MyProfileUi(
                name = "John Due", username = "Georgevik", email = "myemail@domain.com"
            )
        )
    }
}
