package com.geoviksoft.turnia.ui.system.photo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CropGeometryTest {

    // A 3000×2000 landscape under a 300 px square: at zoom 1 its 2000 px height fills the square, so
    // 1 screen px is 6⅔ image px and the image is drawn 450 px wide.
    private val geometry = CropGeometry(imageWidth = 3000, imageHeight = 2000, viewport = 300f)

    @Test
    fun atFirstTheCentredSquareIsCropped() {
        assertRect(Rect(500f, 0f, 2500f, 2000f), geometry.sourceRect(CropState()))
    }

    @Test
    fun theImageCannotLeaveTheSquare() {
        val dragged = geometry.transformed(CropState(), pan = Offset(1_000f, 1_000f), zoomChange = 1f)

        // 450 drawn against 300 shown leaves 75 px either side, and none vertically.
        assertTrue(abs(dragged.offset.x - 75f) < 0.01f && abs(dragged.offset.y) < 0.01f, "${dragged.offset}")
        assertRect(Rect(0f, 0f, 2000f, 2000f), geometry.sourceRect(dragged))
    }

    @Test
    fun zoomStopsAtItsLimits() {
        assertEquals(1f, geometry.transformed(CropState(), Offset.Zero, zoomChange = 0.5f).zoom)
        assertEquals(CropGeometry.MAX_ZOOM, geometry.transformed(CropState(), Offset.Zero, zoomChange = 50f).zoom)
    }

    @Test
    fun aPanAtTwiceTheZoomMovesHalfAsFarInTheImage() {
        val zoomed = geometry.transformed(CropState(), Offset.Zero, zoomChange = 2f)
        val panned = geometry.transformed(zoomed, Offset(-100f, 0f), zoomChange = 1f)

        // At 2× the square shows 1000 image px, from 1000; 100 screen px left is 333⅓ image px right.
        assertRect(Rect(1333.33f, 500f, 2333.33f, 1500f), geometry.sourceRect(panned))
    }

    @Test
    fun zoomingOutPullsAnOffsetBackInside() {
        val zoomed = geometry.transformed(CropState(), Offset.Zero, zoomChange = 2f)
        val toTheEdge = geometry.transformed(zoomed, Offset(1_000f, 1_000f), zoomChange = 1f)

        val zoomedOut = geometry.transformed(toTheEdge, Offset.Zero, zoomChange = 0.5f)

        assertRect(Rect(0f, 0f, 2000f, 2000f), geometry.sourceRect(zoomedOut))
    }

    private fun assertRect(expected: Rect, actual: Rect) {
        val close = listOf(
            expected.left to actual.left, expected.top to actual.top,
            expected.right to actual.right, expected.bottom to actual.bottom,
        ).all { (e, a) -> abs(e - a) < 0.01f }
        assertTrue(close, "expected $expected but was $actual")
    }
}
