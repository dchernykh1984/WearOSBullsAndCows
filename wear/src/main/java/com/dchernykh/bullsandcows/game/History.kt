package com.dchernykh.bullsandcows.game

// The guess history as the watch shows it: a short window onto a list that grows
// longer than the screen. Pure, so the scrolling arithmetic is unit tested rather
// than discovered by swiping past the end of the list on a wrist.
//
// The newest guess is the last entry and the window follows it: after each guess
// the screen jumps to the bottom, and paging walks back up through the older rows.

/**
 * The largest first-row index that still fills the window, and so also where the
 * window sits when it is showing the newest guess. Zero while the whole history
 * fits on screen.
 */
fun maxOffset(
    count: Int,
    visible: Int,
): Int = maxOf(0, count - maxOf(1, visible))

/** An offset held inside the list. Anything out of range reads as the top. */
fun clampOffset(
    count: Int,
    visible: Int,
    offset: Int,
): Int = offset.coerceIn(0, maxOffset(count, visible))

/**
 * The offset after scrolling by [delta] rows: positive towards the newest guess,
 * negative towards the oldest. Stops at both ends instead of wrapping.
 */
fun scrollBy(
    count: Int,
    visible: Int,
    offset: Int,
    delta: Int,
): Int = clampOffset(count, visible, clampOffset(count, visible, offset) + delta)

/** The rows to draw: at most [visible] entries starting at [offset]. */
fun <T> windowOf(
    entries: List<T>,
    visible: Int,
    offset: Int,
): List<T> {
    val rows = maxOf(1, visible)
    val start = clampOffset(entries.size, rows, offset)
    return entries.subList(start, minOf(entries.size, start + rows))
}

/**
 * One page back through the history, wrapping to the newest guesses once the
 * oldest is on screen.
 *
 * Paging only goes one way on purpose: it is driven by a single control, and a
 * control that always does the same thing is one a thumb learns in two taps.
 */
fun pageBack(
    count: Int,
    visible: Int,
    offset: Int,
): Int {
    val rows = maxOf(1, visible)
    val from = clampOffset(count, rows, offset)
    return if (from == 0) maxOffset(count, rows) else clampOffset(count, rows, from - rows)
}

/**
 * What the pager says: "4-6/12" - the guesses on screen out of the guesses played.
 * Null while everything played still fits on screen, which is when there is nothing
 * to page and the pager should not be there at all.
 */
fun windowLabel(
    count: Int,
    visible: Int,
    offset: Int,
): String? {
    val rows = maxOf(1, visible)
    if (count <= rows) return null
    val start = clampOffset(count, rows, offset)
    val last = minOf(count, start + rows)
    return "${start + 1}-$last/$count"
}
