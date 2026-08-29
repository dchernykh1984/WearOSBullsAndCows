package com.dchernykh.bullsandcows

import com.dchernykh.bullsandcows.game.Level
import com.dchernykh.bullsandcows.game.Result
import com.dchernykh.bullsandcows.game.Status
import com.dchernykh.bullsandcows.store.RecordStore
import com.dchernykh.bullsandcows.ui.HISTORY_ROWS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/** An in-memory stand-in for the watch's storage. */
private class FakeRecordStore(
    var level: Level = Level.DEFAULT,
    private val bests: MutableMap<Level, Result> = mutableMapOf(),
) : RecordStore {
    var writes = 0
        private set

    override suspend fun readLevel(): Level = level

    override suspend fun writeLevel(level: Level) {
        this.level = level
    }

    override suspend fun readBest(level: Level): Result? = bests[level]

    override suspend fun writeBest(
        level: Level,
        best: Result,
    ) {
        bests[level] = best
        writes++
    }
}

/** A clock the test winds by hand, so a timed game needs no waiting. */
private class FakeClock(
    var now: Long = 1_000L,
) : () -> Long {
    override fun invoke(): Long = now
}

@OptIn(ExperimentalCoroutinesApi::class)
class BullsAndCowsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = FakeClock()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        store: RecordStore = FakeRecordStore(),
        seed: Int = 0,
    ) = BullsAndCowsViewModel(store, Random(seed), clock)

    /** Type a code into the keypad and play it. */
    private fun play(
        model: BullsAndCowsViewModel,
        digits: List<Int>,
    ) {
        for (digit in digits) model.enterDigit(digit)
        model.submitGuess()
    }

    private fun secretOf(model: BullsAndCowsViewModel) =
        model.uiState.value.game!!
            .secret

    @Test
    fun `opens on the start screen, at the level it was left on`() =
        runTest(dispatcher) {
            val store = FakeRecordStore(level = Level.HARD)
            store.writeBest(Level.HARD, Result(9, 60))
            val model = viewModel(store)

            advanceUntilIdle()

            assertEquals(Screen.START, model.uiState.value.screen)
            assertEquals(Level.HARD, model.uiState.value.level)
            assertEquals(Result(9, 60), model.uiState.value.best)
        }

    @Test
    fun `walks the difficulties and remembers the one it stopped on`() =
        runTest(dispatcher) {
            val store = FakeRecordStore(level = Level.EASY)
            store.writeBest(Level.CLASSIC, Result(4, 30))
            val model = viewModel(store)
            advanceUntilIdle()

            model.cycleLevel()
            advanceUntilIdle()

            assertEquals(Level.CLASSIC, model.uiState.value.level)
            assertEquals(Level.CLASSIC, store.level)
            assertEquals(Result(4, 30), model.uiState.value.best)
        }

    @Test
    fun `deals a code as long as the level says`() =
        runTest(dispatcher) {
            val store = FakeRecordStore(level = Level.HARD)
            val model = viewModel(store)
            advanceUntilIdle()

            model.startGame()

            assertEquals(Screen.PLAYING, model.uiState.value.screen)
            assertEquals(
                Level.HARD.length,
                model.uiState.value.game
                    ?.length,
            )
            assertTrue(
                model.uiState.value.entered
                    .isEmpty(),
            )
        }

    @Test
    fun `composes a guess a digit at a time and takes them back`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()

            model.enterDigit(1)
            model.enterDigit(2)
            assertEquals(listOf(1, 2), model.uiState.value.entered)

            model.eraseDigit()
            assertEquals(listOf(1), model.uiState.value.entered)
        }

    @Test
    fun `refuses a digit already in the guess, and one guess too many`() =
        runTest(dispatcher) {
            val store = FakeRecordStore(level = Level.EASY)
            val model = viewModel(store)
            advanceUntilIdle()
            model.startGame()

            model.enterDigit(1)
            model.enterDigit(1)
            assertEquals(listOf(1), model.uiState.value.entered)

            model.enterDigit(2)
            model.enterDigit(3)
            model.enterDigit(4)
            assertEquals(listOf(1, 2, 3), model.uiState.value.entered)
        }

    @Test
    fun `plays a guess, answers it, and clears the keypad`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            val secret = secretOf(model)

            play(model, listOf(secret[1], secret[0]) + secret.drop(2))

            val game = model.uiState.value.game!!
            assertEquals(1, game.history.size)
            assertTrue(
                model.uiState.value.entered
                    .isEmpty(),
            )
            assertEquals(Status.RUNNING, game.status)
        }

    @Test
    fun `refuses an incomplete guess and leaves it alone`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()

            model.enterDigit(1)
            model.submitGuess()

            assertEquals(listOf(1), model.uiState.value.entered)
            assertEquals(
                0,
                model.uiState.value.game
                    ?.history
                    ?.size,
            )
        }

    @Test
    fun `ends the game when the code is cracked, and records it`() =
        runTest(dispatcher) {
            val store = FakeRecordStore()
            val model = viewModel(store)
            advanceUntilIdle()
            model.startGame()

            clock.now += 45_000
            play(model, secretOf(model))
            advanceUntilIdle()

            val state = model.uiState.value
            assertEquals(Screen.SOLVED, state.screen)
            assertEquals(Status.WON, state.game?.status)
            assertEquals(45, state.seconds)
            assertTrue(state.isRecord)
            assertEquals(Result(1, 45), state.best)
            assertEquals(1, store.writes)
        }

    @Test
    fun `keeps the record when a later game took more guesses`() =
        runTest(dispatcher) {
            val store = FakeRecordStore()
            store.writeBest(Level.DEFAULT, Result(1, 1))
            val model = viewModel(store)
            advanceUntilIdle()
            model.startGame()

            play(model, listOf(9, 8, 7, 6))
            play(model, secretOf(model))
            advanceUntilIdle()

            assertFalse(model.uiState.value.isRecord)
            assertEquals(Result(1, 1), model.uiState.value.best)
            assertEquals(1, store.writes)
        }

    @Test
    fun `keeps a game that was put aside, and picks it up where it was`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            play(model, listOf(9, 8, 7, 6))
            model.enterDigit(5)

            model.showStart()
            assertEquals(Screen.START, model.uiState.value.screen)

            model.resumeGame()

            assertEquals(Screen.PLAYING, model.uiState.value.screen)
            assertEquals(
                1,
                model.uiState.value.game
                    ?.history
                    ?.size,
            )
            // Half-typed guess and all.
            assertEquals(listOf(5), model.uiState.value.entered)
        }

    @Test
    fun `has nothing to resume once the code is cracked`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            play(model, secretOf(model))
            advanceUntilIdle()
            model.showStart()

            model.resumeGame()

            assertEquals(Screen.START, model.uiState.value.screen)
        }

    @Test
    fun `follows the newest guess with the history window`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()

            repeat(HISTORY_ROWS + 2) { i -> play(model, listOf(9, 8, 7, 6).map { (it + i) % 10 }) }

            val played =
                model.uiState.value.game!!
                    .history.size
            assertEquals(played - HISTORY_ROWS, model.uiState.value.offset)
        }

    @Test
    fun `pages back through the history and wraps to the newest`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            repeat(HISTORY_ROWS + 2) { i -> play(model, listOf(9, 8, 7, 6).map { (it + i) % 10 }) }

            model.pageHistory()
            assertEquals(0, model.uiState.value.offset)

            model.pageHistory()
            assertEquals(HISTORY_ROWS - 1, model.uiState.value.offset)
        }

    @Test
    fun `scrolls the history a row at a time and stops at both ends`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            repeat(HISTORY_ROWS + 2) { i -> play(model, listOf(9, 8, 7, 6).map { (it + i) % 10 }) }
            val bottom = model.uiState.value.offset

            model.scrollHistory(-1)
            assertEquals(bottom - 1, model.uiState.value.offset)

            repeat(10) { model.scrollHistory(-1) }
            assertEquals(0, model.uiState.value.offset)

            repeat(10) { model.scrollHistory(1) }
            assertEquals(bottom, model.uiState.value.offset)
        }

    @Test
    fun `reads every level's record for the records screen`() =
        runTest(dispatcher) {
            val store = FakeRecordStore()
            store.writeBest(Level.EASY, Result(3, 20))
            val model = viewModel(store)
            advanceUntilIdle()

            model.showRecords()
            advanceUntilIdle()

            assertEquals(Screen.RECORDS, model.uiState.value.screen)
            assertEquals(Level.entries.toSet(), model.uiState.value.records.keys)
            assertEquals(Result(3, 20), model.uiState.value.records[Level.EASY])
            assertNull(model.uiState.value.records[Level.HARD])
        }

    @Test
    fun `refuses to play out of turn`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()

            model.enterDigit(1)
            model.eraseDigit()
            model.submitGuess()
            model.pageHistory()

            assertEquals(Screen.START, model.uiState.value.screen)
            assertNull(model.uiState.value.game)
            assertTrue(
                model.uiState.value.entered
                    .isEmpty(),
            )
        }

    @Test
    fun `leaves a game untimed when the watch's clock moved backwards`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()

            clock.now -= 5_000
            play(model, secretOf(model))
            advanceUntilIdle()

            assertNull(model.uiState.value.seconds)
            // A game with no clock is still a record on guesses alone.
            assertTrue(model.uiState.value.isRecord)
            assertNotNull(model.uiState.value.best)
        }
}
