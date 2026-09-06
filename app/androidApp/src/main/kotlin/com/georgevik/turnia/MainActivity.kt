package com.georgevik.turnia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.georgevik.turnia.core.domain.repository.NotificationRepository
import com.georgevik.turnia.ui.root.App
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val notifications: NotificationRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openNotification(intent)

        setContent {
            App()
        }
    }

    /** The app was already open, so the tap is delivered here instead — see `singleTop`. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openNotification(intent)
    }

    /**
     * Hands the message's data payload to the domain, which decides whether it leads anywhere.
     */
    private fun openNotification(intent: Intent?) {
        val extras = intent?.extras ?: return

        notifications.opened(
            extras.keySet().mapNotNull { key ->
                extras.getString(key)?.let { value -> key to value }
            }.toMap()
        )
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
