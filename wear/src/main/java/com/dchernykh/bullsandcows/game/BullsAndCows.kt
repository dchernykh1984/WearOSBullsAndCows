package com.dchernykh.bullsandcows.game

import kotlin.random.Random

// The whole rule set, with nothing Android in it, so every rule is exercised by a
// unit test rather than by squinting at a watch.
//
// Bulls and Cows: the watch picks a secret code of distinct digits and the player
// guesses it. Every guess is answered with two counts - bulls, the digits that are
// right and in the right place, and cows, the digits that are in the code but
// somewhere else.
//
// A position is an immutable value and a guess is a function from one to the next,
// which is what lets a test start from any game rather than only from one it could
// reach by playing.

/** A code is made of decimal digits. */
const val DIGIT_COUNT = 10

/**
 * Code lengths the rules accept. Two is the shortest code that is still a
 * deduction rather than a coin toss; eight keeps a distinct-digit code well inside
 * the ten available digits and fits the watch screen.
 */
const val MIN_LENGTH = 2
const val MAX_LENGTH = 8

/**
 * There is no losing state. Bulls and Cows is played until the code is cracked,
 * and the number of guesses that took is the score; a limit would be a rule of our
 * own invention, and it would compete with the score for the same job.
 */
enum class Status { RUNNING, WON }

/** Why a guess was refused. */
enum class Refusal { NOT_RUNNING, WRONG_LENGTH, NOT_A_DIGIT, REPEATED_DIGIT }

/** One played guess and what it earned. */
data class Attempt(
    val digits: List<Int>,
    val bulls: Int,
    val cows: Int,
)

/** How a guess scored: right digit in the right place, and right digit elsewhere. */
data class Score(
    val bulls: Int,
    val cows: Int,
)

/**
 * A game in progress.
 *
 * [secret] is part of the state because a finished game reveals it, and because a
 * test that has to guess the code it is testing is a test of the test.
 */
data class GameState(
    val secret: List<Int>,
    val history: List<Attempt> = emptyList(),
    val status: Status = Status.RUNNING,
) {
    val length: Int get() = secret.size

    val attemptsUsed: Int get() = history.size
}

/**
 * A fresh game.
 *
 * A hand-passed secret that does not fit the rules is replaced by a random one
 * rather than refused: a watch game that throws on start is a black screen, and
 * there is no way for a player to supply one anyway.
 */
fun newGame(
    length: Int,
    secret: List<Int>? = null,
    random: Random = Random.Default,
): GameState = GameState(secret = if (secret != null && isCode(secret)) secret else makeSecret(length, random))

/** Why this guess cannot be played, or null when it can. */
fun GameState.refusalFor(digits: List<Int>): Refusal? =
    when {
        status != Status.RUNNING -> Refusal.NOT_RUNNING
        digits.size != length -> Refusal.WRONG_LENGTH
        !digits.all(::isDigit) -> Refusal.NOT_A_DIGIT
        hasRepeats(digits) -> Refusal.REPEATED_DIGIT
        else -> null
    }

/**
 * Whether a digit is spent for the guess being composed - which is exactly when
 * the keypad draws its key dim. Every code is distinct digits, so a digit already
 * placed is spent for the rest of that guess.
 */
fun digitTaken(
    entered: List<Int>,
    digit: Int,
): Boolean = digit in entered

/**
 * Whether a digit may be appended to the guess being composed. The keypad asks
 * this before it takes a tap, which is why a refused guess is a fallback and not
 * the normal path.
 */
fun GameState.acceptsDigit(
    entered: List<Int>,
    digit: Int,
): Boolean = status == Status.RUNNING && isDigit(digit) && entered.size < length && !digitTaken(entered, digit)

/** What a guess did: the game after it, and the row it added, or why it was refused. */
data class Played(
    val game: GameState,
    val entry: Attempt?,
    val refusal: Refusal?,
) {
    val accepted: Boolean get() = entry != null
}

/** Play a guess. The game ends when every digit is a bull, and in no other way. */
fun GameState.guessed(digits: List<Int>): Played {
    val refusal = refusalFor(digits)
    if (refusal != null) return Played(this, entry = null, refusal = refusal)

    val score = scoreGuess(secret, digits)
    val entry = Attempt(digits, score.bulls, score.cows)
    val status = if (score.bulls == length) Status.WON else status
    return Played(copy(history = history + entry, status = status), entry = entry, refusal = null)
}

/** The digits as the screen writes them, so every row is written the same way. */
fun codeToText(digits: List<Int>): String = digits.joinToString("") { if (isDigit(it)) it.toString() else "?" }
