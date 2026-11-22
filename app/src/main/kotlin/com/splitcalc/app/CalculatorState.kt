package com.splitcalc.app

import com.splitcalc.core.Allocation
import com.splitcalc.core.CalcResult
import com.splitcalc.core.FeeSettings
import com.splitcalc.core.calculate
import com.splitcalc.core.parseDecimal
import com.splitcalc.core.validate
import java.math.BigDecimal

object Limits {
    val MAX_AMOUNT = BigDecimal("10000000")
    val HUNDRED = BigDecimal(100)
    val MAX_FEE_VALUE = BigDecimal(100)
    const val MAX_AMOUNT_CHARS = 11
    const val MAX_DECIMALS = 2
    const val MAX_TICKER_CHARS = 10
    const val MAX_ROWS = 20
    const val MAX_FEE_CHARS = 6
    const val MAX_COMBOS = 12
    const val MAX_COMBO_NAME_CHARS = 20
}

val PRESET_PERCENTS: List<Int> = listOf(5, 10, 15, 20, 25, 30, 40, 50, 100)

data class TickerRow(val id: Long, val ticker: String = "", val percent: BigDecimal? = null)

/** Either a reason the results are hidden, or the calculated results. */
sealed interface Outcome {
    data class Blocked(val message: String) : Outcome
    data class Ready(val result: CalcResult.Success) : Outcome
}

data class SavedRow(val ticker: String, val percent: String)

data class SavedCombo(val name: String, val rows: List<SavedRow>)

data class SavedSplit(
    val amount: String = "",
    val rows: List<SavedRow> = emptyList(),
    val fees: FeeSettings = FeeSettings(),
    val combos: List<SavedCombo> = emptyList(),
)

