package com.geoviksoft.turnia.ui.components.calendar

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/** How much of the tile's outline each arrow covers, tail included. */
private const val ARROW_FRACTION = 0.28f

/**
 * Pieces the arrow's body is drawn in, each thicker than the last, so it tapers to its tail. By width
 * and not by alpha: semi-transparent pieces show a seam wherever two of them meet.
 */
private const val TAIL_PIECES = 10

/** How thick the tail's end is, as a share of the full stroke. */
private const val TAIL_START_WIDTH = 0.3f

/**
 * Two arrows chasing each other round the edge of what they are drawn on: a shift on offer. The
 * same picture as the swap icon, drawn at the size of the whole chip so it stands out in a month.
 *
 * @param cornerRadius the shape's own, so the arrows follow its outline.
 * @param progress how far round the first arrow is, from 0 to 1; the second runs half a lap
 *   behind. A lambda, read only while drawing, so the animation redraws without recomposing.
 */
fun Modifier.swapArrows(
    color: Color,
    cornerRadius: Dp,
    progress: () -> Float,
    strokeWidth: Dp = 1.5.dp,
): Modifier = drawWithCache {
    val stroke = strokeWidth.toPx()
    val headSize = stroke * 2.7f
    // An arrowhead is wider than the line, and a clipped shape would cut it: the outline runs far
    // enough inside that the head never leaves it.
    val inset = headSize * 0.75f
    val outline = Path().apply {
        addRoundRect(
            RoundRect(
                left = inset,
                top = inset,
                right = size.width - inset,
                bottom = size.height - inset,
                cornerRadius = CornerRadius((cornerRadius.toPx() - inset).coerceAtLeast(0f)),
            ),
        )
    }
    val measure = PathMeasure().apply { setPath(outline, forceClosed = true) }
    val lap = measure.length
    val arrowLength = lap * ARROW_FRACTION
    val piece = Path()
    val head = Path()

    fun wrap(distance: Float) = ((distance % lap) + lap) % lap

    // PathMeasure cannot cut across the point where the outline closes, so a piece that spans it
    // is taken in two.
    fun cut(from: Float, length: Float) {
        piece.reset()
        val start = wrap(from)
        val end = start + length
        if (end <= lap) {
            measure.getSegment(start, end, piece, startWithMoveTo = true)
        } else {
            measure.getSegment(start, lap, piece, startWithMoveTo = true)
            measure.getSegment(0f, end - lap, piece, startWithMoveTo = true)
        }
    }

    onDrawWithContent {
        drawContent()
        if (lap <= 0f) return@onDrawWithContent

        repeat(2) { arrow ->
            val tip = (progress() + arrow * 0.5f) * lap
            val pieceLength = arrowLength / TAIL_PIECES

            for (index in 0 until TAIL_PIECES) {
                val thickness = TAIL_START_WIDTH + (1 - TAIL_START_WIDTH) * (index + 1) / TAIL_PIECES
                cut(from = tip - arrowLength + index * pieceLength, length = pieceLength)
                drawPath(
                    path = piece,
                    color = color,
                    style = Stroke(width = stroke * thickness, cap = StrokeCap.Round),
                )
            }

            val at = wrap(tip)
            val position = measure.getPosition(at)
            val tangent = measure.getTangent(at).normalized()
            val normal = Offset(-tangent.y, tangent.x)
            val back = position - tangent * (headSize * 0.35f)
            head.reset()
            head.moveTo(position.x + tangent.x * headSize, position.y + tangent.y * headSize)
            head.lineTo(back.x + normal.x * headSize * 0.75f, back.y + normal.y * headSize * 0.75f)
            head.lineTo(back.x - normal.x * headSize * 0.75f, back.y - normal.y * headSize * 0.75f)
            head.close()
            drawPath(path = head, color = color)
        }
    }
}

private fun Offset.normalized(): Offset {
    val length = sqrt(x * x + y * y)
    return if (length == 0f) this else Offset(x / length, y / length)
}
