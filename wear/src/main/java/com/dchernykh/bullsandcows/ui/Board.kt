package com.dchernykh.bullsandcows.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material3.Text
import com.dchernykh.bullsandcows.BullsAndCowsUiState
import com.dchernykh.bullsandcows.BullsAndCowsViewModel
import com.dchernykh.bullsandcows.R
import com.dchernykh.bullsandcows.game.Attempt
import com.dchernykh.bullsandcows.game.codeToText
import com.dchernykh.bullsandcows.game.digitTaken
import com.dchernykh.bullsandcows.game.windowLabel
import com.dchernykh.bullsandcows.game.windowOf
import com.dchernykh.bullsandcows.layout.BoardStack
import com.dchernykh.bullsandcows.layout.Keypad
import com.dchernykh.bullsandcows.layout.columnsIn
import com.dchernykh.bullsandcows.layout.Box as LayoutBox

/** The ring of ten digit keys, just inside the bezel. */
@Composable
fun KeypadRing(
    keypad: Keypad,
    state: BullsAndCowsUiState,
    viewModel: BullsAndCowsViewModel,
) {
    keypad.slots.forEachIndexed { digit, slot ->
        DigitKey(
            box = slot,
            digit = digit,
            taken = digitTaken(state.entered, digit),
            label = stringResource(R.string.digit_key, digit),
            onClick = { viewModel.enterDigit(digit) },
        )
    }
}

/**
 * The counter above the history.
 *
 * While everything played fits on screen it is a plain count of guesses. Once it
 * does not, it becomes the pager - "4-6/12" - and tapping it walks a screenful
 * back through the older guesses. The tap is the control that always works:
 * vertical swipes belong to the system on a watch and do not reliably reach an app.
 */
@Composable
fun Counter(
    stack: BoardStack,
    state: BullsAndCowsUiState,
    viewModel: BullsAndCowsViewModel,
) {
    val played = state.game?.history?.size ?: 0
    val pager = windowLabel(played, HISTORY_ROWS, state.offset)
    val box = stack.counter
    if (pager == null) {
        Box(modifier = Modifier.absoluteBox(box), contentAlignment = Alignment.Center) {
            Text(
                text = played.toString(),
                color = ColorMuted,
                fontSize = with(LocalDensity.current) { (box.h * 0.6f).toSp() },
                maxLines = 1,
            )
        }
    } else {
        PillButton(
            box = box,
            text = pager,
            onClick = viewModel::pageHistory,
            label = stringResource(R.string.pager),
        )
    }
}

/** The window onto the guesses played, newest at the bottom. */
@Composable
fun History(
    stack: BoardStack,
    state: BullsAndCowsUiState,
) {
    val played = state.game?.history.orEmpty()
    val rows = windowOf(played, HISTORY_ROWS, state.offset)
    val bull = stringResource(R.string.bull_mark)
    val cow = stringResource(R.string.cow_mark)
    stack.history.forEachIndexed { index, box ->
        rows.getOrNull(index)?.let { HistoryRow(box, it, bull, cow) }
    }
}

/** One played guess: the code, then its bulls and its cows - `1234 0B 4C`. */
@Composable
private fun HistoryRow(
    box: LayoutBox,
    attempt: Attempt,
    bullMark: String,
    cowMark: String,
) {
    val columns = columnsIn(box, count = 3, gap = 0)
    RowCell(columns[0], codeToText(attempt.digits), ColorText, FontWeight.Medium)
    RowCell(columns[1], "${attempt.bulls}$bullMark", ColorBull, FontWeight.Normal)
    RowCell(columns[2], "${attempt.cows}$cowMark", ColorCow, FontWeight.Normal)
}

@Composable
private fun RowCell(
    box: LayoutBox,
    text: String,
    color: androidx.compose.ui.graphics.Color,
    weight: FontWeight,
) {
    Box(modifier = Modifier.absoluteBox(box), contentAlignment = Alignment.Center) {
        FittedText(text = text, color = color, boxHeight = box.h, boxWidth = box.w, fraction = 0.8f, weight = weight)
    }
}

/** The guess being composed, one slot per digit of the code. */
@Composable
fun GuessRow(
    stack: BoardStack,
    state: BullsAndCowsUiState,
) {
    val length = state.game?.length ?: return
    val slots = columnsIn(stack.guess, count = length, gap = stack.gap, maxWidth = stack.guess.h)
    slots.forEachIndexed { index, slot ->
        GuessSlot(box = slot, digit = state.entered.getOrNull(index), isNext = index == state.entered.size)
    }
}

/** Erase and OK. OK only lights up once the guess is as long as the code. */
@Composable
fun Actions(
    stack: BoardStack,
    state: BullsAndCowsUiState,
    viewModel: BullsAndCowsViewModel,
) {
    val length = state.game?.length ?: return
    val columns = columnsIn(stack.actions, count = 2, gap = stack.gap)
    PillButton(
        box = columns[0],
        text = stringResource(R.string.erase),
        onClick = viewModel::eraseDigit,
        enabled = state.entered.isNotEmpty(),
    )
    PillButton(
        box = columns[1],
        text = stringResource(R.string.check),
        onClick = viewModel::submitGuess,
        accented = true,
        enabled = state.entered.size == length,
    )
}