data class CalculatorState(
    val loaded: Boolean = false,
    val amountText: String = "",
    val rows: List<TickerRow> = listOf(TickerRow(id = 1)),
    val selectedId: Long? = 1,
    val fees: FeeSettings = FeeSettings(),
    val notice: String? = null,
    val nextId: Long = 2,
    val combos: List<SavedCombo> = emptyList(),
    val focusRequestId: Long? = null,
) {
    val allocated: BigDecimal
        get() = rows.fold(BigDecimal.ZERO) { sum, row -> sum + (row.percent ?: BigDecimal.ZERO) }

    val amount: BigDecimal?
        get() = parseTyped(amountText)?.takeIf { it.signum() > 0 }

    val selectedRow: TickerRow? get() = rows.firstOrNull { it.id == selectedId }

    /** Percent that would remain for the selected row if it were set to fill the gap to 100. */
    val restForSelected: BigDecimal
        get() = selectedRow?.let { Limits.HUNDRED - othersTotal(it.id) } ?: BigDecimal.ZERO

    val canFillRest: Boolean get() = restForSelected.signum() > 0

    val canSaveCombo: Boolean
        get() = isValidCombo(rows.map { SavedRow(it.ticker, it.percent?.plain().orEmpty()) }, fees)

    val defaultComboName: String
        get() = sanitizeComboName(rows.map { it.ticker }.filter { it.isNotEmpty() }.joinToString("/"))

    val outcome: Outcome
        get() {
            if (allocated.compareTo(Limits.HUNDRED) != 0) return Outcome.Blocked("Percentages must total 100%")
            val budget = amount ?: return Outcome.Blocked("Enter an amount")
            val allocations = rows.map { Allocation(it.ticker, it.percent ?: BigDecimal.ZERO) }
            return when (val result = calculate(allocations, budget, fees)) {
                is CalcResult.Invalid -> Outcome.Blocked(result.errors.first().message)
                is CalcResult.Success -> Outcome.Ready(result)
            }
        }

    fun withAmount(raw: String): CalculatorState {
        val accepted = acceptAmountInput(raw) ?: return this
        return copy(amountText = accepted, notice = null)
    }

    fun withTicker(id: Long, raw: String): CalculatorState =
        copy(rows = rows.map { if (it.id == id) it.copy(ticker = sanitizeTicker(raw)) else it }, notice = null)

    fun select(id: Long): CalculatorState =
        if (rows.any { it.id == id }) copy(selectedId = id, notice = null) else this

    fun addRow(): CalculatorState {
        if (rows.size >= Limits.MAX_ROWS) return copy(notice = "At most ${Limits.MAX_ROWS} tickers")
        return copy(
            rows = rows + TickerRow(nextId),
            selectedId = nextId,
            nextId = nextId + 1,
            notice = null,
            focusRequestId = nextId,
        )
    }

    fun consumeFocusRequest(): CalculatorState = if (focusRequestId == null) this else copy(focusRequestId = null)

    fun removeRow(id: Long): CalculatorState {
        val index = rows.indexOfFirst { it.id == id }
        if (index < 0) return this
        val remaining = rows.filterNot { it.id == id }
        val selection = if (selectedId == id) remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.id else selectedId
        return copy(rows = remaining, selectedId = selection, notice = null)
    }

    /** Sets the selected row to [percent]; zero clears it. Refuses values that would push the total over 100. */
    fun applyPercent(percent: BigDecimal): CalculatorState {
        val row = selectedRow ?: return copy(notice = "Select a ticker first")
        if (percent.signum() < 0 || percent > Limits.HUNDRED) return copy(notice = "Enter a percent from 0 to 100")
        if (othersTotal(row.id) + percent > Limits.HUNDRED) return copy(notice = "Total can't exceed 100%")
        val value = percent.takeIf { it.signum() > 0 }
        return copy(rows = rows.map { if (it.id == row.id) it.copy(percent = value) else it }, notice = null)
    }

    fun applyRest(): CalculatorState =
        if (canFillRest) applyPercent(restForSelected) else copy(notice = "Nothing left to allocate")

    fun applyCustom(text: String): CalculatorState {
        val value = parseTyped(text) ?: return copy(notice = "Enter a percent from 0 to 100")
        if (value.stripTrailingZeros().scale() > Limits.MAX_DECIMALS) return copy(notice = "Use at most 2 decimals")
        return applyPercent(value)
    }

    fun saveCombo(name: String): CalculatorState {
        if (!canSaveCombo) return copy(notice = "Combo needs tickers totaling 100%")
        val finalName = sanitizeComboName(name).ifEmpty { defaultComboName }
        val saved = SavedCombo(finalName, rows.map { SavedRow(it.ticker, it.percent?.plain().orEmpty()) })
        val replaced = combos.any { it.name == finalName }
        if (!replaced && combos.size >= Limits.MAX_COMBOS) return copy(notice = "At most ${Limits.MAX_COMBOS} saved combos")
        val updated = if (replaced) combos.map { if (it.name == finalName) saved else it } else combos + saved
        return copy(combos = updated, notice = null)
    }

    fun applyCombo(name: String): CalculatorState {
        val combo = combos.firstOrNull { it.name == name } ?: return this
        val newRows = combo.rows.mapIndexed { index, row ->
            TickerRow(id = nextId + index, ticker = row.ticker, percent = parseDecimal(row.percent))
        }
        return copy(
            rows = newRows,
            selectedId = newRows.first().id,
            nextId = nextId + newRows.size,
            notice = null,
        )
    }

    fun deleteCombo(name: String): CalculatorState = copy(combos = combos.filterNot { it.name == name })

    fun withFees(settings: FeeSettings): CalculatorState = copy(fees = settings, notice = null)

    fun dismissNotice(): CalculatorState = if (notice == null) this else copy(notice = null)

    fun toSaved(): SavedSplit = SavedSplit(
        amount = amountText,
        rows = rows.map { SavedRow(it.ticker, it.percent?.plain().orEmpty()) },
        fees = fees,
        combos = combos,
    )

    private fun othersTotal(id: Long): BigDecimal =
        rows.filter { it.id != id }.fold(BigDecimal.ZERO) { sum, row -> sum + (row.percent ?: BigDecimal.ZERO) }

    companion object {
        fun from(saved: SavedSplit): CalculatorState {
            val rows = saved.rows.take(Limits.MAX_ROWS).mapIndexed { index, row ->
                val percent = parseDecimal(row.percent)?.takeIf { it.signum() > 0 && it <= Limits.HUNDRED }
                TickerRow(id = index + 1L, ticker = sanitizeTicker(row.ticker), percent = percent)
            }.ifEmpty { listOf(TickerRow(id = 1)) }
            val fees = saved.fees.takeIf { it.isAllowed() } ?: FeeSettings()
            return CalculatorState(
                loaded = true,
                amountText = acceptAmountInput(saved.amount).orEmpty(),
                rows = rows,
                selectedId = (rows.firstOrNull { it.percent == null } ?: rows.first()).id,
                fees = fees,
                nextId = rows.size + 1L,
                combos = cleanCombos(saved.combos, fees),
            )
        }
    }
}

