package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.geoviksoft.turnia.core.system.BuildInfo

// Defaults to release so a preview, which has no Koin, draws what users see.
val LocalBuildInfo: ProvidableCompositionLocal<BuildInfo> = staticCompositionLocalOf {
    BuildInfo(isDebug = false)
}
