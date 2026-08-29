package com.dchernykh.bullsandcows.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dchernykh.bullsandcows.BullsAndCowsUiState
import com.dchernykh.bullsandcows.BullsAndCowsViewModel
import com.dchernykh.bullsandcows.R
import com.dchernykh.bullsandcows.Screen
import com.dchernykh.bullsandcows.game.Level
import com.dchernykh.bullsandcows.game.Result
import com.dchernykh.bullsandcows.game.Status
import com.dchernykh.bullsandcows.game.formatDuration
import com.dchernykh.bullsandcows.layout.centeredBox
import kotlin.math.roundToInt

// The three menus. They live one file away from the shell that hosts them because
// they are what changes when the game gains a screen, and the shell is what does
// not.

/** Whichever menu is in front, or none at all while a game is being played. */
@Composable
fun Screens(
    screenSize: Int,
    metrics: MenuMetrics,
    state: BullsAndCowsUiState,
    viewModel: BullsAndCowsViewModel,
) {
    when (state.screen) {
        Screen.PLAYING -> Unit
        Screen.START -> StartMenu(screenSize, metrics, state, viewModel)
        Screen.SOLVED -> SolvedMenu(screenSize, metrics, state, viewModel)
        Screen.RECORDS -> RecordsScreen(screenSize, metrics, state, viewModel)
    }
}

@Composable
private fun StartMenu(
    screenSize: Int,
    metrics: MenuMetrics,
    state: BullsAndCowsUiState,
    viewModel: BullsAndCowsViewModel,
) {
    val items = mutableListOf<MenuItem>()
    items += MenuItem.Line(metrics.big, ColorText, stringResource(R.string.app_name))
    items += MenuItem.Gap(metrics.gap)
    items += MenuItem.Line(metrics.small, ColorMuted, stringResource(R.string.best_value, bestText(state.best)))
    items += MenuItem.Gap(metrics.gap)
    // A game put aside is still there: Continue picks it up exactly where it was,
    // half-typed guess and all. It appears only when there is one to pick up.
    if (state.game?.status == Status.RUNNING) {
        items += MenuItem.Action(metrics.button, stringResource(R.string.resume), viewModel::resumeGame)
        items += MenuItem.Gap(metrics.gap)
    }
    items += MenuItem.Line(metrics.small, ColorMuted, stringResource(R.string.level))
    items += MenuItem.Action(metrics.button, stringResource(state.level.labelRes), viewModel::cycleLevel)
    items += MenuItem.Gap(metrics.gap)
    items += MenuItem.Action(metrics.button, stringResource(R.string.play), viewModel::startGame)
    items += MenuItem.Action(metrics.button, stringResource(R.string.records), viewModel::showRecords)
    items += MenuItem.Line(metrics.small, ColorMuted, stringResource(R.string.hint))
    items += MenuItem.Line(metrics.small, ColorMuted, stringResource(R.string.legend))

    MenuOverlay(screenSize, metrics, items)
}

@Composable
private fun SolvedMenu(
    screenSize: Int,
    metrics: MenuMetrics,
    state: BullsAndCowsUiState,
    viewModel: BullsAndCowsViewModel,
) {
    MenuOverlay(
        screenSize = screenSize,
        metrics = metrics,
        items =
            listOf(
                // A solved code is all bulls, which is why the headline is the
                // bull colour and a new best is the cow one: the two lines of a
                // won game must not read as one.
                MenuItem.Line(metrics.big, ColorBull, stringResource(R.string.solved)),
                MenuItem.Gap(metrics.gap),
                MenuItem.Line(metrics.row, ColorText, stringResource(R.string.tries_value, tries(state))),
                MenuItem.Line(
                    metrics.row,
                    ColorText,
                    stringResource(R.string.time_value, formatDuration(state.seconds)),
                ),
                MenuItem.Line(
                    metrics.row,
                    if (state.isRecord) ColorCow else ColorMuted,
                    if (state.isRecord) {
                        stringResource(R.string.new_best)
                    } else {
                        stringResource(R.string.best_value, bestText(state.best))
                    },
                ),
                MenuItem.Gap(metrics.gap),
                MenuItem.Action(metrics.button, stringResource(R.string.again), viewModel::showStart),
            ),
    )
}

/** Every level with whatever record it holds, best first as the ladder runs. */
@Composable
private fun RecordsScreen(
    screenSize: Int,
    metrics: MenuMetrics,
    state: BullsAndCowsUiState,
    viewModel: BullsAndCowsViewModel,
) {
    val rows = Level.entries.map { it to state.records[it] }
    val anySolved = rows.any { it.second != null }

    val items = mutableListOf<MenuItem>()
    items += MenuItem.Line(metrics.row, ColorText, stringResource(R.string.records))
    items += MenuItem.Gap(metrics.gap)
    if (!anySolved) {
        // Three dashes and a heading would read as a table of zero scores rather
        // than as a game nobody has finished yet.
        items += MenuItem.Line(metrics.row, ColorMuted, stringResource(R.string.no_records))
    }
    items += MenuItem.Gap(metrics.gap)
    items += MenuItem.Action(metrics.button, stringResource(R.string.back), viewModel::showStart)

    MenuOverlay(screenSize, metrics, items)

    if (!anySolved) return
    // The table itself is drawn over the stack, in the gap the heading leaves.
    val stackHeight = items.sumOf { it.height }
    var y = (screenSize / 2f).roundToInt() - (stackHeight / 2f).roundToInt() + metrics.row + metrics.gap
    for ((level, result) in rows) {
        val box = centeredBox(screenSize, screenSize / 2f, y, metrics.row, metrics.maxWidth, SCREEN_PADDING)
        RecordRow(
            box = box,
            level = stringResource(level.labelRes),
            attempts = result?.attempts?.toString() ?: stringResource(R.string.no_result),
            time = formatDuration(result?.seconds),
        )
        y += metrics.row
    }
}

@Composable
private fun bestText(best: Result?) = best?.attempts?.toString() ?: stringResource(R.string.no_result)

private fun tries(state: BullsAndCowsUiState) = (state.game?.attemptsUsed ?: 0).toString()
