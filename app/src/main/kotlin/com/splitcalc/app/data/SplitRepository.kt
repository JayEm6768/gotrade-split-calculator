package com.splitcalc.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.splitcalc.app.SavedCombo
import com.splitcalc.app.SavedRow
import com.splitcalc.app.SavedSplit
import com.splitcalc.app.plain
import com.splitcalc.core.FeeSettings
import com.splitcalc.core.parseDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.splitStore: DataStore<Preferences> by preferencesDataStore(
    name = "split",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Local-only persistence of the amount, ticker rows, fee settings and saved combos. */
class SplitRepository private constructor(private val store: DataStore<Preferences>) {

    val data: Flow<SavedSplit> = store.data.map { prefs ->
        SavedSplit(
            amount = prefs[AMOUNT].orEmpty(),
            rows = decodeRows(prefs[ROWS].orEmpty()),
            fees = decodeFees(prefs[FEES].orEmpty()),
            combos = decodeCombos(prefs[COMBOS].orEmpty()),
        )
    }

    suspend fun load(): SavedSplit = data.first()

    suspend fun save(split: SavedSplit) {
        store.edit { prefs ->
            prefs[AMOUNT] = split.amount
            prefs[ROWS] = encodeRows(split.rows)
            prefs[FEES] = encodeFees(split.fees)
            prefs[COMBOS] = encodeCombos(split.combos)
        }
    }

    /** Saves on a repository-owned scope so the write outlives the calling screen, then runs [afterSave]. */
    fun saveInBackground(split: SavedSplit, afterSave: suspend () -> Unit = {}) {
        scope.launch {
            save(split)
            afterSave()
        }
    }

    companion object {
        private val AMOUNT = stringPreferencesKey("amount")
        private val ROWS = stringPreferencesKey("rows")
        private val FEES = stringPreferencesKey("fees")
        private val COMBOS = stringPreferencesKey("combos")
        private const val COMBO_SEPARATOR = "|"
        private const val SEPARATOR = ";"
        private const val FIELD_SEPARATOR = ":"

        @OptIn(ExperimentalCoroutinesApi::class)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

        @Volatile
        private var instance: SplitRepository? = null

        fun get(context: Context): SplitRepository =
            instance ?: synchronized(this) {
                instance ?: SplitRepository(context.applicationContext.splitStore).also { instance = it }
            }

        internal fun encodeRows(rows: List<SavedRow>): String =
            rows.joinToString(SEPARATOR) { "${it.ticker}$FIELD_SEPARATOR${it.percent}" }

        internal fun decodeRows(text: String): List<SavedRow> =
            text.split(SEPARATOR).filter { it.isNotEmpty() }.map { entry ->
                val parts = entry.split(FIELD_SEPARATOR, limit = 2)
                SavedRow(parts[0], parts.getOrElse(1) { "" })
            }

        internal fun encodeCombos(combos: List<SavedCombo>): String =
            combos.joinToString(COMBO_SEPARATOR) { "${it.name}=${encodeRows(it.rows)}" }

        internal fun decodeCombos(text: String): List<SavedCombo> =
            text.split(COMBO_SEPARATOR).mapNotNull { entry ->
                val parts = entry.split("=", limit = 2)
                if (parts.size < 2 || parts[0].isBlank()) return@mapNotNull null
                val rows = decodeRows(parts[1])
                if (rows.isEmpty()) null else SavedCombo(parts[0], rows)
            }

        internal fun encodeFees(fees: FeeSettings): String =
            listOf(fees.tradingRatePercent, fees.minTradingFee, fees.regulatoryFee).joinToString(SEPARATOR) { it.plain() }

        internal fun decodeFees(text: String): FeeSettings {
            val values = text.split(SEPARATOR).map { parseDecimal(it) }
            if (values.size != 3 || values.any { it == null }) return FeeSettings()
            return FeeSettings(values[0]!!, values[1]!!, values[2]!!)
        }
    }
}
