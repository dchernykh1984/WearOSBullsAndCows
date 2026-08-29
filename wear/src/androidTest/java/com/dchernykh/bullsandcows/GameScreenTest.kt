package com.dchernykh.bullsandcows

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dchernykh.bullsandcows.game.Level
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

/**
 * What no JVM test can check: that the game actually runs on a watch.
 *
 * Launching the activity exercises the manifest, the theme, the launcher icon, the
 * ring of ten keys, the whole Compose tree and the DataStore-backed record store in
 * one go - the parts excused from the coverage floor precisely because they need a
 * device. The rules are covered far more cheaply by the unit tests, so this walks
 * the screens and plays a guess rather than trying to crack anything.
 *
 * Every label is read from the resources, so the test says the same thing on a
 * watch set to any of the eleven languages.
 */
class GameScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = rule.activity.getString(id)

    private fun digit(d: Int) = rule.activity.getString(R.string.digit_key, d)

    private fun onScreen(label: String) = rule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()

    private val levelLabels get() = Level.entries.map { text(it.labelRes) }

    @Test
    fun opensOnTheStartScreen() {
        rule.onNodeWithText(text(R.string.app_name)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.play)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.records)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.hint)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.legend)).assertIsDisplayed()
    }

    @Test
    fun walksTheDifficulties() {
        rule.waitUntil { levelLabels.any(::onScreen) }
        val before = levelLabels.first(::onScreen)

        rule.onNodeWithText(before).performClick()
        rule.waitUntil { !onScreen(before) }

        assertNotEquals(before, levelLabels.first(::onScreen))
    }

    @Test
    fun startsAGameAndShowsTheRingOfKeys() {
        rule.onNodeWithText(text(R.string.play)).performClick()
        rule.waitForIdle()

        for (d in 0..9) rule.onNodeWithContentDescription(digit(d)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.erase)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.check)).assertIsDisplayed()
    }

    @Test
    fun playsAGuessAndShowsItInTheHistory() {
        rule.onNodeWithText(text(R.string.play)).performClick()
        rule.waitForIdle()

        // The default level is four digits; any four distinct ones are a legal guess.
        for (d in listOf(1, 2, 3, 4)) rule.onNodeWithContentDescription(digit(d)).performClick()
        rule.onNodeWithText(text(R.string.check)).performClick()
        rule.waitForIdle()

        // Either the guess is in the history, or it happened to be the code.
        rule.waitUntil { onScreen("1234") || onScreen(text(R.string.solved)) }
    }

    @Test
    fun takesADigitBackBeforePlayingIt() {
        rule.onNodeWithText(text(R.string.play)).performClick()
        rule.waitForIdle()

        rule.onNodeWithContentDescription(digit(7)).performClick()
        rule.onNodeWithText(text(R.string.erase)).performClick()
        rule.waitForIdle()

        // With nothing typed, erase goes back to doing nothing.
        rule.onNodeWithText(text(R.string.check)).assertIsDisplayed()
    }

    @Test
    fun readsTheRecordsAndComesBack() {
        rule.onNodeWithText(text(R.string.records)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText(text(R.string.back)).assertIsDisplayed()

        rule.onNodeWithText(text(R.string.back)).performClick()
        rule.waitUntil { onScreen(text(R.string.play)) }
    }
}
