package com.dchernykh.bullsandcows.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private val SECRET = listOf(1, 2, 3, 4)

private fun gameOf(secret: List<Int> = SECRET) = newGame(secret.size, secret)

class NewGameTest {
    @Test
    fun `deals the secret it was handed`() {
        val game = gameOf()

        assertEquals(SECRET, game.secret)
        assertEquals(4, game.length)
        assertEquals(Status.RUNNING, game.status)
        assertEquals(0, game.attemptsUsed)
    }

    @Test
    fun `makes its own secret when it is given none`() {
        val game = newGame(5, null, Random(7))

        assertEquals(5, game.length)
        assertTrue(isCode(game.secret))
    }

    @Test
    fun `replaces a secret the rules will not take, rather than refusing to start`() {
        // A watch game that throws on start is a black screen, and there is no way
        // for a player to supply a secret anyway.
        val game = newGame(4, listOf(1, 1, 1, 1), Random(3))

        assertTrue(isCode(game.secret))
    }
}

class GuessTest {
    @Test
    fun `answers a guess with its bulls and cows and remembers it`() {
        val played = gameOf().guessed(listOf(1, 2, 4, 3))

        assertTrue(played.accepted)
        assertEquals(Attempt(listOf(1, 2, 4, 3), bulls = 2, cows = 2), played.entry)
        assertEquals(1, played.game.attemptsUsed)
        assertEquals(Status.RUNNING, played.game.status)
    }

    @Test
    fun `ends the game when every digit is a bull, and in no other way`() {
        val played = gameOf().guessed(SECRET)

        assertEquals(Status.WON, played.game.status)
        assertEquals(1, played.game.attemptsUsed)
    }

    @Test
    fun `never runs out of guesses`() {
        var game = gameOf()
        repeat(50) { game = game.guessed(listOf(5, 6, 7, 8)).game }

        assertEquals(Status.RUNNING, game.status)
        assertEquals(50, game.attemptsUsed)
    }

    @Test
    fun `refuses a guess of the wrong length`() {
        val game = gameOf()
        val played = game.guessed(listOf(1, 2, 3))

        assertFalse(played.accepted)
        assertEquals(Refusal.WRONG_LENGTH, played.refusal)
        assertSame(game, played.game)
    }

    @Test
    fun `refuses a guess with a repeated digit`() {
        assertEquals(Refusal.REPEATED_DIGIT, gameOf().guessed(listOf(1, 1, 2, 3)).refusal)
    }

    @Test
    fun `refuses a guess that is not made of digits`() {
        assertEquals(Refusal.NOT_A_DIGIT, gameOf().guessed(listOf(1, 2, 3, 42)).refusal)
    }

    @Test
    fun `refuses a guess once the game is won`() {
        val won = gameOf().guessed(SECRET).game

        assertEquals(Refusal.NOT_RUNNING, won.guessed(listOf(5, 6, 7, 8)).refusal)
    }

    @Test
    fun `has nothing to refuse about a legal guess`() {
        assertNull(gameOf().refusalFor(listOf(9, 8, 7, 6)))
    }
}

class KeypadRulesTest {
    private val game = gameOf()

    @Test
    fun `spends a digit for the rest of the guess being composed`() {
        assertFalse(digitTaken(listOf(1, 2), 3))
        assertTrue(digitTaken(listOf(1, 2), 2))
    }

    @Test
    fun `takes a digit the guess has room for`() {
        assertTrue(game.acceptsDigit(listOf(1, 2), 3))
    }

    @Test
    fun `refuses a digit already placed`() {
        assertFalse(game.acceptsDigit(listOf(1, 2), 1))
    }

    @Test
    fun `refuses a digit once the guess is as long as the code`() {
        assertFalse(game.acceptsDigit(listOf(1, 2, 3, 4), 5))
    }

    @Test
    fun `refuses anything that is not a digit`() {
        assertFalse(game.acceptsDigit(emptyList(), 42))
    }

    @Test
    fun `refuses every digit once the game is won`() {
        val won = game.guessed(SECRET).game

        assertFalse(won.acceptsDigit(emptyList(), 5))
    }
}
