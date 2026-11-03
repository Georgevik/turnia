package com.geoviksoft.turnia.ui.system.photo

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Picked image in, profile photo out. A gallery photo can be 4000 px across and stored on its side,
 * so it is decoded already turned upright and no larger than [MAX_DECODED_SIDE]: enough to crop
 * from, and far from the tens of megabytes the full image would take.
 *
 * What leaves is always re-encoded, which is what drops the original's metadata, its location
 * included.
 */
object PhotoCodec {

    /** The side of a profile photo, in pixels: a 96 dp avatar on a 4x screen. */
    const val PHOTO_SIDE = 384

    /** What `storage.rules` accepts. */
    const val MAX_PHOTO_BYTES = 100 * 1024

    const val MAX_DECODED_SIDE = 2048

    private val QUALITIES = listOf(80, 60)

    /** The picked image, upright and scaled down to fit [MAX_DECODED_SIDE]; null if it is no image. */
    fun decode(bytes: ByteArray): ImageBitmap? = decodeUpright(bytes, MAX_DECODED_SIDE)

    /** The cropped square as WebP, within [MAX_PHOTO_BYTES]; null if no quality gets it there. */
    fun encode(square: ImageBitmap): ByteArray? =
        QUALITIES.asSequence()
            .map { quality -> encodeWebp(square, quality) }
            .firstOrNull { it.isNotEmpty() && it.size <= MAX_PHOTO_BYTES }
}

internal expect fun decodeUpright(bytes: ByteArray, maxSide: Int): ImageBitmap?

internal expect fun encodeWebp(bitmap: ImageBitmap, quality: Int): ByteArray
