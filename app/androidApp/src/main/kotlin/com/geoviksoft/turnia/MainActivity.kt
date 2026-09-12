package com.geoviksoft.turnia

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.geoviksoft.turnia.ui.root.App
import org.koin.androidx.viewmodel.ext.android.viewModel

// AppCompat for the per-app language: below Android 13 only an AppCompatActivity applies it.
class MainActivity : AppCompatActivity() {

    private val vm: MainActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        vm.openNotification(intent)
        // A recreated activity gets the same intent back, and the join sheet would open again.
        if (savedInstanceState == null) {
            vm.openLink(intent)
        }
        vm.readInstallReferrer(applicationContext)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.openNotification(intent)
        vm.openLink(intent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
