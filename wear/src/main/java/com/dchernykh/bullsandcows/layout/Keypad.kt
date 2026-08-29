package com.dchernykh.bullsandcows.layout

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// The keypad, laid out as a ring of round keys just inside the bezel.
//
// A round screen has no good place for a 3x4 grid of digits, but it has a perfect
// one for a ring: ten keys spaced evenly around the edge are all the same distance
// from the centre and all equally easy to reach with a thumb. What the ring
// encloses is a smaller circle, which is where the game draws everything else -
// hence `innerRadius`, the free radius the board must stay inside.

// Fractions of the screen diameter. The key is big enough to hit without looking
// (about 72px on a 466px screen), the bezel margin keeps it off the curved glass,
// and the gap separates the ring from whatever is drawn inside it.
private const val KEY_SIZE_RATIO = 0.155f
private const val MARGIN_RATIO = 0.015f
private const val GAP_RATIO = 0.02f

/** Slot 0 sits at the top and the rest follow clockwise, like the hours on a face. */
private const val START_ANGLE = -PI.toFloat() / 2f

/** The ring: the key size, the circle the keys sit on, and where each one lands. */
data class Keypad(
    val count: Int,
    val keySize: Int,
    val radius: Float,
    val innerRadius: Float,
    val slots: List<Box>,
)

private fun ratioSize(
    screenSize: Int,
    ratio: Float,
): Int = maxOf(1, (screenSize * ratio).roundToInt())

/** The ring for [count] keys on a round screen [screenSize] pixels across. */
fun keypadLayout(
    screenSize: Int,
    count: Int,
): Keypad {
    val keys = maxOf(1, count)
    val keySize = ratioSize(screenSize, KEY_SIZE_RATIO)
    val margin = ratioSize(screenSize, MARGIN_RATIO)
    val gap = ratioSize(screenSize, GAP_RATIO)

    val centre = screenSize / 2f
    val radius = centre - margin - keySize / 2f
    val innerRadius = maxOf(0f, radius - keySize / 2f - gap)

    val slots =
        List(keys) { i ->
            val angle = START_ANGLE + (i * 2f * PI.toFloat()) / keys
            Box(
                x = (centre + radius * cos(angle) - keySize / 2f).roundToInt(),
                y = (centre + radius * sin(angle) - keySize / 2f).roundToInt(),
                w = keySize,
                h = keySize,
            )
        }
    return Keypad(count = keys, keySize = keySize, radius = radius, innerRadius = innerRadius, slots = slots)
}