private fun isValidCombo(rows: List<SavedRow>, fees: FeeSettings): Boolean {
    if (rows.isEmpty() || rows.size > Limits.MAX_ROWS) return false
    val percents = rows.map { parseDecimal(it.percent) ?: return false }
    if (percents.fold(BigDecimal.ZERO, BigDecimal::add).compareTo(Limits.HUNDRED) != 0) return false
    val allocations = rows.zip(percents) { row, percent -> Allocation(row.ticker, percent) }
    return validate(allocations, BigDecimal.ONE, fees).isEmpty()
}

private fun cleanCombos(stored: List<SavedCombo>, fees: FeeSettings): List<SavedCombo> =
    stored.asSequence()
        .map { combo ->
            SavedCombo(
                name = sanitizeComboName(combo.name),
                rows = combo.rows.map { SavedRow(sanitizeTicker(it.ticker), it.percent) },
            )
        }
        .filter { it.name.isNotEmpty() && isValidCombo(it.rows, fees) }
        .distinctBy { it.name }
        .take(Limits.MAX_COMBOS)
        .toList()

/** Keeps ASCII letters, digits, space, dash, underscore and slash so names never clash with storage separators. */
fun sanitizeComboName(raw: String): String =
    raw.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == ' ' || it == '-' || it == '_' || it == '/' }
        .trim()
        .take(Limits.MAX_COMBO_NAME_CHARS)
        .trim()

fun BigDecimal.plain(): String = stripTrailingZeros().toPlainString()

/** Parses a number that may still end in a dot while the user is typing. */
private fun parseTyped(text: String): BigDecimal? = parseDecimal(text.trim().trimEnd('.'))

/**
 * Filters pasted or typed amount text. Returns the text to keep, or null when the input must be
 * rejected (more than two decimals, above the cap, or too long).
 */
fun acceptAmountInput(raw: String): String? {
    val text = raw.filter { it in '0'..'9' || it == '.' }
    if (text.length > Limits.MAX_AMOUNT_CHARS || text.count { it == '.' } > 1) return null
    val dot = text.indexOf('.')
    if (dot >= 0 && text.length - dot - 1 > Limits.MAX_DECIMALS) return null
    val number = parseTyped(text)
    if (number != null && number > Limits.MAX_AMOUNT) return null
    return text
}

fun sanitizeTicker(raw: String): String =
    raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' || it == '.' || it == '-' }.take(Limits.MAX_TICKER_CHARS)

/** Keeps digits and a single dot for fee and percent fields; null rejects the edit. */
fun sanitizeDecimalInput(raw: String, maxChars: Int = Limits.MAX_FEE_CHARS): String? {
    val text = raw.filter { it in '0'..'9' || it == '.' }
    return text.takeIf { it.length <= maxChars && it.count { c -> c == '.' } <= 1 }
}

/** Returns the fee settings for the three form fields, or null if any field is invalid. */
fun parseFees(rate: String, minimum: String, regulatory: String): FeeSettings? {
    val r = parseTyped(rate) ?: return null
    val m = parseTyped(minimum) ?: return null
    val g = parseTyped(regulatory) ?: return null
    return FeeSettings(r, m, g).takeIf { it.isAllowed() }
}

fun FeeSettings.isAllowed(): Boolean =
    minTradingFee <= Limits.MAX_FEE_VALUE && regulatoryFee <= Limits.MAX_FEE_VALUE &&
        validate(listOf(Allocation("X", Limits.HUNDRED)), BigDecimal.ONE, this).isEmpty()
