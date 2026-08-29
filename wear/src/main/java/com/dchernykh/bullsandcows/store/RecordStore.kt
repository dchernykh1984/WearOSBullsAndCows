package com.dchernykh.bullsandcows.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dchernykh.bullsandcows.game.Level
import com.dchernykh.bullsandcows.game.Result
import com.dchernykh.bullsandcows.game.normalizeResult
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * What survives closing the app: the difficulty last played, and one best result
 * per difficulty. Cracking a five-digit code in seven guesses says something quite
 * different from doing it on three, so the two are never pooled.
 *
 * The Zepp OS original packed a result into one string, because its storage only
 * held strings. DataStore holds typed values, so the guesses and the seconds are
 * two keys and there is nothing left to encode or half-decode.
 *
 * An interface, because everything interesting happens above it: a JVM test plays
 * whole games against an in-memory implementation instead of an emulator.
 */
interface RecordStore {
    suspend fun readLevel(): Level

    suspend fun writeLevel(level: Level)

    suspend fun readBest(level: Level): Result?

    suspend fun writeBest(
        level: Level,
        best: Result,
    )
}

private val Context.recordDataStore: DataStore<Preferences> by preferencesDataStore(name = "records")

private val LEVEL_KEY = stringPreferencesKey("level")

private fun attemptsKey(level: Level) = intPreferencesKey("best_${level.name}")

private fun secondsKey(level: Level) = intPreferencesKey("seconds_${level.name}")

/**
 * The real store, on top of Preferences DataStore.
 *
 * Storage that has gone wrong must not stop anyone playing: a failed read reads as
 * nothing stored and a failed write is dropped, so a corrupt preferences file costs
 * a record rather than the app.
 */
class DataStoreRecordStore(
    context: Context,
) : RecordStore {
    // The application context, not the activity's: a DataStore outlives any one
    // screen, and holding the activity here would leak it for the life of the app.
    private val dataStore = context.applicationContext.recordDataStore

    private suspend fun read(): Preferences =
        dataStore.data
            .catch { cause ->
                // Only I/O. Anything else is a bug in this file rather than a
                // broken disk, and swallowing it would hide it.
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }.first()

    private suspend fun write(change: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit(change)
        } catch (_: IOException) {
            // Nothing to do and nothing worth saying: the game carries on.
        }
    }

    override suspend fun readLevel(): Level = Level.fromStoredName(read()[LEVEL_KEY])

    override suspend fun writeLevel(level: Level) = write { it[LEVEL_KEY] = level.name }

    override suspend fun readBest(level: Level): Result? {
        val stored = read()
        return normalizeResult(stored[attemptsKey(level)], stored[secondsKey(level)])
    }

    override suspend fun writeBest(
        level: Level,
        best: Result,
    ) = write {
        it[attemptsKey(level)] = best.attempts
        // A record with no time keeps none: a stored zero would read as a game
        // that took no time at all, and that beats every real record.
        val seconds = best.seconds
        if (seconds == null) it.remove(secondsKey(level)) else it[secondsKey(level)] = seconds
    }
}
