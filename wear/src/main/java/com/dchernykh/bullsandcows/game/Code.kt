package com.dchernykh.bullsandcows.game

import kotlin.random.Random

// What a code is, and what a guess against one earns. Everything here is about the
// digits themselves; the game that is played with them is in BullsAndCows.kt.

fun isDigit(value: Int): Boolean = value in 0 until DIGIT_COUNT

fun hasRepeats(digits: List<Int>): Boolean = digits.toSet().size != digits.size

/** Whether a value is a usable code: distinct digits, of a legal length. */
fun isCode(digits: List<Int>): Boolean =
    digits.size in MIN_LENGTH..MAX_LENGTH && digits.all(::isDigit) && !hasRepeats(digits)

/**
 * The bulls and cows a guess earns against a secret.
 *
 * Bulls are counted first and what is left is matched as multisets, so a digit is
 * never counted twice: guessing 1122 against 1213 is one bull (the first 1) and
 * two cows (the second 1 and one 2), not four of something. Codes in this game are
 * always distinct digits, but the rule is written for the general case because a
 * scoring function that only works on the easy input is one that quietly stops
 * being right the day the game changes.
 */
fun scoreGuess(
    secret: List<Int>,
    guess: List<Int>,
): Score {
    val length = minOf(secret.size, guess.size)
    val secretCounts = IntArray(DIGIT_COUNT)
    val guessCounts = IntArray(DIGIT_COUNT)

    var bulls = 0
    for (i in 0 until length) {
        val wanted = secret[i]
        val tried = guess[i]
        if (isDigit(wanted) && wanted == tried) {
            bulls++
            continue
        }
        if (isDigit(wanted)) secretCounts[wanted]++
        if (isDigit(tried)) guessCounts[tried]++
    }

    var cows = 0
    for (d in 0 until DIGIT_COUNT) cows += minOf(secretCounts[d], guessCounts[d])
    return Score(bulls, cows)
}

/**
 * A uniformly random secret.
 *
 * The code never starts with a zero, which is the official rule: a leading zero
 * reads as a shorter number on the screen, and the player would have to be told it
 * can happen. The first digit is drawn from 1..9 and the rest, zero included, from
 * what is left - drawing from a shrinking pool keeps every legal code equally
 * likely, instead of favouring the ones a "reroll the zero" fix would produce.
 */
fun makeSecret(
    length: Int,
    random: Random = Random.Default,
): List<Int> {
    val size = length.coerceIn(MIN_LENGTH, MAX_LENGTH)
    val pool = (0 until DIGIT_COUNT).toMutableList()
    val digits = mutableListOf(pool.removeAt(1 + random.nextInt(DIGIT_COUNT - 1)))
    repeat(size - 1) { digits.add(pool.removeAt(random.nextInt(pool.size))) }
    return digits
}
