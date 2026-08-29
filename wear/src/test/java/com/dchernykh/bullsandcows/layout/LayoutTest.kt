package com.dchernykh.bullsandcows.layout

import com.dchernykh.bullsandcows.ui.HISTORY_ROWS
import com.dchernykh.bullsandcows.ui.SCREEN_PADDING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/** The round sizes Wear OS watches actually come in, small to large. */
private val SCREENS = listOf(384, 416, 454, 466, 480)

private const val KEYS = 10

class KeypadTest {
    @Test
    fun `puts one key in each slot, evenly round the ring`() {
        for (screen in SCREENS) {
            val keypad = keypadLayout(screen, KEYS)

            assertEquals(KEYS, keypad.slots.size)
            assertTrue(keypad.keySize > 0)
            for (slot in keypad.slots) {
                assertEquals(keypad.keySize, slot.w)
                assertEquals(keypad.keySize, slot.h)
            }
        }
    }

    @Test
    fun `starts at the top and runs clockwise, like the hours on a face`() {
        val keypad = keypadLayout(466, KEYS)
        val centre = 466 / 2f
        val first = keypad.slots.first()
        val quarter = keypad.slots[KEYS / 4]

        // The first key is above the centre; a quarter of the way round is right of it.
        assertTrue(first.y + first.h / 2f < centre)
        assertTrue(abs(first.x + first.w / 2f - centre) <= 1f)
        assertTrue(quarter.x + quarter.w / 2f > centre)
    }

    @Test
    fun `keeps every key on the round screen, clear of the bezel`() {
        // A key is a circle, so what has to fit is the circle: its own radius
        // added to how far its centre sits from the middle of the screen. Its
        // square bounding box has corners the drawn key never reaches.
        for (screen in SCREENS) {
            val keypad = keypadLayout(screen, KEYS)
            val centre = screen / 2f
            for (slot in keypad.slots) {
                val reach =
                    hypot(slot.x + slot.w / 2f - centre, slot.y + slot.h / 2f - centre) + slot.w / 2f
                assertTrue("a key overhangs a $screen screen", reach <= centre)
            }
        }
    }

    @Test
    fun `sits every key the same distance from the centre`() {
        val screen = 466
        val keypad = keypadLayout(screen, KEYS)
        val centre = screen / 2f

        for (slot in keypad.slots) {
            val distance = hypot(slot.x + slot.w / 2f - centre, slot.y + slot.h / 2f - centre)
            assertEquals(keypad.radius, distance, 1.5f)
        }
    }

    @Test
    fun `leaves a free circle inside the ring, clear of the keys`() {
        for (screen in SCREENS) {
            val keypad = keypadLayout(screen, KEYS)

            assertTrue(keypad.innerRadius > 0)
            assertTrue(keypad.innerRadius < keypad.radius - keypad.keySize / 2f + 1)
        }
    }

    @Test
    fun `still lays out a ring of one key`() {
        assertEquals(1, keypadLayout(466, 1).slots.size)
        assertEquals(1, keypadLayout(466, 0).slots.size)
    }
}

class BoardStackTest {
    private fun stackFor(screen: Int) =
        boardStack(screen, keypadLayout(screen, KEYS).innerRadius, HISTORY_ROWS, SCREEN_PADDING)

    @Test
    fun `stacks the board top to bottom without overlapping itself`() {
        for (screen in SCREENS) {
            val stack = stackFor(screen)
            val tops = listOf(stack.counter) + stack.history + listOf(stack.guess, stack.actions)

            for (i in 0 until tops.size - 1) {
                assertTrue(
                    "rows overlap on a $screen screen",
                    tops[i].y + tops[i].h <= tops[i + 1].y,
                )
            }
        }
    }

    @Test
    fun `fits the whole stack inside the circle the ring leaves free`() {
        for (screen in SCREENS) {
            val keypad = keypadLayout(screen, KEYS)
            val stack = stackFor(screen)
            val boxes = listOf(stack.counter) + stack.history + listOf(stack.guess, stack.actions)

            for (box in boxes) {
                assertInsideCircle(screen, keypad.innerRadius, box, "a board row")
            }
        }
    }

    @Test
    fun `squares the history rows off to one set of columns`() {
        // Left to itself each row takes the full chord at its own height, and the
        // guesses would step sideways from line to line.
        for (screen in SCREENS) {
            val history = stackFor(screen).history
            val widths = history.map { it.w }.toSet()
            val lefts = history.map { it.x }.toSet()

            assertEquals(1, widths.size)
            assertEquals(1, lefts.size)
        }
    }

    @Test
    fun `shows exactly the rows it was asked for`() {
        assertEquals(HISTORY_ROWS, stackFor(466).history.size)
        assertEquals(1, boardStack(466, 150f, 0, SCREEN_PADDING).history.size)
    }

    @Test
    fun `centres the stack on the screen`() {
        for (screen in SCREENS) {
            val stack = stackFor(screen)
            val top = stack.counter.y
            val bottom = stack.actions.y + stack.actions.h

            assertTrue(abs((screen - bottom) - top) <= 2)
        }
    }
}

class ColumnsTest {
    private val row = Box(x = 100, y = 50, w = 300, h = 40)

    @Test
    fun `splits a row into equal columns`() {
        val columns = columnsIn(row, count = 3, gap = 10)

        assertEquals(3, columns.size)
        assertEquals(1, columns.map { it.w }.toSet().size)
        assertEquals(row.y, columns[0].y)
        assertEquals(row.h, columns[0].h)
    }

    @Test
    fun `leaves the gap between columns and nowhere else`() {
        val columns = columnsIn(row, count = 3, gap = 10)

        assertEquals(columns[0].x + columns[0].w + 10, columns[1].x)
        assertEquals(columns[1].x + columns[1].w + 10, columns[2].x)
    }

    @Test
    fun `centres the columns in the row`() {
        val columns = columnsIn(row, count = 3, gap = 10)
        val left = columns.first().x - row.x
        val right = row.x + row.w - (columns.last().x + columns.last().w)

        assertTrue(abs(left - right) <= 1)
    }

    @Test
    fun `caps a column's width, so a short guess does not get enormous slots`() {
        val columns = columnsIn(row, count = 3, gap = 10, maxWidth = 40)

        assertEquals(40, columns[0].w)
        // Still centred, with the leftover shared either side.
        assertTrue(columns.first().x > row.x)
    }

    @Test
    fun `always makes at least one column, of at least one pixel`() {
        assertEquals(1, columnsIn(row, count = 0, gap = 0).size)
        assertTrue(columnsIn(Box(0, 0, 2, 10), count = 5, gap = 10)[0].w >= 1)
    }
}
