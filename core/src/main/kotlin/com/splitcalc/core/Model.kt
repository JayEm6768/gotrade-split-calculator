package com.splitcalc.core

import java.math.BigDecimal

data class Allocation(val ticker: String, val percent: BigDecimal)

data class FeeSettings(
    val tradingRatePercent: BigDecimal = BigDecimal("0.3"),
    val minTradingFee: BigDecimal = BigDecimal("0.10"),
    val regulatoryFee: BigDecimal = BigDecimal("0.01"),
)

data class ValidationError(val message: String)

data class RowResult(
    val ticker: String,
    val percent: BigDecimal,
    val slice: BigDecimal,
    val orderAmount: BigDecimal,
    val tradingFee: BigDecimal,
    val regulatoryFee: BigDecimal,
    val totalFee: BigDecimal,
    val leftover: BigDecimal,
    val tooSmall: Boolean,
)

data class Totals(
    val budget: BigDecimal,
    val totalInvested: BigDecimal,
    val totalFees: BigDecimal,
    val totalLeftover: BigDecimal,
    val feePercentOfBudget: BigDecimal,
)

sealed class CalcResult {
    data class Invalid(val errors: List<ValidationError>) : CalcResult()
    data class Success(
        val rows: List<RowResult>,
        val totals: Totals,
        val warnings: List<String>,
    ) : CalcResult()
}
