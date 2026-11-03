package com.geoviksoft.turnia.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.geoviksoft.turnia.ui.system.photo.PhotoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random

/** What a gallery photo becomes before it leaves the device. No emulator needed. */
@RunWith(AndroidJUnit4::class)
class PhotoCodecTest {

    @Test
    fun aLargePortraitIsDecodedUprightAndSmaller() {
        val decoded = checkNotNull(PhotoCodec.decode(galleryPhoto()))

        // 4000×3000 stored on its side, turned upright and fitted into 2048.
        assertEquals(1536, decoded.width)
        assertEquals(2048, decoded.height)
    }

    @Test
    fun theUploadIsASmallWebpWithNoMetadata() {
        val decoded = checkNotNull(PhotoCodec.decode(galleryPhoto())).asAndroidBitmap()
        val side = minOf(decoded.width, decoded.height)
        val square = Bitmap.createScaledBitmap(
            Bitmap.createBitmap(decoded, 0, 0, side, side), PhotoCodec.PHOTO_SIDE, PhotoCodec.PHOTO_SIDE, true,
        )

        val webp = checkNotNull(PhotoCodec.encode(square.asImageBitmap()))

        assertTrue("${webp.size} bytes", webp.size <= PhotoCodec.MAX_PHOTO_BYTES)
        assertEquals("RIFF", String(webp, 0, 4, Charsets.US_ASCII))
        assertEquals("WEBP", String(webp, 8, 4, Charsets.US_ASCII))
        assertFalse("No EXIF chunk", String(webp, Charsets.ISO_8859_1).contains("EXIF"))
        val back = BitmapFactory.decodeByteArray(webp, 0, webp.size)
        assertEquals(PhotoCodec.PHOTO_SIDE, back.width)
        assertEquals(PhotoCodec.PHOTO_SIDE, back.height)
    }

    /** A 4000×3000 JPEG of noise — the worst case to compress — taken on its side, with a location. */
    private fun galleryPhoto(): ByteArray {
        val bitmap = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val random = Random(7)
        for (y in 0 until 3000 step 20) for (x in 0 until 4000 step 20) {
            paint.color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            canvas.drawRect(x.toFloat(), y.toFloat(), x + 20f, y + 20f, paint)
        }
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "gallery.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        ExifInterface(file.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "40/1,25/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "3/1,42/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
            saveAttributes()
        }
        return file.readBytes()
    }
}
