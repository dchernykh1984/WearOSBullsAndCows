package com.dchernykh.bullsandcows.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizeResultTest {
    @Test
    fun `keeps a real result`() {
        assertEquals(Result(7, 90), normalizeResult(7, 90))
    }

    @Test
    fun `reads nothing stored as never solved`() {
        assertNull(normalizeResult(null, 90))
        assertNull(normalizeResult(0, 90))
        assertNull(normalizeResult(-3, 90))
    }

    @Test
    fun `keeps a record that was never timed`() {
        // A record set before the game measured time is a real record with no
        // time, and it has to keep working rather than sort as though it took no
        // time at all.
        assertEquals(Result(7, null), normalizeResult(7, null))
        assertEquals(Result(7, null), normalizeResult(7, -1))
    }
}

class BeatsTest {
    @Test
    fun `puts fewer guesses first`() {
        assertTrue(beats(Result(5, 500), Result(6, 10)))
        assertFalse(beats(Result(6, 10), Result(5, 500)))
    }

    @Test
    fun `settles equal guesses by the shorter game`() {
        assertTrue(beats(Result(5, 40), Result(5, 90)))
        assertFalse(beats(Result(5, 90), Result(5, 40)))
        assertFalse(beats(Result(5, 40), Result(5, 40)))
    }

    @Test
    fun `never displaces an equal record on a time that is not there`() {
        // There is nothing to compare, so the record already on the watch keeps
        // its place.
        assertFalse(beats(Result(5, null), Result(5, 90)))
        assertFalse(beats(Result(5, 90), Result(5, null)))
    }

    @Test
    fun `takes any real result over none at all`() {
        assertTrue(beats(Result(99, null), null))
        assertFalse(beats(null, Result(5, 40)))
        assertFalse(beats(null, null))
    }
}

class UpdateBestTest {
    @Test
    fun `takes the first solve as the record`() {
        val outcome = updateBest(null, Result(9, 120))

        assertEquals(Result(9, 120), outcome.best)
        assertTrue(outcome.isRecord)
    }

    @Test
    fun `records a better game`() {
        val outcome = updateBest(Result(9, 120), Result(7, 300))

        assertEquals(Result(7, 300), outcome.best)
        assertTrue(outcome.isRecord)
    }

    @Test
    fun `keeps the record when the game was worse`() {
        val outcome = updateBest(Result(7, 300), Result(9, 10))

        assertEquals(Result(7, 300), outcome.best)
        assertFalse(outcome.isRecord)
    }
}

class TimingTest {
    @Test
    fun `measures whole seconds between two readings`() {
        assertEquals(4, elapsedSeconds(1_000, 5_400))
        assertEquals(0, elapsedSeconds(1_000, 1_000))
    }

    @Test
    fun `reports nothing when the watch's clock moved backwards`() {
        // A negative duration is better shown as nothing than as a record nobody
        // can beat.
        assertNull(elapsedSeconds(5_000, 1_000))
    }

    @Test
    fun `writes a duration as minutes and seconds`() {
        assertEquals("0:07", formatDuration(7))
        assertEquals("4:31", formatDuration(271))
    }

    @Test
    fun `never rolls the minutes up into hours`() {
        // A game that ran for an hour is a game somebody walked away from, and
        // "1:08:05" would only make that harder to read at a glance.
        assertEquals("128:05", formatDuration(128 * 60 + 5))
    }

    @Test
    fun `writes the placeholder for a game with no time`() {
        assertEquals(NO_TIME_TEXT, formatDuration(null))
        assertEquals(NO_TIME_TEXT, formatDuration(-1))
    }
}

class LevelTest {
    @Test
    fun `runs from the quick game to the long one`() {
        assertEquals(listOf(3, 4, 5), Level.entries.map { it.length })
        assertEquals(4, Level.DEFAULT.length)
    }

    @Test
    fun `cycles through every level and back`() {
        var level = Level.entries.first()
        repeat(Level.entries.size) { level = level.next }
        assertEquals(Level.entries.first(), level)
    }

    @Test
    fun `reads back a stored level`() {
        for (level in Level.entries) assertEquals(level, Level.fromStoredName(level.name))
    }

    @Test
    fun `falls back to the classic game, not to the first level`() {
        assertEquals(Level.DEFAULT, Level.fromStoredName(null))
        assertEquals(Level.DEFAULT, Level.fromStoredName("IMPOSSIBLE"))
    }

    @Test
    fun `gives every level a length the rules accept`() {
        for (level in Level.entries) {
            assertTrue(level.length in MIN_LENGTH..MAX_LENGTH)
        }
    }
}
