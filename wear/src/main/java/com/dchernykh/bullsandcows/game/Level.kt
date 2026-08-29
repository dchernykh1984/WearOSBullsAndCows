package com.dchernykh.bullsandcows.game

import androidx.annotation.StringRes
import com.dchernykh.bullsandcows.R

/**
 * The difficulty levels: a level is just how long the secret is.
 *
 * Nobody runs out of guesses, so a level carries no budget, and every code is made
 * of distinct digits, so there is no second rule to explain. One dial with three
 * positions, and the button says the digit count rather than a name - a ladder
 * that reads itself needs no legend, which a ladder of invented names did:
 * "Expert" told a player nothing about what it would change.
 *
 * Four distinct digits is the classic game; three is the quick version and five
 * the long one. Both are the documented ways of moving the difficulty.
 *
 * The name is the storage key, so a level must never be renamed and none may be
 * reordered: either would hand one difficulty's record to another.
 */
enum class Level(
    val length: Int,
    @param:StringRes val labelRes: Int,
) {
    EASY(length = 3, labelRes = R.string.level_3),
    CLASSIC(length = 4, labelRes = R.string.level_4),
    HARD(length = 5, labelRes = R.string.level_5),
    ;

    /** The next level in the cycle, so one button walks through all of them. */
    val next: Level get() = entries[(ordinal + 1) % entries.size]

    companion object {
        val DEFAULT = CLASSIC

        /**
         * The level a stored name refers to, or the default.
         *
         * Anything unrecognised reads as the default rather than as the first
         * level, which would silently move everyone to the three-digit game.
         */
        fun fromStoredName(name: String?): Level = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
