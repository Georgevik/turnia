package com.geoviksoft.turnia.ui.system.photo

import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoCodecIosTest {

    @Test
    fun theUploadIsASmallWebpOfTheRightSize() {
        val square = noise(PhotoCodec.PHOTO_SIDE)

        val webp = checkNotNull(PhotoCodec.encode(square.asComposeImageBitmap()))

        assertTrue(webp.size in 1..PhotoCodec.MAX_PHOTO_BYTES, "${webp.size} bytes")
        assertEquals("WEBP", webp.decodeToString(8, 12))
        val back = Image.makeFromEncoded(webp)
        assertEquals(PhotoCodec.PHOTO_SIDE, back.width)
        assertEquals(PhotoCodec.PHOTO_SIDE, back.height)
    }

    /** Random pixels: the worst case to compress. */
    private fun noise(side: Int): Bitmap {
        val random = Random(7)
        val pixels = ByteArray(side * side * 4) { index -> if (index % 4 == 3) -1 else random.nextInt(256).toByte() }
        return Bitmap().apply {
            allocN32Pixels(side, side, opaque = true)
            installPixels(pixels)
        }
    }
}
