package com.splitcalc.core

import java.math.BigDecimal
import kotlin.test.*

class CalculatorTest {
    private fun d(s: String) = BigDecimal(s)
    private fun alloc(vararg p: Pair<String, String>) = p.map { Allocation(it.first, d(it.second)) }
    private fun ok(r: CalcResult) = assertIs<CalcResult.Success>(r)
    private fun invalid(r: CalcResult) = assertIs<CalcResult.Invalid>(r)

    @Test fun singleTickerRealTrade() {
        val r = ok(calculate(alloc("VOO" to "100"), d("23.02")))
        val row = r.rows.single()
        assertEquals(d("22.91"), row.orderAmount)
        assertEquals(d("0.10"), row.tradingFee)
        assertEquals(d("0.01"), row.regulatoryFee)
        assertEquals(d("0.00"), row.leftover)
    }

    @Test fun fourTickerSplit() {
        val r = ok(calculate(alloc("VOO" to "45", "SMH" to "25", "SCHD" to "15", "IBIT" to "15"), d("20")))
        assertEquals(listOf("9.00", "5.00", "3.00", "3.00").map(::d), r.rows.map { it.slice })
        assertEquals(listOf("8.89", "4.89", "2.89", "2.89").map(::d), r.rows.map { it.orderAmount })
        assertEquals(d("0.44"), r.totals.totalFees)
        assertEquals(d("2.20"), r.totals.feePercentOfBudget)
        assertEquals(r.totals.budget, r.totals.totalInvested + r.totals.totalFees + r.totals.totalLeftover)
        assertTrue(r.warnings.any { it.contains("2.20%") })
    }

    @Test fun ninetyFivePercentRejected() {
        val r = invalid(calculate(alloc("VOO" to "40", "SMH" to "25", "SCHD" to "15", "IBIT" to "15"), d("20")))
        assertTrue(r.errors.any { it.message == "Percentages total 95.00%, must be exactly 100%" })
    }

    @Test fun validationErrors() {
        invalid(calculate(alloc("VOO" to "60", "SMH" to "41"), d("20")))
        invalid(calculate(alloc("VOO" to "50", "voo" to "50"), d("20")))
        invalid(calculate(alloc("VOO" to "100", "SMH" to "0"), d("20")))
        invalid(calculate(alloc("VOO" to "150", "SMH" to "-50"), d("20")))
        invalid(calculate(alloc("VOO" to "100"), d("0")))
        invalid(calculate(alloc("V O O" to "100"), d("20")))
        invalid(calculate(emptyList(), d("20")))
        invalid(calculate(alloc("VOO" to "100"), d("20"), FeeSettings(tradingRatePercent = d("100"))))
    }

    @Test fun parserRejectsGarbage() {
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal("1e5"))
        assertNull(parseDecimal("-5"))
        assertNull(parseDecimal(""))
        assertEquals(d("1000.50"), parseDecimal("$1,000.50"))
        assertEquals(d("12.5"), parseDecimal("12.5%"))
    }

    @Test fun largeBudgetPercentBranch() {
        val row = ok(calculate(alloc("VOO" to "100"), d("1000"))).rows.single()
        assertEquals(d("996.99"), row.orderAmount)
        assertEquals(d("3.00"), row.tradingFee)
        assertTrue(row.orderAmount + row.totalFee <= row.slice)
        assertTrue(row.leftover.signum() >= 0 && row.leftover < d("0.02"))
    }

    @Test fun tinySliceTooSmall() {
        val r = ok(calculate(alloc("VOO" to "99", "SMH" to "1"), d("5")))
        val small = r.rows[1]
        assertEquals(d("0.05"), small.slice)
        assertTrue(small.tooSmall)
        assertEquals(small.slice, small.leftover)
        assertTrue(r.warnings.any { it.contains("too small") })
        assertEquals(r.totals.budget, r.totals.totalInvested + r.totals.totalFees + r.totals.totalLeftover)
    }

    @Test fun oddCentSlicesSumToBudget() {
        val r = ok(calculate(alloc("A" to "33.33", "B" to "33.33", "C" to "33.34"), d("10.00")))
        assertEquals(d("10.00"), r.rows.fold(BigDecimal.ZERO) { s, x -> s + x.slice })
    }

    @Test fun propertyLoop() {
        val splits = listOf(
            alloc("VOO" to "100"),
            alloc("VOO" to "50", "SMH" to "50"),
            alloc("VOO" to "45", "SMH" to "25", "SCHD" to "15", "IBIT" to "15"),
            alloc("A" to "33.33", "B" to "33.33", "C" to "33.34"),
        )
        var b = d("1")
        while (b <= d("300")) {
            for (s in splits) {
                val r = ok(calculate(s, b))
                assertEquals(r.totals.budget, r.totals.totalInvested + r.totals.totalFees + r.totals.totalLeftover, "budget $b")
                assertEquals(b.setScale(2), r.rows.fold(BigDecimal.ZERO.setScale(2)) { x, y -> x + y.slice })
                for (row in r.rows.filter { !it.tooSmall }) {
                    assertTrue(row.orderAmount + row.totalFee <= row.slice, "budget $b $row")
                    assertTrue(row.leftover.signum() >= 0)
                }
            }
            b += d("0.37")
        }
    }
}
