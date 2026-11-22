package com.splitcalc.core

import java.math.BigDecimal
import java.math.RoundingMode

private val CENT = BigDecimal("0.01")
private val HUNDRED = BigDecimal(100)

/** Splits budget into cent slices using the largest-remainder method; slices sum exactly to budget. */
internal fun splitSlices(budget: BigDecimal, percents: List<BigDecimal>): List<BigDecimal> {
    val totalPercent = percents.fold(BigDecimal.ZERO) { s, p -> s + p }
    val exact = percents.map { budget.multiply(it).divide(totalPercent, 10, RoundingMode.HALF_UP) }
    val floors = exact.map { it.setScale(2, RoundingMode.DOWN) }
    var centsLeft = budget.subtract(floors.fold(BigDecimal.ZERO) { s, f -> s + f }).movePointRight(2).toInt()
    val order = exact.indices.sortedWith(compareByDescending<Int> { exact[it] - floors[it] }.thenBy { it })
    val result = floors.toMutableList()
    for (i in order) {
        if (centsLeft <= 0) break
        result[i] = result[i] + CENT
        centsLeft--
    }
    return result
}

private fun tradingFeeFor(n: BigDecimal, rate: BigDecimal, min: BigDecimal): BigDecimal =
    n.multiply(rate).setScale(2, RoundingMode.UP).max(min)

internal fun computeRow(a: Allocation, ticker: String, slice: BigDecimal, s: FeeSettings): RowResult {
    val r = s.tradingRatePercent.divide(HUNDRED)
    val min = s.minTradingFee
    val reg = s.regulatoryFee
    val afterMin = slice - min - reg
    // "afterMin <= min / r" without dividing
    var n = if (r.signum() == 0 || afterMin.multiply(r) <= min) afterMin
    else (slice - reg).divide(BigDecimal.ONE + r, 10, RoundingMode.DOWN)
    n = n.setScale(2, RoundingMode.DOWN)

    var fee = tradingFeeFor(n, r, min)
    while (n + fee + reg > slice && n.signum() > 0) {
        n -= CENT
        fee = tradingFeeFor(n, r, min)
    }
    if (n.signum() <= 0) {
        val zero = BigDecimal.ZERO.setScale(2)
        return RowResult(ticker, a.percent, slice, zero, zero, zero, zero, slice, tooSmall = true)
    }
    val totalFee = fee + reg
    return RowResult(ticker, a.percent, slice, n, fee, reg, totalFee, slice - n - totalFee, tooSmall = false)
}

fun calculate(
    allocations: List<Allocation>,
    budget: BigDecimal,
    settings: FeeSettings = FeeSettings(),
): CalcResult {
    val errors = validate(allocations, budget, settings)
    if (errors.isNotEmpty()) return CalcResult.Invalid(errors)

    val cleanBudget = budget.setScale(2, RoundingMode.HALF_UP)
    val slices = splitSlices(cleanBudget, allocations.map { it.percent })
    val rows = allocations.mapIndexed { i, a -> computeRow(a, normalizeTicker(a.ticker), slices[i], settings) }

    fun sum(f: (RowResult) -> BigDecimal) = rows.fold(BigDecimal.ZERO.setScale(2)) { s, r -> s + f(r) }
    val fees = sum { it.totalFee }
    val feePct = fees.multiply(HUNDRED).divide(cleanBudget, 2, RoundingMode.HALF_UP)
    val totals = Totals(cleanBudget, sum { it.orderAmount }, fees, sum { it.leftover }, feePct)

    val warnings = mutableListOf<String>()
    if (feePct > BigDecimal("1.00")) {
        val min = settings.minTradingFee.setScale(2, RoundingMode.HALF_UP)
        warnings += "Fees are $feePct% of your budget — minimum fee $$min per order applies; " +
            "fewer tickers or a bigger amount lowers this."
    }
    rows.filter { it.tooSmall }.forEach { warnings += "Slice $${it.slice} is too small to cover fees (${it.ticker})" }
    return CalcResult.Success(rows, totals, warnings)
}
