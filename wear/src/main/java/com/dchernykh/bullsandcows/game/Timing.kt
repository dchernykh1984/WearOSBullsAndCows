package com.dchernykh.bullsandcows.game

// How long a game took, and how to write it on a watch.
//
// The clock is read once when the game starts and once when it is solved, and the
// answer is the difference between those two readings - not the time the board was
// actually on screen. A game left in the menu keeps running: that is the deliberate
// choice, because "how long did it take you" is a question about the wall clock,
// and because measuring attention rather than time would need the app to define
// what counts as attention.

/** Shown instead of a duration there is none of, or none that can be believed. */
const val NO_TIME_TEXT = "-"

private const val SECOND = 1000L
private const val MINUTE = 60

/**
 * Whole seconds between two clock readings, or null when the pair cannot be
 * trusted.
 *
 * A watch's clock can be set, corrected by the phone, or crossed by a timezone
 * change mid-game, and a negative duration is better shown as nothing than as a
 * record nobody can beat.
 */
fun elapsedSeconds(
    startedAt: Long,
    finishedAt: Long,
): Int? {
    val millis = finishedAt - startedAt
    if (millis < 0) return null
    return (millis / SECOND).toInt()
}

/**
 * A duration as minutes and seconds: "0:07", "4:31", "128:05".
 *
 * The minutes are never rolled up into hours - a Bulls and Cows game that ran for
 * an hour is a game somebody walked away from, and "1:08:05" would only make that
 * harder to read at a glance.
 */
fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds < 0) return NO_TIME_TEXT
    val minutes = seconds / MINUTE
    val rest = seconds % MINUTE
    return "$minutes:${if (rest < 10) "0" else ""}$rest"
}
