package com.geoviksoft.turnia.ui.system.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The system Photo Picker, images only; below Android 11 it falls back to the document picker. The
 * activity result is delivered to the launcher registered on the current composition, so a result
 * that arrives after the activity was recreated still finds its handler.
 */
@Composable
internal actual fun rememberSystemPhotoLauncher(onPicked: (PickedPhoto) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val handler by rememberUpdatedState(onPicked)

    val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult handler(PickedPhoto.Cancelled)

        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            }
            handler(bytes?.let(PickedPhoto::Image) ?: PickedPhoto.Unreadable)
        }
    }

    return remember(launcher) { { launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) } }
}
