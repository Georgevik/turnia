package com.geoviksoft.turnia.ui.system.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
actual fun rememberPhotoPicker(): PhotoPicker = remember { IosPhotoPicker() }

/**
 * `PHPickerViewController`, one image. The item is loaded as `public.jpeg`, which has the system
 * convert a HEIC photo on the way: Skia, which decodes images in Compose, cannot read HEIC.
 */
private class IosPhotoPicker : PhotoPicker {

    // The controller holds its delegate weakly: this keeps it alive while the picker is up.
    private var delegate: PickerDelegate? = null

    override fun pick(onPicked: (ByteArray?) -> Unit) {
        val configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
        }
        val picker = PHPickerViewController(configuration = configuration)
        val pickerDelegate = PickerDelegate { bytes ->
            delegate = null
            onPicked(bytes)
        }
        delegate = pickerDelegate
        picker.delegate = pickerDelegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
            ?: run { delegate = null; onPicked(null) }
    }

    private fun topViewController(): UIViewController? {
        @Suppress("DEPRECATION")
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        return top
    }
}

private class PickerDelegate(private val done: (ByteArray?) -> Unit) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return done(null)

        result.itemProvider.loadDataRepresentationForTypeIdentifier(JPEG_TYPE) { data, _ ->
            val bytes = data?.toByteArray()
            dispatch_async(dispatch_get_main_queue()) { done(bytes) }
        }
    }
}

// A top-level constant: an Objective-C subclass's companion may hold no fields.
private const val JPEG_TYPE = "public.jpeg"

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}
