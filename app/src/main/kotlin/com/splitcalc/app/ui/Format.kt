package com.splitcalc.app.ui

import java.math.BigDecimal
import java.util.Locale

fun BigDecimal.money(): String = String.format(Locale.US, "$%,.2f", this)

fun BigDecimal.percentLabel(): String = stripTrailingZeros().toPlainString() + "%"
