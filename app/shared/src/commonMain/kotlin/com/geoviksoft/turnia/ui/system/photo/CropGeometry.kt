package com.geoviksoft.turnia.ui.system.photo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.min

/** Where the user has moved and zoomed the image under the crop circle. */
data class CropState(val zoom: Float = 1f, val offset: Offset = Offset.Zero)

/**
 * The image under a fixed square of side [viewport], the box the crop circle is drawn in. At zoom
 * 1 the image just covers the square, centred; it can be moved and zoomed in, but never so far that
 * the square shows anything outside the image.
 *
 * Pure arithmetic in pixels, so it is tested without a screen: the dialog only feeds it gestures and
 * draws what it says.
 */
class CropGeometry(
    private val imageWidth: Int,
    private val imageHeight: Int,
    private val viewport: Float,
) {
    private val fitScale = viewport / min(imageWidth, imageHeight)

    /** Image pixels to screen pixels. */
    fun scale(state: CropState): Float = fitScale * state.zoom

    fun transformed(state: CropState, pan: Offset, zoomChange: Float): CropState =
        clamped(CropState((state.zoom * zoomChange).coerceIn(1f, MAX_ZOOM), state.offset + pan))

    fun clamped(state: CropState): CropState {
        val scale = scale(state)
        val maxX = (imageWidth * scale - viewport) / 2
        val maxY = (imageHeight * scale - viewport) / 2
        return state.copy(offset = Offset(state.offset.x.coerceIn(-maxX, maxX), state.offset.y.coerceIn(-maxY, maxY)))
    }

    /** The square of the image, in its own pixels, that the viewport shows. */
    fun sourceRect(state: CropState): Rect {
        val scale = scale(state)
        val left = ((imageWidth * scale - viewport) / 2 - state.offset.x) / scale
        val top = ((imageHeight * scale - viewport) / 2 - state.offset.y) / scale
        val side = viewport / scale
        return Rect(left, top, left + side, top + side)
    }

    companion object {
        const val MAX_ZOOM = 5f
    }
}
