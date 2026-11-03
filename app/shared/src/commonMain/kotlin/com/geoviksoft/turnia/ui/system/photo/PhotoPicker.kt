package com.geoviksoft.turnia.ui.system.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Picks one image from the device's photos and hands back its encoded bytes, or null when the user
 * cancels. The system picker asks for no permission on either platform.
 */
fun interface PhotoPicker {
    fun pick(onPicked: (ByteArray?) -> Unit)
}

@Composable
expect fun rememberPhotoPicker(): PhotoPicker

/**
 * The picker screens use, provided at the root. A local rather than [rememberPhotoPicker] at the
 * call site so a test can swap in one that returns its own image: the system picker is out of its
 * reach.
 */
val LocalPhotoPicker: ProvidableCompositionLocal<PhotoPicker> = staticCompositionLocalOf {
    error("LocalPhotoPicker not provided — this content must be hosted under the app's root")
}

/** For previews: nothing is ever picked. */
val NoPhotoPicker = PhotoPicker { onPicked -> onPicked(null) }
