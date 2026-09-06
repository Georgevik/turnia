package com.georgevik.turnia.ui.system

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun RequestNotificationPermission() {
    // Below 33 the permission does not exist and notifications are on unless the user turned them
    // off in the system settings, which is not something the app can ask about.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        // Asking again after a denial shows nothing at all from the second refusal onwards, so the
        // check is what keeps this from being a silent no-op the app cannot distinguish.
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
