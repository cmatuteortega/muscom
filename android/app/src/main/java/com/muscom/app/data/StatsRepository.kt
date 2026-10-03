package com.muscom.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Estadísticas locales del prototipo (en fase 2 pasan al servidor). */
data class Stats(
    val played: Int = 0,
    val won: Int = 0,
    val hands: Int = 0,
    val ordagosWon: Int = 0,
    val ordagosLost: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val pointsFor: Int = 0,
    val pointsAgainst: Int = 0,
) {
    val winRate: Int get() = if (played == 0) 0 else (100 * won / played)
}

internal val Context.statsStore: DataStore<Preferences> by preferencesDataStore(name = "stats")

@Singleton
class StatsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val keys = listOf("played", "won", "hands", "ordagosWon", "ordagosLost", "streak", "bestStreak", "pointsFor", "pointsAgainst")
        .associateWith { intPreferencesKey(it) }

    val stats: Flow<Stats> = context.statsStore.data.map { p ->
        fun g(k: String) = p[keys.getValue(k)] ?: 0
        Stats(g("played"), g("won"), g("hands"), g("ordagosWon"), g("ordagosLost"), g("streak"), g("bestStreak"), g("pointsFor"), g("pointsAgainst"))
    }

    suspend fun recordGame(won: Boolean, hands: Int, ordago: Boolean, pointsFor: Int, pointsAgainst: Int) {
        context.statsStore.edit { p ->
            fun inc(k: String, by: Int = 1) { p[keys.getValue(k)] = (p[keys.getValue(k)] ?: 0) + by }
            inc("played")
            if (won) inc("won")
            inc("hands", hands)
            if (ordago) inc(if (won) "ordagosWon" else "ordagosLost")
            inc("pointsFor", pointsFor)
            inc("pointsAgainst", pointsAgainst)
            val streak = if (won) (p[keys.getValue("streak")] ?: 0) + 1 else 0
            p[keys.getValue("streak")] = streak
            p[keys.getValue("bestStreak")] = maxOf(streak, p[keys.getValue("bestStreak")] ?: 0)
        }
    }

    suspend fun clear() {
        context.statsStore.edit { it.clear() }
    }
}
