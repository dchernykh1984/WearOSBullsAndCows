package com.dchernykh.bullsandcows.layout

import kotlin.math.roundToInt

// Where everything inside the keypad ring goes.
//
// The stack, top to bottom: the attempt counter, the history window, the guess
// being composed, and the two action buttons. It is centred vertically, so the
// widest rows land where the circle is widest.

// Heights and the gap between them, as fractions of the free radius. The counter
// row doubles as the control that pages the history, so it is sized as something a
// thumb hits rather than as a caption; the action row carries the two buttons
// tapped most after the keypad.
private const val COUNTER_RATIO = 0.26f
private const val ROW_RATIO = 0.2f
private const val GUESS_RATIO = 0.29f
private const val ACTION_RATIO = 0.35f
private const val GAP_RATIO = 0.04f

/** The stack never uses the full chord, so a long row keeps a little air either side. */
private const val WIDTH_RATIO = 0.94f

/** Every box the board is drawn in, already clipped to the circle the ring leaves. */
data class BoardStack(
    val height: Int,
    val gap: Int,
    val counter: Box,
    val history: List<Box>,
    val guess: Box,
    val actions: Box,
)

private fun part(
    radius: Float,
    ratio: Float,
): Int = maxOf(1, (radius * ratio).roundToInt())

/**
 * The stack for a round screen [screenSize] across whose keypad ring leaves a free
 * circle of [innerRadius], showing [historyRows] past guesses at once.
 */
fun boardStack(
    screenSize: Int,
    innerRadius: Float,
    historyRows: Int,
    padding: Int,
): BoardStack {
    val rows = maxOf(1, historyRows)
    val counterHeight = part(innerRadius, COUNTER_RATIO)
    val rowHeight = part(innerRadius, ROW_RATIO)
    val guessHeight = part(innerRadius, GUESS_RATIO)
    val actionHeight = part(innerRadius, ACTION_RATIO)
    val gap = part(innerRadius, GAP_RATIO)

    val height = counterHeight + gap + rows * rowHeight + gap + guessHeight + gap + actionHeight
    val maxWidth = (2f * innerRadius * WIDTH_RATIO).roundToInt().toFloat()

    fun box(
        top: Int,
        size: Int,
    ) = centeredBox(screenSize, innerRadius, top, size, maxWidth, padding)

    var top = (screenSize / 2f).roundToInt() - (height / 2f).roundToInt()
    val counter = box(top, counterHeight)

    top += counterHeight + gap
    val rowsAtTheirOwnWidth = List(rows) { i -> box(top + i * rowHeight, rowHeight) }
    // The history is a table, and a table needs one set of columns. Left to itself
    // each row takes the full chord at its own height, so on a round screen the row
    // furthest from the middle comes out narrowest and the guesses and their counts
    // step sideways from line to line. Every row is squared off to the narrowest.
    val narrowest = rowsAtTheirOwnWidth.minBy { it.w }
    val history = rowsAtTheirOwnWidth.map { it.copy(x = narrowest.x, w = narrowest.w) }

    top += rows * rowHeight + gap
    val guess = box(top, guessHeight)

    top += guessHeight + gap
    val actions = box(top, actionHeight)

    return BoardStack(height, gap, counter, history, guess, actions)
}
