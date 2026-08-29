package com.dchernykh.bullsandcows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.dchernykh.bullsandcows.game.GameState
import com.dchernykh.bullsandcows.game.Level
import com.dchernykh.bullsandcows.game.Result
import com.dchernykh.bullsandcows.game.Status
import com.dchernykh.bullsandcows.game.acceptsDigit
import com.dchernykh.bullsandcows.game.elapsedSeconds
import com.dchernykh.bullsandcows.game.guessed
import com.dchernykh.bullsandcows.game.maxOffset
import com.dchernykh.bullsandcows.game.newGame
import com.dchernykh.bullsandcows.game.pageBack
import com.dchernykh.bullsandcows.game.scrollBy
import com.dchernykh.bullsandcows.game.updateBest
import com.dchernykh.bullsandcows.store.RecordStore
import com.dchernykh.bullsandcows.ui.HISTORY_ROWS
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Which of the four screens is in front. */
enum class Screen { START, PLAYING, SOLVED, RECORDS }

/** Everything the screen draws. */
data class BullsAndCowsUiState(
    val screen: Screen = Screen.START,
    val level: Level = Level.DEFAULT,
    val best: Result? = null,
    val game: GameState? = null,
    /** The guess being composed, digit by digit. */
    val entered: List<Int> = emptyList(),
    /** The first history row on screen; the window follows the newest guess. */
    val offset: Int = 0,
    val seconds: Int? = null,
    val isRecord: Boolean = false,
    val records: Map<Level, Result?> = emptyMap(),
)

/**
 * The game as the screen sees it.
 *
 * [now] is injected rather than read from the system, which is the whole of what
 * makes a timed game testable; [random] likewise, so a test can play a known code.
 */
class BullsAndCowsViewModel(
    private val store: RecordStore,
    private val random: Random = Random.Default,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BullsAndCowsUiState())
    val uiState: StateFlow<BullsAndCowsUiState> = _uiState.asStateFlow()

    private var startedAt: Long = 0

    // Every touch of storage goes through this, each waiting on the one before, so
    // a read can never overtake the write it should have seen.
    private var settings: Job = Job().apply { complete() }

    init {
        settings =
            viewModelScope.launch {
                val level = store.readLevel()
                _uiState.update { it.copy(level = level, best = store.readBest(level)) }
            }
    }

    /** Walk to the next difficulty and remember it, with that level's record. */
    fun cycleLevel() {
        val next = _uiState.value.level.next
        val previous = settings
        settings =
            viewModelScope.launch {
                previous.join()
                store.writeLevel(next)
                _uiState.update { it.copy(level = next, best = store.readBest(next)) }
            }
    }

    /**
     * Deal a new code.
     *
     * A game already in progress is dropped, which costs nothing: an abandoned
     * game is never a loss and never touches a record.
     */
    fun startGame() {
        startedAt = now()
        _uiState.update {
            it.copy(
                screen = Screen.PLAYING,
                game = newGame(it.level.length, random = random),
                entered = emptyList(),
                offset = 0,
                seconds = null,
                isRecord = false,
            )
        }
    }

    /** Pick the game back up exactly where it was, half-typed guess and all. */
    fun resumeGame() {
        if (_uiState.value.game?.status != Status.RUNNING) return
        _uiState.update { it.copy(screen = Screen.PLAYING) }
    }

    /** Add a digit to the guess being composed, if the rules will take it. */
    fun enterDigit(digit: Int) {
        val state = _uiState.value
        val game = state.game ?: return
        if (state.screen != Screen.PLAYING || !game.acceptsDigit(state.entered, digit)) return
        _uiState.update { it.copy(entered = it.entered + digit) }
    }

    /** Take the last digit back. */
    fun eraseDigit() {
        if (_uiState.value.screen != Screen.PLAYING) return
        _uiState.update { it.copy(entered = it.entered.dropLast(1)) }
    }

    /** Play the guess. A guess the rules refuse leaves everything as it was. */
    fun submitGuess() {
        val state = _uiState.value
        val game = state.game ?: return
        if (state.screen != Screen.PLAYING) return
        val played = game.guessed(state.entered)
        if (!played.accepted) return

        val next = played.game
        _uiState.update {
            it.copy(
                game = next,
                entered = emptyList(),
                // The window follows the newest guess, which is where a player
                // wants to be after playing one.
                offset = maxOffset(next.history.size, HISTORY_ROWS),
            )
        }
        if (next.status == Status.WON) finish(next)
    }

    /** One page back through the history, wrapping once the oldest is on screen. */
    fun pageHistory() {
        val game = _uiState.value.game ?: return
        _uiState.update { it.copy(offset = pageBack(game.history.size, HISTORY_ROWS, it.offset)) }
    }

    /** One row at a time, which is what a vertical swipe does. */
    fun scrollHistory(delta: Int) {
        val game = _uiState.value.game ?: return
        _uiState.update { it.copy(offset = scrollBy(game.history.size, HISTORY_ROWS, it.offset, delta)) }
    }

    fun showStart() {
        _uiState.update { it.copy(screen = Screen.START, isRecord = false) }
    }

    /** The table of every level's record, read fresh so it cannot show a stale one. */
    fun showRecords() {
        val previous = settings
        settings =
            viewModelScope.launch {
                previous.join()
                val rows = Level.entries.associateWith { store.readBest(it) }
                _uiState.update { it.copy(screen = Screen.RECORDS, records = rows) }
            }
    }

    /**
     * The code is cracked. The board is left on screen under the panel, and a
     * record is written only when there is one.
     */
    private fun finish(won: GameState) {
        val seconds = elapsedSeconds(startedAt, now())
        val state = _uiState.value
        val outcome = updateBest(state.best, Result(won.attemptsUsed, seconds))
        _uiState.update {
            it.copy(
                screen = Screen.SOLVED,
                seconds = seconds,
                best = outcome.best,
                isRecord = outcome.isRecord,
            )
        }
        val best = outcome.best
        if (!outcome.isRecord || best == null) return
        val previous = settings
        settings =
            viewModelScope.launch {
                previous.join()
                // Not cancellable. The app being closed the instant a code falls
                // is exactly when a record is worth keeping.
                withContext(NonCancellable) { store.writeBest(state.level, best) }
            }
    }

    companion object {
        fun factory(store: RecordStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return BullsAndCowsViewModel(store) as T
                }
            }
    }
}
