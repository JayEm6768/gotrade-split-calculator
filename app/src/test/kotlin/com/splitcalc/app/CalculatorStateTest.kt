package com.splitcalc.app

import com.splitcalc.core.FeeSettings
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalculatorStateTest {

    private fun state(vararg rows: Pair<String, Int?>, amount: String = "20") = CalculatorState(
        loaded = true,
        amountText = amount,
        rows = rows.mapIndexed { i, (ticker, pct) -> TickerRow(i + 1L, ticker, pct?.let { BigDecimal(it) }) },
        selectedId = 1,
        nextId = rows.size + 1L,
    )

    private fun CalculatorState.percents() = rows.map { it.percent?.toInt() }

    @Test
    fun `valid split matches the reference numbers`() {
        val s = state("VOO" to 45, "SMH" to 25, "SCHD" to 15, "IBIT" to 15)
        val outcome = assertIs<Outcome.Ready>(s.outcome)
        assertEquals(
            listOf("8.89", "4.89", "2.89", "2.89"),
            outcome.result.rows.map { it.orderAmount.toPlainString() },
        )
        assertEquals("0.44", outcome.result.totals.totalFees.toPlainString())
    }

    @Test
    fun `results are blocked until the total is exactly 100`() {
        val outcome = assertIs<Outcome.Blocked>(state("VOO" to 45, "SMH" to 50).outcome)
        assertEquals("Percentages must total 100%", outcome.message)
    }

    @Test
    fun `missing amount blocks results even at 100 percent`() {
        val outcome = assertIs<Outcome.Blocked>(state("VOO" to 100, amount = "").outcome)
        assertEquals("Enter an amount", outcome.message)
    }

    @Test
    fun `rest fills the selected row up to 100`() {
        val s = state("VOO" to 45, "SMH" to null).select(2).applyRest()
        assertEquals(listOf(45, 55), s.percents())
    }

    @Test
    fun `rest is unavailable when nothing is left`() {
        val s = state("VOO" to 100, "SMH" to null).select(2)
        assertTrue(!s.canFillRest)
        val after = s.applyRest()
        assertNull(after.rows[1].percent)
        assertNotNull(after.notice)
    }

    @Test
    fun `a preset that would exceed 100 is refused`() {
        val s = state("VOO" to 80, "SMH" to null).select(2).applyPercent(BigDecimal(30))
        assertNull(s.rows[1].percent)
        assertEquals("Total can't exceed 100%", s.notice)
    }

    @Test
    fun `changing a row can reuse its own share`() {
        val s = state("VOO" to 80, "SMH" to 20).applyPercent(BigDecimal(80))
        assertEquals(listOf(80, 20), s.percents())
        assertNull(s.notice)
    }

    @Test
    fun `custom percent accepts decimals but not garbage`() {
        val ok = state("VOO" to null).applyCustom("12.5")
        assertEquals(BigDecimal("12.5"), ok.rows[0].percent)
        assertNotNull(state("VOO" to null).applyCustom("abc").notice)
        assertNotNull(state("VOO" to null).applyCustom("12.345").notice)
        assertNotNull(state("VOO" to null).applyCustom("101").notice)
    }

    @Test
    fun `adding a row selects it and removing keeps a sensible selection`() {
        val added = state("VOO" to 50).addRow()
        assertEquals(2, added.rows.size)
        assertEquals(added.rows.last().id, added.selectedId)

        val removed = added.removeRow(added.rows.last().id)
        assertEquals(1, removed.rows.size)
        assertEquals(removed.rows.first().id, removed.selectedId)
    }

    @Test
    fun `row count is capped`() {
        var s = CalculatorState(loaded = true)
        repeat(Limits.MAX_ROWS + 5) { s = s.addRow() }
        assertEquals(Limits.MAX_ROWS, s.rows.size)
    }

    @Test
    fun `amount input is capped and limited to two decimals`() {
        val s = CalculatorState(loaded = true)
        assertEquals("19.99", s.withAmount("19.99").amountText)
        assertEquals("19.99", s.withAmount("19.99").withAmount("19.999").amountText)
        assertEquals("10000000", s.withAmount("10000000").amountText)
        assertEquals("10000000", s.withAmount("10000000").withAmount("10000001").amountText)
        assertEquals("1000", s.withAmount("$1,000").amountText)
        assertEquals("", s.withAmount("abc").amountText)
    }

    @Test
    fun `ticker input is uppercased, filtered and limited`() {
        val s = CalculatorState(loaded = true).withTicker(1, "voo ")
        assertEquals("VOO", s.rows[0].ticker)
        assertEquals("BRK.B", s.withTicker(1, "brk.b").rows[0].ticker)
        assertEquals(Limits.MAX_TICKER_CHARS, s.withTicker(1, "abcdefghijklmnop").rows[0].ticker.length)
        assertEquals("AB1", s.withTicker(1, "a!b@1#").rows[0].ticker)
    }

    @Test
    fun `saved state round trips`() {
        val s = state("VOO" to 45, "SMH" to 55, amount = "20.5")
        val restored = CalculatorState.from(s.toSaved())
        assertEquals(s.amountText, restored.amountText)
        assertEquals(s.rows.map { it.ticker to it.percent }, restored.rows.map { it.ticker to it.percent })
        assertTrue(restored.loaded)
    }

    @Test
    fun `corrupt saved data falls back to a safe state`() {
        val saved = SavedSplit(amount = "NaN", rows = listOf(SavedRow("<script>", "abc"), SavedRow("VOO", "500")))
        val restored = CalculatorState.from(saved)
        assertEquals("", restored.amountText)
        assertEquals("SCRIPT", restored.rows[0].ticker)
        assertNull(restored.rows[0].percent)
        assertNull(restored.rows[1].percent)
    }

    @Test
    fun `out of range saved fees are reset to defaults`() {
        val saved = SavedSplit(fees = FeeSettings(BigDecimal("250"), BigDecimal("1E+9"), BigDecimal("0.01")))
        assertEquals(FeeSettings(), CalculatorState.from(saved).fees)
    }

    @Test
    fun `fee form parsing validates input`() {
        assertNotNull(parseFees("0.3", "0.10", "0.01"))
        assertNull(parseFees("", "0.10", "0.01"))
        assertNull(parseFees("100", "0.10", "0.01"))
        assertNull(parseFees("0.3", "1000", "0.01"))
    }

    @Test
    fun `view model persists only real changes`() {
        val saved = mutableListOf<SavedSplit>()
        val vm = CalculatorViewModel(repository = null, persist = { saved += it }, initial = CalculatorState(loaded = true))
        vm.onAmountChange("20")
        vm.onPreset(100)
        vm.onPreset(5)
        vm.onSelect(1)
        assertEquals(3, saved.size)
        assertEquals("20", saved.last().amount)
        assertEquals("5", saved.last().rows.single().percent)
    }

    private fun comboState() = state("VOO" to 45, "SMH" to 25, "SCHD" to 15, "IBIT" to 15, amount = "20")

    @Test
    fun `applying a combo fills rows and keeps the amount`() {
        val saved = comboState().saveCombo("Core")
        val cleared = saved.copy(rows = listOf(TickerRow(9)), selectedId = 9, nextId = 10, amountText = "55")
        val applied = cleared.applyCombo("Core")
        assertEquals(listOf("VOO", "SMH", "SCHD", "IBIT"), applied.rows.map { it.ticker })
        assertEquals(listOf(45, 25, 15, 15), applied.percents())
        assertEquals("55", applied.amountText)
        assertEquals(applied.rows.first().id, applied.selectedId)
        assertEquals(applied.rows.size, applied.rows.map { it.id }.toSet().size)
        val added = applied.addRow()
        assertEquals(5, added.rows.size)
        assertEquals(added.rows.last().id, added.selectedId)
        assertEquals(added.rows.size, added.rows.map { it.id }.toSet().size)
    }

    @Test
    fun `combo save is refused for bad totals or blank tickers`() {
        val badTotal = state("VOO" to 45, "SMH" to 50).saveCombo("X")
        assertTrue(badTotal.combos.isEmpty())
        assertEquals("Combo needs tickers totaling 100%", badTotal.notice)
        assertTrue(state("VOO" to 50, "" to 50).saveCombo("X").combos.isEmpty())
    }

    @Test
    fun `saving a combo with the same name replaces it`() {
        val first = state("VOO" to 100).saveCombo("Mine")
        val second = first.copy(rows = state("QQQ" to 100).rows).saveCombo("Mine")
        assertEquals(1, second.combos.size)
        assertEquals("QQQ", second.combos.single().rows.single().ticker)
    }

    @Test
    fun `combo count is capped`() {
        var s = state("VOO" to 100)
        repeat(Limits.MAX_COMBOS + 3) { s = s.saveCombo("c$it") }
        assertEquals(Limits.MAX_COMBOS, s.combos.size)
        assertEquals("At most 12 saved combos", s.notice)
    }

    @Test
    fun `blank combo name defaults to the tickers`() {
        val s = state("VOO" to 60, "SMH" to 40).saveCombo("  ")
        assertEquals("VOO/SMH", s.combos.single().name)
    }

    @Test
    fun `combo names are sanitized`() {
        assertEquals("ab c-d_e", sanitizeComboName("a|b= c;-d:_e"))
        assertEquals(Limits.MAX_COMBO_NAME_CHARS, sanitizeComboName("x".repeat(50)).length)
    }

    @Test
    fun `deleting a combo removes it`() {
        val s = state("VOO" to 100).saveCombo("A").deleteCombo("A")
        assertTrue(s.combos.isEmpty())
    }

    @Test
    fun `malformed stored combos are dropped when loading`() {
        val good = SavedCombo("Ok", listOf(SavedRow("VOO", "100")))
        val loaded = CalculatorState.from(
            SavedSplit(
                combos = listOf(
                    good,
                    SavedCombo("NoRows", emptyList()),
                    SavedCombo("Short", listOf(SavedRow("VOO", "50"))),
                    SavedCombo("BadTicker", listOf(SavedRow("", "100"))),
                    SavedCombo("|=", listOf(SavedRow("VOO", "100"))),
                ),
            ),
        )
        assertEquals(listOf("Ok"), loaded.combos.map { it.name })
    }

    @Test
    fun `adding a row requests focus until consumed`() {
        val added = state("VOO" to 50).addRow()
        assertEquals(added.selectedId, added.focusRequestId)
        assertNull(added.consumeFocusRequest().focusRequestId)
        assertNull(added.toSaved().let { CalculatorState.from(it).focusRequestId })
    }
}
