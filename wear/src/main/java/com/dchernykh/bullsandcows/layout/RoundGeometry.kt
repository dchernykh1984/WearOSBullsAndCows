package com.dchernykh.bullsandcows.layout

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Keeping content inside a round screen.
//
// A round watch cuts the corners off every row, so a line placed near the top or
// the bottom is sliced by the bezel unless its width is held to the chord of the
// circle at that height.
//
// Every helper here takes the circle it must stay inside as a radius concentric
// with the screen: the screen's own radius for a full-width menu, and the smaller
// one left free inside the keypad ring for the board the game plays on.

/** A pixel box, in screen coordinates. */
data class Box(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
) {
    operator fun contains(point: Pair<Int, Int>): Boolean {
        val (px, py) = point
        return px >= x && px < x + w && py >= y && py < y + h
    }
}

/**
 * Half the on-screen width of a circle of this radius, [dy] pixels from its
 * horizontal centre line. Zero past the edge of the circle.
 */
fun chordHalfWidth(
    radius: Float,
    dy: Float,
): Float {
    val distance = abs(dy)
    if (distance >= radius) return 0f
    return sqrt(radius * radius - distance * distance)
}

/**
 * The widest a horizontally centred line may be with its centre at [y], inside a
 * circle of [radius] concentric with the screen, once [padding] is kept from the
 * edge on each side. The binding chord is at whichever end of the line's height is
 * further from the centre line.
 */
fun safeLineWidth(
    screenSize: Int,
    radius: Float,
    y: Float,
    lineHeight: Float,
    padding: Int,
): Float {
    val centre = screenSize / 2f
    val dyTop = abs(y - lineHeight / 2f - centre)
    val dyBottom = abs(y + lineHeight / 2f - centre)
    val half = chordHalfWidth(radius, maxOf(dyTop, dyBottom))
    val width = 2f * half - 2f * padding
    return if (width > 0f) width else 0f
}

/**
 * A horizontally centred box of the given height whose top edge is at [top], never
 * wider than [maxWidth] and never poking past the circle.
 */
fun centeredBox(
    screenSize: Int,
    radius: Float,
    top: Int,
    height: Int,
    maxWidth: Float,
    padding: Int,
): Box {
    val safe = safeLineWidth(screenSize, radius, top + height / 2f, height.toFloat(), padding)
    val width = floor(minOf(maxWidth, safe)).toInt()
    return Box(x = ((screenSize - width) / 2f).roundToInt(), y = top, w = width, h = height)
}

/**
 * A box split into [count] equal columns separated by [gap] pixels, with the row of
 * columns centred in it.
 *
 * No column is wider than [maxWidth], which is how a three-digit guess gets slots
 * the same size as a five-digit one instead of three enormous ones. Every row of
 * cells on the board - the guess slots, the two action buttons, the columns of a
 * history row - is this one shape.
 */
fun columnsIn(
    box: Box,
    count: Int,
    gap: Int,
    maxWidth: Int = box.w,
): List<Box> {
    val columns = maxOf(1, count)
    val space = maxOf(0, gap)
    val width = ((box.w - (columns - 1) * space) / columns).coerceIn(1, maxOf(1, maxWidth))
    val left = box.x + ((box.w - (width * columns + space * (columns - 1))) / 2f).roundToInt()
    return List(columns) { i -> Box(x = left + i * (width + space), y = box.y, w = width, h = box.h) }
}
