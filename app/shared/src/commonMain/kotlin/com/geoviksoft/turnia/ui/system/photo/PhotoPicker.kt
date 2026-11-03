package com.geoviksoft.turnia.ui.system.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/** What the picker hands back. */
sealed interface PickedPhoto {
    data object Cancelled : PickedPhoto

    class Image(val bytes: ByteArray) : PickedPhoto

    /** Something was picked, but it could not be read. */
    data object Unreadable : PickedPhoto
}

/**
 * The device's photos, one image at a time; the system picker asks for no permission on either
 * platform. The result goes to the handler the launcher was remembered with, on every composition:
 * an activity recreated while the picker was up still gets it.
 */
interface PhotoPicker {
    @Composable
    fun rememberLauncher(onPicked: (PickedPhoto) -> Unit): () -> Unit
}

object SystemPhotoPicker : PhotoPicker {
    @Composable
    override fun rememberLauncher(onPicked: (PickedPhoto) -> Unit): () -> Unit = rememberSystemPhotoLauncher(onPicked)
}

@Composable
internal expect fun rememberSystemPhotoLauncher(onPicked: (PickedPhoto) -> Unit): () -> Unit

/**
 * The picker screens use, provided at the root. A local so a test can swap in one that returns its
 * own image: the system picker is out of its reach.
 */
val LocalPhotoPicker: ProvidableCompositionLocal<PhotoPicker> = staticCompositionLocalOf {
    error("LocalPhotoPicker not provided — this content must be hosted under the app's root")
}

/** For previews: nothing is ever picked. */
object NoPhotoPicker : PhotoPicker {
    @Composable
    override fun rememberLauncher(onPicked: (PickedPhoto) -> Unit): () -> Unit =
        remember(onPicked) { { onPicked(PickedPhoto.Cancelled) } }
}
