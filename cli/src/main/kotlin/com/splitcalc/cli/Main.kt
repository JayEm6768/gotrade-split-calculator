package com.splitcalc.cli

import com.splitcalc.core.*
import java.math.BigDecimal
import java.util.Locale
import kotlin.system.exitProcess

private val HUNDRED = BigDecimal(100)

/** Prints a prompt and reads a line; null on EOF. */
private fun prompt(text: String): String? {
    print(text)
    System.out.flush()
    val line = readlnOrNull()
    if (line == null) println()
    return line
}

private fun parseSettings(args: Array<String>): FeeSettings {
    var s = FeeSettings()
    var i = 0
    while (i < args.size) {
        val v = args.getOrNull(i + 1)?.let { parseDecimal(it) }
        if (v == null) {
            System.err.println("Invalid or missing value for ${args[i]}")
            exitProcess(2)
        }
        s = when (args[i]) {
            "--rate" -> s.copy(tradingRatePercent = v)
            "--min" -> s.copy(minTradingFee = v)
            "--reg" -> s.copy(regulatoryFee = v)
            else -> {
                System.err.println("Unknown option ${args[i]}")
                exitProcess(2)
            }
        }
        i += 2
    }
    val errors = validate(listOf(Allocation("X", HUNDRED)), BigDecimal.ONE, s)
    if (errors.isNotEmpty()) {
        errors.forEach { System.err.println(it.message) }
        exitProcess(2)
    }
    return s
}

private fun money(v: BigDecimal) = "$" + String.format(Locale.US, "%.2f", v)

private fun readPercent(ticker: String): BigDecimal? {
    while (true) {
        val raw = prompt("Percent for $ticker: ") ?: return null
        val p = parseDecimal(raw)
        if (p == null || p.signum() <= 0 || p > HUNDRED) println("Enter a number above 0 and at most 100.")
        else return p
    }
}

/** Returns a list totalling 100%, or null on EOF/quit. */
private fun readAllocations(): List<Allocation>? {
    while (true) {
        val list = mutableListOf<Allocation>()
        var total = BigDecimal.ZERO
        while (true) {
            val raw = prompt("Ticker (blank to finish): ") ?: return null
            if (raw.isBlank()) break
            val ticker = normalizeTicker(raw)
            if (!isValidTicker(ticker)) {
                println("Invalid ticker '${raw.trim()}'.")
                continue
            }
            if (list.any { it.ticker == ticker }) {
                println("$ticker is already in the list.")
                continue
            }
            val pct = readPercent(ticker) ?: return null
            list += Allocation(ticker, pct)
            total += pct
            println("Running total: ${total.toPlainString()}%")
        }
        val errors = validate(list, BigDecimal.ONE)
        if (errors.isEmpty()) return list
        errors.forEach { println("Error: ${it.message}") }
        while (true) {
            when (prompt("[e]dit (restart list) or [q]uit? ")?.trim()?.lowercase()) {
                null, "q" -> return null
                "e" -> break
            }
        }
    }
}

private fun readBudget(): BigDecimal? {
    while (true) {
        val raw = prompt("Amount to invest (USD): ") ?: return null
        val b = parseDecimal(raw)
        if (b == null || b.signum() <= 0) println("Enter an amount greater than 0.") else return b
    }
}

private fun printResult(r: CalcResult.Success) {
    val fmt = "%-8s %7s %10s %13s %12s %8s %9s"
    println(String.format(Locale.US, fmt, "Ticker", "%", "Slice", "Order amount", "Trading fee", "Reg fee", "Leftover"))
    for (x in r.rows) {
        println(
            String.format(
                Locale.US, fmt, x.ticker, x.percent.stripTrailingZeros().toPlainString() + "%",
                money(x.slice), money(x.orderAmount), money(x.tradingFee), money(x.regulatoryFee), money(x.leftover),
            )
        )
    }
    val t = r.totals
    println()
    println("Budget:         ${money(t.budget)}")
    println("Total invested: ${money(t.totalInvested)}")
    println("Total fees:     ${money(t.totalFees)} (${t.feePercentOfBudget}% of budget)")
    println("Leftover cash:  ${money(t.totalLeftover)}")
    r.warnings.forEach { println("Warning: $it") }
}

fun main(args: Array<String>) {
    System.setOut(java.io.PrintStream(java.io.FileOutputStream(java.io.FileDescriptor.out), true, "UTF-8"))
    val settings = parseSettings(args)
    val list = readAllocations() ?: return
    while (true) {
        val budget = readBudget() ?: return
        when (val r = calculate(list, budget, settings)) {
            is CalcResult.Invalid -> r.errors.forEach { println("Error: ${it.message}") }
            is CalcResult.Success -> printResult(r)
        }
        println()
        val again = prompt("Calculate another amount with the same list? [y/n] ")?.trim()?.lowercase()
        if (again != "y") return
    }
}
