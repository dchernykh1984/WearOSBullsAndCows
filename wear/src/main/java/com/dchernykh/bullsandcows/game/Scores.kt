package com.dchernykh.bullsandcows.game

// The best result of a game, kept apart from the storage that holds it so the rule
// is unit tested.
//
// A result is the number of guesses a win took and how long it took, and *lower is
// better on both* - the opposite of most games. Guesses decide it; the time only
// separates two wins that took the same number of guesses, which happens
// constantly, because the guess counts are small integers.

/** No record at all. Not a guess count, because a game cannot be won in no guesses. */
const val NO_BEST = 0

/**
 * A win: how many guesses it took, and how long.
 *
 * [seconds] is null for a record set before the game measured time at all, and for
 * one whose clock could not be believed. Such a record is a real record with no
 * time, and it has to keep working rather than sort as though it took no time.
 */
data class Result(
    val attempts: Int,
    val seconds: Int?,
)

/** A best after a finished game, and whether that game is the one that set it. */
data class RecordOutcome(
    val best: Result?,
    val isRecord: Boolean,
)

/**
 * A stored value read back as a result, or null when there is no usable one.
 *
 * Anything missing or not positive reads as "never solved", so a corrupt entry
 * cannot make a record impossible to beat.
 */
fun normalizeResult(
    attempts: Int?,
    seconds: Int?,
): Result? {
    if (attempts == null || attempts <= NO_BEST) return null
    return Result(attempts, seconds?.takeIf { it >= 0 })
}

/**
 * Whether [candidate] beats [best].
 *
 * Fewer guesses wins; the same number is settled by the shorter time. A result
 * with no time never displaces an equal one that has a time - there is nothing to
 * compare, and the record already on the watch keeps its place.
 */
fun beats(
    candidate: Result?,
    best: Result?,
): Boolean {
    if (candidate == null || best == null) return candidate != null
    if (candidate.attempts != best.attempts) return candidate.attempts < best.attempts
    // Both games have to have been timed for the tie-break to mean anything.
    val a = candidate.seconds
    val b = best.seconds
    return a != null && b != null && a < b
}

/**
 * The best after a solved game. The first solve of a level always sets the record;
 * after that only a better result does.
 */
fun updateBest(
    previous: Result?,
    result: Result?,
): RecordOutcome =
    if (beats(result, previous)) {
        RecordOutcome(result, isRecord = true)
    } else {
        RecordOutcome(previous, isRecord = false)
    }
