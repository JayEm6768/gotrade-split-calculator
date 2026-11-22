package com.splitcalc.core

import java.math.BigDecimal
import java.math.RoundingMode

private val TICKER_REGEX = Regex("^[A-Z][A-Z0-9.\\-]{0,9}$")
private val NUMBER_REGEX = Regex("^\\d+(\\.\\d+)?$|^\\.\\d+$")
private val HUNDRED = BigDecimal(100)

fun normalizeTicker(raw: String): String = raw.trim().uppercase()

fun isValidTicker(raw: String): Boolean = TICKER_REGEX.matches(normalizeTicker(raw))

/** Parses user text such as "45", "$1,000.50" or "12.5%". Returns null if not a plain non-negative number. */
fun parseDecimal(text: String): BigDecimal? {
    val cleaned = text.trim().removePrefix("$").removeSuffix("%").replace(",", "").trim()
    if (!NUMBER_REGEX.matches(cleaned)) return null
    return BigDecimal(cleaned)
}

fun validate(
    allocations: List<Allocation>,
    budget: BigDecimal,
    settings: FeeSettings = FeeSettings(),
): List<ValidationError> {
    val errors = mutableListOf<ValidationError>()
    if (allocations.isEmpty()) errors += ValidationError("Add at least one ticker")

    val seen = mutableSetOf<String>()
    for (a in allocations) {
        val t = normalizeTicker(a.ticker)
        if (!TICKER_REGEX.matches(t)) errors += ValidationError("Invalid ticker '${a.ticker.trim()}'")
        else if (!seen.add(t)) errors += ValidationError("Duplicate ticker $t")
        if (a.percent.signum() <= 0 || a.percent > HUNDRED) {
            errors += ValidationError("Percent for $t must be above 0 and at most 100")
        }
    }

    if (allocations.isNotEmpty()) {
        val total = allocations.fold(BigDecimal.ZERO) { s, a -> s + a.percent }
        if ((total - HUNDRED).abs() > BigDecimal("0.01")) {
            val shown = total.setScale(2, RoundingMode.HALF_UP).toPlainString()
            errors += ValidationError("Percentages total $shown%, must be exactly 100%")
        }
    }

    if (budget.signum() <= 0) errors += ValidationError("Budget must be greater than 0")
    if (settings.tradingRatePercent.signum() < 0 || settings.tradingRatePercent >= HUNDRED) {
        errors += ValidationError("Trading rate must be at least 0 and below 100")
    }
    if (settings.minTradingFee.signum() < 0) errors += ValidationError("Minimum trading fee cannot be negative")
    if (settings.regulatoryFee.signum() < 0) errors += ValidationError("Regulatory fee cannot be negative")
    return errors
}
