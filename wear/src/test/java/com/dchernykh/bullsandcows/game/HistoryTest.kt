package com.dchernykh.bullsandcows.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val VISIBLE = 3

class WindowTest {
    @Test
    fun `has nowhere to scroll while everything fits`() {
        assertEquals(0, maxOffset(2, VISIBLE))
        assertEquals(0, maxOffset(VISIBLE, VISIBLE))
    }

    @Test
    fun `stops at the newest guess`() {
        assertEquals(9, maxOffset(12, VISIBLE))
        assertEquals(9, clampOffset(12, VISIBLE, 99))
    }

    @Test
    fun `reads an offset below the list as the top`() {
        assertEquals(0, clampOffset(12, VISIBLE, -4))
    }

    @Test
    fun `scrolls in both directions and stops at each end`() {
        assertEquals(4, scrollBy(12, VISIBLE, 3, 1))
        assertEquals(2, scrollBy(12, VISIBLE, 3, -1))
        assertEquals(0, scrollBy(12, VISIBLE, 0, -1))
        assertEquals(9, scrollBy(12, VISIBLE, 9, 1))
    }

    @Test
    fun `shows at most a windowful, starting where it was asked to`() {
        val entries = (1..12).toList()

        assertEquals(listOf(4, 5, 6), windowOf(entries, VISIBLE, 3))
        assertEquals(listOf(10, 11, 12), windowOf(entries, VISIBLE, 9))
        assertEquals(listOf(1, 2), windowOf(listOf(1, 2), VISIBLE, 0))
    }
}

class PageBackTest {
    @Test
    fun `walks a screenful back through the older guesses`() {
        assertEquals(6, pageBack(12, VISIBLE, 9))
        assertEquals(3, pageBack(12, VISIBLE, 6))
    }

    @Test
    fun `wraps to the newest once the oldest is on screen`() {
        // Paging only goes one way on purpose: it is driven by a single control,
        // and a control that always does the same thing is one a thumb learns in
        // two taps.
        assertEquals(9, pageBack(12, VISIBLE, 0))
    }

    @Test
    fun `stops short rather than running past the top`() {
        assertEquals(0, pageBack(12, VISIBLE, 2))
    }
}

class WindowLabelTest {
    @Test
    fun `says nothing while everything played fits on screen`() {
        assertNull(windowLabel(3, VISIBLE, 0))
        assertNull(windowLabel(0, VISIBLE, 0))
    }

    @Test
    fun `names the guesses on screen out of the guesses played`() {
        assertEquals("4-6/12", windowLabel(12, VISIBLE, 3))
        assertEquals("1-3/12", windowLabel(12, VISIBLE, 0))
        assertEquals("10-12/12", windowLabel(12, VISIBLE, 9))
    }

    @Test
    fun `never names a row that is not there`() {
        // An offset past the last full window is pulled back to it, so the label
        // never counts up to a guess that has not been played.
        assertEquals("2-4/4", windowLabel(4, VISIBLE, 2))
        assertEquals("2-4/4", windowLabel(4, VISIBLE, 99))
    }
}
