package com.dchernykh.bullsandcows.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import com.dchernykh.bullsandcows.BullsAndCowsViewModel
import com.dchernykh.bullsandcows.Screen
import com.dchernykh.bullsandcows.layout.boardStack
import com.dchernykh.bullsandcows.layout.keypadLayout
import kotlin.math.abs

/** How many keys the ring carries: one per decimal digit. */
private const val KEYS = 10

/**
 * The whole screen: the ring of digit keys, the board it encloses, and whichever
 * menu is in front.
 *
 * The layout is worked out once from the screen diameter and then everything is
 * placed at absolute pixels, which is what keeps the port looking like the game it
 * was ported from on any round watch.
 */
@Composable
fun BullsAndCowsApp(viewModel: BullsAndCowsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The screen is a circle, so one diameter drives every measurement.
    val container = LocalWindowInfo.current.containerSize
    val screenSize = minOf(container.width, container.height)
    // A screen of no size divides into negative boxes rather than into nothing.
    if (screenSize <= 0) return

    val keypad = remember(screenSize) { keypadLayout(screenSize, KEYS) }
    val stack =
        remember(screenSize, keypad) {
            boardStack(screenSize, keypad.innerRadius, HISTORY_ROWS, SCREEN_PADDING)
        }
    val menu = remember(screenSize) { MenuMetrics(screenSize) }

    // Bulls and Cows is played in long thinking pauses with nothing touching the
    // screen, and a ten-second display timeout would black out mid-deduction.
    KeepScreenOnWhile(state.screen == Screen.PLAYING)

    // Wear OS reads a swipe from the left edge as Back. From a game it goes to the
    // menu, keeping the game so Continue can pick it up; from a menu it steps back
    // towards the start screen, and from there it is left alone so the watch closes
    // the app as it does any other.
    BackHandler(enabled = state.screen != Screen.START) { viewModel.showStart() }

    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(ColorBackground)
                    .historySwipes(state.screen, viewModel),
        ) {
            if (state.screen == Screen.PLAYING) {
                KeypadRing(keypad, state, viewModel)
                Counter(stack, state, viewModel)
                History(stack, state)
                GuessRow(stack, state)
                Actions(stack, state, viewModel)
            }

            Screens(screenSize, menu, state, viewModel)
        }
    }
}

@Composable
private fun KeepScreenOnWhile(playing: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, playing) {
        view.keepScreenOn = playing
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * Vertical swipes walk the history one row at a time, and stop at both ends.
 *
 * They are the second way to move it, not the only one: the counter doubles as a
 * pager because vertical swipes belong to the system on a watch and do not
 * reliably reach an app. A tap that always works beats a gesture that usually does.
 */
private fun Modifier.historySwipes(
    screen: Screen,
    viewModel: BullsAndCowsViewModel,
): Modifier =
    pointerInput(screen) {
        if (screen != Screen.PLAYING) return@pointerInput
        var handled = false
        detectDragGestures(
            onDragStart = { handled = false },
            onDragEnd = { handled = false },
            onDragCancel = { handled = false },
        ) { change, drag ->
            change.consume()
            if (!handled && abs(drag.y) > abs(drag.x)) {
                handled = true
                // Pulling down brings the earlier guesses into view, which is what
                // a finger expects of any list.
                viewModel.scrollHistory(if (drag.y > 0) -1 else 1)
            }
        }
    }
