package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.pm.PackageInfoCompat

@Composable
actual fun rememberAppVersion(): AppVersion {
    val context = LocalContext.current

    return remember(context) {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        AppVersion(
            name = info.versionName.orEmpty(),
            build = PackageInfoCompat.getLongVersionCode(info).toString(),
        )
    }
}
