package com.dchernykh.bullsandcows.layout

import org.junit.Assert.assertTrue
import kotlin.math.hypot

/**
 * The one assertion every layout test wants: that the bezel does not slice a
 * corner off the thing that was just placed.
 *
 * Shared because writing it out means four nested loops - screens, boxes, corners
 * of a box, coordinates of a corner - and four of those in a row is what turns a
 * test file into something nobody reads.
 */
fun assertCornersOnScreen(
    screenSize: Int,
    box: Box,
    what: String,
) {
    val radius = screenSize / 2f
    val corners =
        listOf(
            box.x to box.y,
            box.x + box.w to box.y,
            box.x to box.y + box.h,
            box.x + box.w to box.y + box.h,
        )
    for ((x, y) in corners) {
        assertTrue(
            "corner ($x, $y) of $what escapes a $screenSize screen",
            hypot(x - radius, y - radius) <= radius,
        )
    }
}

/**
 * Every corner of a box, inside a circle concentric with the screen.
 *
 * The board is drawn inside the circle the keypad ring leaves free, not inside the
 * screen, so what it must fit is that smaller radius.
 */
fun assertInsideCircle(
    screenSize: Int,
    radius: Float,
    box: Box,
    what: String,
) {
    val centre = screenSize / 2f
    val corners =
        listOf(
            box.x to box.y,
            box.x + box.w to box.y,
            box.x to box.y + box.h,
            box.x + box.w to box.y + box.h,
        )
    for ((x, y) in corners) {
        assertTrue(
            "corner ($x, $y) of $what escapes the circle on a $screenSize screen",
            hypot(x - centre, y - centre) <= radius + 1f,
        )
    }
}
