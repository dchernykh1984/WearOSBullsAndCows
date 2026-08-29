package com.dchernykh.bullsandcows.ui

import androidx.compose.ui.graphics.Color

// The colours, carried over unchanged from the Zepp OS original so the two
// versions of the game look like the same game. On the OLED a watch uses, black is
// not a colour but pixels that are switched off, which is why it all sits on black.

/** Guesses visible at once inside the ring. Three keep the type readable at arm's length. */
const val HISTORY_ROWS = 3

/** The padding kept between anything centred and the edge of the circle it lives in. */
const val SCREEN_PADDING = 8

val ColorBackground = Color(0xFF000000)
val ColorPanel = Color(0xFF0C1013)
val ColorText = Color(0xFFFFFFFF)
val ColorMuted = Color(0xFF9AA4AB)

/**
 * A bull is a digit in the right place, a cow a digit in the wrong one. The two
 * keep their colours everywhere they appear: the counts in the history rows, the
 * solved headline and the revealed code in the bull colour - a solved code is all
 * bulls - and a new best in the cow colour, so the two lines of a won game do not
 * read as one.
 */
val ColorBull = Color(0xFFFFB020)
val ColorCow = Color(0xFF35C4A0)

/** The keys of the ring, and the look a key takes once its digit is spent. */
val ColorKey = Color(0xFF1D262C)
val ColorKeyPressed = Color(0xFF2F3D46)
val ColorKeyTaken = Color(0xFF11171B)
val ColorKeyTextTaken = Color(0xFF46525A)

/** The slots of the guess: empty, filled, and the one the next digit goes into. */
val ColorSlot = Color(0xFF161D22)
val ColorSlotNext = Color(0xFF27333B)
val ColorSlotFilled = Color(0xFF2F3D46)

/** Buttons: the neutral ones, and the accented one that plays a complete guess. */
val ColorButton = Color(0xFF1D262C)
val ColorButtonPressed = Color(0xFF2F3D46)
val ColorAccent = Color(0xFF1E7A56)
val ColorAccentPressed = Color(0xFF2AA073)
