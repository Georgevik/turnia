package com.georgevik.turnia.ui.components.calendar

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Overlays evenly-spaced 45° stripes on top of the content — used to mark an
 * event whose owner is the current user but whose assignee is someone else
 * ("you own it, but you don't do it"). Draws after the content so the hatch
 * sits over the colored chip; keep any text above this in the stacking order.
 *
 * @param color stripe color; pass the chip's on-color at partial alpha so it
 *   reads on both light and dark fills.
 */
fun Modifier.diagonalHatch(
    color: Color,
    strokeWidth: Dp = 2.dp,
    spacing: Dp = 7.dp,
): Modifier = drawWithContent {
    drawContent()
    val stroke = strokeWidth.toPx()
    val step = spacing.toPx()
    val w = size.width
    val h = size.height
    // Lines run bottom-left → top-right; sweep their x-intercept from -h to w so
    // the whole box is covered regardless of aspect ratio.
    var x = -h
    while (x < w) {
        drawLine(
            color = color,
            start = Offset(x, h),
            end = Offset(x + h, 0f),
            strokeWidth = stroke,
        )
        x += step
    }
}
