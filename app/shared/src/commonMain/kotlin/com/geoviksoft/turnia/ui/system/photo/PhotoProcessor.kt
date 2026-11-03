package com.geoviksoft.turnia.ui.system.photo

import androidx.compose.ui.graphics.ImageBitmap

/** [PhotoCodec] behind an interface, so a ViewModel test needs no platform graphics. */
interface PhotoProcessor {
    fun decode(bytes: ByteArray): ImageBitmap?
    fun encode(square: ImageBitmap): ByteArray?
}

object CodecPhotoProcessor : PhotoProcessor {
    override fun decode(bytes: ByteArray): ImageBitmap? = PhotoCodec.decode(bytes)
    override fun encode(square: ImageBitmap): ByteArray? = PhotoCodec.encode(square)
}
