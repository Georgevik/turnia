package com.geoviksoft.turnia.ui.system.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

@Composable
internal actual fun rememberSystemPhotoLauncher(onPicked: (PickedPhoto) -> Unit): () -> Unit {
    val handler by rememberUpdatedState(onPicked)
    val picker = remember { IosPhotoPicker() }
    return remember(picker) { { picker.pick { handler(it) } } }
}

/**
 * `PHPickerViewController`, one image. The item is loaded as whatever image it is — HEIC, JPEG, PNG
 * — and decoded by UIKit later, which reads all of them.
 */
private class IosPhotoPicker {

    // The controller holds its delegate weakly: this keeps it alive while the picker is up.
    private var delegate: PickerDelegate? = null

    fun pick(onPicked: (PickedPhoto) -> Unit) {
        val configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
        }
        val picker = PHPickerViewController(configuration = configuration)
        val pickerDelegate = PickerDelegate { picked ->
            delegate = null
            onPicked(picked)
        }
        delegate = pickerDelegate
        picker.delegate = pickerDelegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
            ?: run { delegate = null; onPicked(PickedPhoto.Cancelled) }
    }

    private fun topViewController(): UIViewController? {
        @Suppress("DEPRECATION")
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        return top
    }
}

private class PickerDelegate(private val done: (PickedPhoto) -> Unit) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return done(PickedPhoto.Cancelled)

        result.itemProvider.loadDataRepresentationForTypeIdentifier(IMAGE_TYPE) { data, _ ->
            val picked = data?.toByteArray()?.let(PickedPhoto::Image) ?: PickedPhoto.Unreadable
            dispatch_async(dispatch_get_main_queue()) { done(picked) }
        }
    }
}

// A top-level constant: an Objective-C subclass's companion may hold no fields.
private const val IMAGE_TYPE = "public.image"

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}
