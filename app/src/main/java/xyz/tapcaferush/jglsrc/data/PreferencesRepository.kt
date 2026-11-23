package xyz.tapcaferush.jglsrc.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tap_cafe_prefs")

class PreferencesRepository(private val context: Context) {
    private val dataStore = context.dataStore

    val isVibrationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[VIBRATION_ENABLED] ?: true
    }

    val allLevelsStats: Flow<List<LevelStatsSnapshot>> = dataStore.data.map { prefs ->
        (1..LEVEL_COUNT).map { id -> readLevel(prefs, id) }
    }

    suspend fun setVibrationEnabled(value: Boolean) {
        dataStore.edit { it[VIBRATION_ENABLED] = value }
    }

    suspend fun recordLevelSession(
        levelId: Int,
        sessionScore: Int,
        servedDelta: Int,
        peakCombo: Int,
        rushScore: Int,
        correctDelta: Int,
        wrongDelta: Int,
        expiredDelta: Int
    ) {
        if (levelId !in 1..LEVEL_COUNT) return
        dataStore.edit { prefs ->
            val shiftsKey = shiftsKey(levelId)
            prefs[shiftsKey] = (prefs[shiftsKey] ?: 0) + 1

            val best = bestScoreKey(levelId)
            val prevBest = prefs[best] ?: 0
            if (sessionScore > prevBest) prefs[best] = sessionScore

            val served = servedKey(levelId)
            prefs[served] = (prefs[served] ?: 0) + servedDelta

            val comboK = bestComboKey(levelId)
            val prevCombo = prefs[comboK] ?: 0
            if (peakCombo > prevCombo) prefs[comboK] = peakCombo

            val rushK = bestRushKey(levelId)
            val prevRush = prefs[rushK] ?: 0
            if (rushScore > prevRush) prefs[rushK] = rushScore

            val correctK = correctKey(levelId)
            prefs[correctK] = (prefs[correctK] ?: 0) + correctDelta

            val wrongK = wrongKey(levelId)
            prefs[wrongK] = (prefs[wrongK] ?: 0) + wrongDelta

            val expK = expiredKey(levelId)
            prefs[expK] = (prefs[expK] ?: 0) + expiredDelta
        }
    }

    suspend fun resetProgress() {
        dataStore.edit { prefs ->
            for (id in 1..LEVEL_COUNT) {
                prefs.remove(shiftsKey(id))
                prefs.remove(bestScoreKey(id))
                prefs.remove(servedKey(id))
                prefs.remove(bestComboKey(id))
                prefs.remove(bestRushKey(id))
                prefs.remove(correctKey(id))
                prefs.remove(wrongKey(id))
                prefs.remove(expiredKey(id))
            }
        }
    }

    private fun readLevel(prefs: Preferences, levelId: Int): LevelStatsSnapshot {
        return LevelStatsSnapshot(
            shiftsPlayed = prefs[shiftsKey(levelId)] ?: 0,
            bestScore = prefs[bestScoreKey(levelId)] ?: 0,
            totalServed = prefs[servedKey(levelId)] ?: 0,
            bestCombo = prefs[bestComboKey(levelId)] ?: 0,
            bestRushScore = prefs[bestRushKey(levelId)] ?: 0,
            correctTaps = prefs[correctKey(levelId)] ?: 0,
            wrongTaps = prefs[wrongKey(levelId)] ?: 0,
            expiredOrders = prefs[expiredKey(levelId)] ?: 0
        )
    }

    companion object {
        const val LEVEL_COUNT = 6

        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")

        private fun shiftsKey(id: Int) = intPreferencesKey("lvl_${id}_shifts")
        private fun bestScoreKey(id: Int) = intPreferencesKey("lvl_${id}_best_score")
        private fun servedKey(id: Int) = intPreferencesKey("lvl_${id}_served")
        private fun bestComboKey(id: Int) = intPreferencesKey("lvl_${id}_best_combo")
        private fun bestRushKey(id: Int) = intPreferencesKey("lvl_${id}_best_rush")
        private fun correctKey(id: Int) = intPreferencesKey("lvl_${id}_correct")
        private fun wrongKey(id: Int) = intPreferencesKey("lvl_${id}_wrong")
        private fun expiredKey(id: Int) = intPreferencesKey("lvl_${id}_expired")
    }
}
