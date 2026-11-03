package com.geoviksoft.turnia.ui.system.photo

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

/**
 * Through UIKit, which reads what Skia cannot — HEIC, and EXIF orientation — and draws the image
 * upright at the size asked for. Skia then only has to read back a plain JPEG.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual fun decodeUpright(bytes: ByteArray, maxSide: Int): ImageBitmap? {
    val image = UIImage.imageWithData(bytes.toNSData()) ?: return null
    val (width, height) = image.size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null

    val scale = minOf(1.0, maxSide / maxOf(width, height))
    val target = CGSizeMake(width * scale, height * scale)
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { this.scale = 1.0 }
    val upright = UIGraphicsImageRenderer(size = target, format = format).imageWithActions { _ ->
        target.useContents { image.drawInRect(CGRectMake(0.0, 0.0, this.width, this.height)) }
    }
    val jpeg = UIImageJPEGRepresentation(upright, 0.95) ?: return null
    return Image.makeFromEncoded(jpeg.toByteArray()).toComposeImageBitmap()
}

internal actual fun encodeWebp(bitmap: ImageBitmap, quality: Int): ByteArray =
    Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData(EncodedImageFormat.WEBP, quality)?.bytes
        ?: ByteArray(0)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}
