package com.georgevik.turnia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.GreetingViewModel
import com.mmk.kmpauth.google.rememberGoogleAuthState
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(viewModel: GreetingViewModel = koinViewModel(), onOpenAbout: () -> Unit) {
    val googleAuth = rememberGoogleAuthState(onResult = viewModel::onSignInResult)

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GoogleSignInButton(
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { googleAuth.launch() }

        viewModel.signedInUser?.let { Text("Signed in: ${it.uid}") }
        viewModel.lastSignInError?.let { Text("Error: $it") }

        Button(onClick = onOpenAbout) {
            Text("Go to About")
        }
    }
}
