package com.geoviksoft.turnia.ui.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.navigation.root.routes.RootRoute
import com.geoviksoft.turnia.ui.signin.components.EmailSignInSheet
import com.geoviksoft.turnia.ui.signin.components.SignInButton
import com.geoviksoft.turnia.ui.signin.components.SignInProvider
import com.geoviksoft.turnia.ui.signin.model.SignInError
import com.geoviksoft.turnia.ui.signin.model.SignInUi
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.components.TurniaLogo
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import com.mmk.kmpauth.apple.rememberAppleAuthState
import com.mmk.kmpauth.google.rememberGoogleAuthState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.app_name
import turnia.app.shared.generated.resources.signin_apple
import turnia.app.shared.generated.resources.signin_email
import turnia.app.shared.generated.resources.signin_error_failed
import turnia.app.shared.generated.resources.signin_google
import turnia.app.shared.generated.resources.welcome_body
import turnia.app.shared.generated.resources.welcome_feature_clear_body
import turnia.app.shared.generated.resources.welcome_feature_clear_title
import turnia.app.shared.generated.resources.welcome_feature_notify_body
import turnia.app.shared.generated.resources.welcome_feature_notify_title
import turnia.app.shared.generated.resources.welcome_feature_trace_body
import turnia.app.shared.generated.resources.welcome_feature_trace_title
import turnia.app.shared.generated.resources.welcome_headline
import turnia.app.shared.generated.resources.welcome_signin_hint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(viewModel: SignInViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val navigator = LocalNavigator.current

    uiState.userMessage?.let { message ->
        val text = message.message()
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.userMessageShown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoAndAppName()

            Spacer(Modifier.height(8.dp))

            Welcome()
        }

        FeatureSection()

        SignInSection(uiState, viewModel)
    }

    uiState.emailForm?.let { form ->
        ModalBottomSheet(
            onDismissRequest = viewModel::onEmailSignInDismissed,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            EmailSignInSheet(
                form = form,
                onEmailChange = viewModel::onEmailChanged,
                onPasswordChange = viewModel::onPasswordChanged,
                onSubmit = viewModel::onEmailSubmit,
                onForgotPassword = viewModel::onPasswordResetRequested,
                onCreateAccount = {
                    viewModel.onEmailSignInDismissed()
                    navigator.goTo(RootRoute.CreateAccountKey)
                },
            )
        }
    }
}

@Composable
private fun LogoAndAppName(modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TurniaLogo(Modifier.size(88.dp))

        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun Welcome(modifier: Modifier = Modifier) {
    Column(modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(Res.string.welcome_headline),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(Res.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FeatureSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FeatureRow(
            icon = Icons.Default.SwapHoriz,
            title = stringResource(Res.string.welcome_feature_trace_title),
            body = stringResource(Res.string.welcome_feature_trace_body),
        )
        FeatureRow(
            icon = Icons.Default.VerifiedUser,
            title = stringResource(Res.string.welcome_feature_clear_title),
            body = stringResource(Res.string.welcome_feature_clear_body),
        )
        FeatureRow(
            icon = Icons.Default.NotificationsActive,
            title = stringResource(Res.string.welcome_feature_notify_title),
            body = stringResource(Res.string.welcome_feature_notify_body),
        )
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val SignInButtonHeight = 52.dp

@Composable
private fun SignInSection(uiState: SignInUi, viewModel: SignInViewModel) {
    // Google and Apple answer through the same callback: whichever the user picks, what comes back
    // is a session, and the screen has nothing left to decide.
    val googleAuth = rememberGoogleAuthState(onResult = viewModel::onSignInResult)
    val appleAuth = rememberAppleAuthState(onResult = viewModel::onSignInResult)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(Res.string.welcome_signin_hint),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        if (uiState.signingIn) {
            Box(
                modifier = Modifier.fillMaxWidth().height(SignInButtonHeight),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SignInButton(
                    provider = SignInProvider.Google,
                    text = stringResource(Res.string.signin_google),
                    onClick = {
                        viewModel.onSignInStarted()
                        googleAuth.launch()
                    },
                    modifier = Modifier.fillMaxWidth().height(SignInButtonHeight),
                )

                SignInButton(
                    provider = SignInProvider.Apple,
                    text = stringResource(Res.string.signin_apple),
                    onClick = {
                        viewModel.onSignInStarted()
                        appleAuth.launch()
                    },
                    modifier = Modifier.fillMaxWidth().height(SignInButtonHeight),
                )

                SignInButton(
                    provider = SignInProvider.Email,
                    text = stringResource(Res.string.signin_email),
                    onClick = viewModel::onEmailSignInOpened,
                    modifier = Modifier.fillMaxWidth().height(SignInButtonHeight),
                )
            }
        }
    }
}

@Composable
private fun SignInError.message(): String = stringResource(
    when (this) {
        SignInError.Failed -> Res.string.signin_error_failed
    }
)
