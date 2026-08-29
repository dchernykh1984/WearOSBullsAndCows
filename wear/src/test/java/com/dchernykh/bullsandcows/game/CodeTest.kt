package com.dchernykh.bullsandcows.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DigitTest {
    @Test
    fun `knows a decimal digit when it sees one`() {
        for (d in 0 until DIGIT_COUNT) assertTrue(isDigit(d))
        assertFalse(isDigit(-1))
        assertFalse(isDigit(DIGIT_COUNT))
    }

    @Test
    fun `spots a repeated digit`() {
        assertFalse(hasRepeats(listOf(1, 2, 3)))
        assertTrue(hasRepeats(listOf(1, 2, 1)))
        assertFalse(hasRepeats(emptyList()))
    }
}

class IsCodeTest {
    @Test
    fun `takes distinct digits of a legal length`() {
        assertTrue(isCode(listOf(1, 2, 3, 4)))
        assertTrue(isCode(List(MAX_LENGTH) { it }))
        assertTrue(isCode(List(MIN_LENGTH) { it }))
    }

    @Test
    fun `refuses a code that is too short or too long`() {
        assertFalse(isCode(listOf(1)))
        assertFalse(isCode(List(MAX_LENGTH + 1) { it }))
    }

    @Test
    fun `refuses anything that is not a digit`() {
        assertFalse(isCode(listOf(1, 2, 10)))
        assertFalse(isCode(listOf(1, -2)))
    }

    @Test
    fun `refuses a repeated digit`() {
        assertFalse(isCode(listOf(1, 2, 1)))
    }

    @Test
    fun `never has to choose between the two rules`() {
        // A distinct-digit code always fits in the ten digits, so the length rule
        // and the repeat rule can never contradict each other.
        assertTrue(MAX_LENGTH <= DIGIT_COUNT)
    }
}

class ScoreGuessTest {
    @Test
    fun `counts a digit in the right place as a bull`() {
        assertEquals(Score(bulls = 4, cows = 0), scoreGuess(listOf(1, 2, 3, 4), listOf(1, 2, 3, 4)))
    }

    @Test
    fun `counts a digit in the wrong place as a cow`() {
        assertEquals(Score(bulls = 0, cows = 4), scoreGuess(listOf(1, 2, 3, 4), listOf(4, 3, 2, 1)))
    }

    @Test
    fun `mixes the two`() {
        assertEquals(Score(bulls = 2, cows = 2), scoreGuess(listOf(1, 2, 3, 4), listOf(1, 2, 4, 3)))
    }

    @Test
    fun `counts nothing for a guess with nothing in common`() {
        assertEquals(Score(bulls = 0, cows = 0), scoreGuess(listOf(1, 2, 3), listOf(4, 5, 6)))
    }

    @Test
    fun `never counts a digit twice, even with repeats`() {
        // 1122 against 1213 is one bull (the first 1) and two cows (the second 1
        // and one 2), not four of something. Codes in this game are always
        // distinct, but the rule has to be right for the general case or it
        // quietly stops being right the day the game changes.
        assertEquals(Score(bulls = 1, cows = 2), scoreGuess(listOf(1, 2, 1, 3), listOf(1, 1, 2, 2)))
    }

    @Test
    fun `bulls and cows together never exceed the code`() {
        val secret = listOf(1, 2, 3, 4)
        for (seed in 0 until 200) {
            val guess = makeSecret(4, Random(seed))
            val score = scoreGuess(secret, guess)
            assertTrue(score.bulls + score.cows <= secret.size)
        }
    }
}

class MakeSecretTest {
    @Test
    fun `makes a code the rules accept`() {
        for (seed in 0 until 200) {
            for (length in MIN_LENGTH..MAX_LENGTH) {
                val secret = makeSecret(length, Random(seed))
                assertEquals(length, secret.size)
                assertTrue("seed $seed length $length gave $secret", isCode(secret))
            }
        }
    }

    @Test
    fun `never starts with a zero`() {
        // The official rule: a leading zero reads as a shorter number on the
        // screen, and the player would have to be told it can happen.
        for (seed in 0 until 500) {
            assertTrue(makeSecret(4, Random(seed)).first() != 0)
        }
    }

    @Test
    fun `still puts a zero somewhere after the first place`() {
        val withZero = (0 until 500).map { makeSecret(4, Random(it)) }.any { 0 in it }
        assertTrue("zero should still be reachable", withZero)
    }

    @Test
    fun `clamps a length the rules will not take`() {
        assertEquals(MIN_LENGTH, makeSecret(0, Random(0)).size)
        assertEquals(MAX_LENGTH, makeSecret(99, Random(0)).size)
    }
}

class CodeToTextTest {
    @Test
    fun `writes the digits as the screen shows them`() {
        assertEquals("1234", codeToText(listOf(1, 2, 3, 4)))
        assertEquals("", codeToText(emptyList()))
    }

    @Test
    fun `marks anything that is not a digit`() {
        assertEquals("1?3", codeToText(listOf(1, 42, 3)))
    }
}
