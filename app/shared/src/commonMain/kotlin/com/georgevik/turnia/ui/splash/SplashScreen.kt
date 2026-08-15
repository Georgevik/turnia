package com.georgevik.turnia.ui.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.navigation.Route
import com.georgevik.turnia.ui.system.toErrorSnackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.loading_phrases
import turnia.app.shared.generated.resources.logo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun SplashScreen(
    vm: SplashViewModel = koinViewModel(),
    snackbar: SnackbarHostState,
    onNextScreen: (route: Route) -> Unit
) {
    LaunchedEffect(Unit) {
        vm.uiEvent.collect { event ->
            when (event) {
                is SplashUiEvent.Error -> snackbar.showSnackbar(event.message.toErrorSnackbar())
                SplashUiEvent.NewUser -> onNextScreen(Route.SignInKey)
                SplashUiEvent.UserLoaded -> onNextScreen(Route.AboutKey)
            }
        }
    }

    SplashScreenContent()
}

@Composable
fun SplashScreenContent() {
    val phrases = stringArrayResource(Res.array.loading_phrases)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {

            Image(
                modifier = Modifier.weight(1f).fillMaxWidth(0.5f),
                alignment = Alignment.BottomEnd,
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Logo"
            )
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(100.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(24.dp))
            RotatingLoadingText(phrases = phrases)
        }
    }
}

@Composable
fun RotatingLoadingText(
    modifier: Modifier = Modifier, phrases: List<String>, rotationInterval: Duration = 3.seconds
) {
    if (phrases.isEmpty()) return
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(phrases, rotationInterval) {
        while (isActive) {
            delay(rotationInterval)
            currentIndex = (currentIndex + 1) % phrases.size
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = phrases[currentIndex], transitionSpec = {
                fadeIn() togetherWith fadeOut()
            }
        ) {
            Text(
                text = phrases[currentIndex],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}
