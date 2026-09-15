package com.geoviksoft.turnia.ui.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit

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

        if (granted) return@LaunchedEffect

        // Android lets an app ask twice before it stops showing the dialog; Turnia asks once.
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED, false)) return@LaunchedEffect
        prefs.edit { putBoolean(KEY_ASKED, true) }
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private const val PREFS = "notification_permission"
private const val KEY_ASKED = "asked"
