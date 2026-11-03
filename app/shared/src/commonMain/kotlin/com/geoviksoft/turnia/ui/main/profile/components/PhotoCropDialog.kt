package com.geoviksoft.turnia.ui.main.profile.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.geoviksoft.turnia.ui.system.TestTags
import com.geoviksoft.turnia.ui.system.photo.CropGeometry
import com.geoviksoft.turnia.ui.system.photo.CropState
import com.geoviksoft.turnia.ui.system.photo.PhotoCodec
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.profile_photo_crop_title
import turnia.app.shared.generated.resources.profile_photo_crop_use
import kotlin.math.roundToInt

/**
 * The picked image under a fixed circle, moved with a drag and zoomed with a pinch. *Use* hands back
 * the square behind the circle at [PhotoCodec.PHOTO_SIDE] pixels: the circle is only a mask, here
 * and wherever the photo is drawn, so the file needs no transparency.
 */
@Composable
fun PhotoCropDialog(
    image: ImageBitmap,
    onCancel: () -> Unit,
    onUse: (ImageBitmap) -> Unit,
) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(16.dp)
                .testTag(TestTags.PHOTO_CROP),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.profile_photo_crop_title),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val density = LocalDensity.current
                val side = minOf(maxWidth, maxHeight) * 0.9f
                val viewport = with(density) { side.toPx() }
                val geometry = remember(image, viewport) { CropGeometry(image.width, image.height, viewport) }
                var state by remember(geometry) { mutableStateOf(CropState()) }

                CropViewport(
                    image = image,
                    geometry = geometry,
                    state = state,
                    modifier = Modifier.size(side).pointerInput(geometry) {
                        detectTransformGestures { _, pan, zoom, _ -> state = geometry.transformed(state, pan, zoom) }
                    },
                )

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(Res.string.dialog_cancel), color = Color.White)
                    }
                    Button(
                        onClick = { onUse(crop(image, geometry, state)) },
                        modifier = Modifier.testTag(TestTags.PHOTO_CROP_USE),
                    ) {
                        Text(stringResource(Res.string.profile_photo_crop_use))
                    }
                }
            }
        }
    }
}

@Composable
private fun CropViewport(image: ImageBitmap, geometry: CropGeometry, state: CropState, modifier: Modifier) {
    Canvas(modifier) {
        val scale = geometry.scale(state)
        val drawn = Size(image.width * scale, image.height * scale)
        val topLeft = Offset((size.width - drawn.width) / 2, (size.height - drawn.height) / 2) + state.offset
        translate(topLeft.x, topLeft.y) {
            drawImage(
                image = image,
                dstSize = IntSize(drawn.width.roundToInt(), drawn.height.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
        // Dim what falls outside the circle, the part the photo will not keep.
        val outside = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size))
            addOval(androidx.compose.ui.geometry.Rect(Offset.Zero, size))
        }
        drawPath(outside, Color.Black.copy(alpha = 0.6f))
        drawCircle(Color.White, radius = size.minDimension / 2, style = Stroke(width = 2.dp.toPx()))
    }
}

private fun crop(image: ImageBitmap, geometry: CropGeometry, state: CropState): ImageBitmap {
    val source = geometry.sourceRect(state)
    val side = PhotoCodec.PHOTO_SIDE
    val square = ImageBitmap(side, side)
    androidx.compose.ui.graphics.Canvas(square).drawImageRect(
        image = image,
        srcOffset = IntOffset(source.left.roundToInt(), source.top.roundToInt()),
        srcSize = IntSize(
            source.width.roundToInt().coerceAtMost(image.width - source.left.roundToInt()),
            source.height.roundToInt().coerceAtMost(image.height - source.top.roundToInt()),
        ),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(side, side),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    return square
}
