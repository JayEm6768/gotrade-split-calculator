package com.splitcalc.app

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.splitcalc.app.data.SplitRepository
import com.splitcalc.app.widget.SplitWidget
import com.splitcalc.core.FeeSettings
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalculatorViewModel(
    repository: SplitRepository?,
    private val persist: (SavedSplit) -> Unit = {},
    initial: CalculatorState = CalculatorState(),
) : ViewModel() {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    init {
        if (!initial.loaded && repository != null) {
            viewModelScope.launch { _state.value = CalculatorState.from(repository.load()) }
        }
    }

    fun onAmountChange(text: String) = update { it.withAmount(text) }
    fun onTickerChange(id: Long, text: String) = update { it.withTicker(id, text) }
    fun onSelect(id: Long) = update { it.select(id) }
    fun onAddRow() = update { it.addRow() }
    fun onRemoveRow(id: Long) = update { it.removeRow(id) }
    fun onPreset(percent: Int) = update { it.applyPercent(BigDecimal(percent)) }
    fun onRest() = update { it.applyRest() }
    fun onCustom(text: String) = update { it.applyCustom(text) }
    fun onFeesChange(fees: FeeSettings) = update { it.withFees(fees) }
    fun onFocusHandled() = update { it.consumeFocusRequest() }
    fun onSaveCombo(name: String) = update { it.saveCombo(name) }
    fun onApplyCombo(name: String) = update { it.applyCombo(name) }
    fun onDeleteCombo(name: String) = update { it.deleteCombo(name) }
    fun onDismissNotice() = update { it.dismissNotice() }

    private fun update(transform: (CalculatorState) -> CalculatorState) {
        val previous = _state.value
        val next = transform(previous)
        _state.value = next
        if (next.loaded && next.toSaved() != previous.toSaved()) persist(next.toSaved())
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                val repository = SplitRepository.get(appContext)
                CalculatorViewModel(
                    repository,
                    persist = { saved -> repository.saveInBackground(saved) { SplitWidget().updateAll(appContext) } },
                )
            }
        }
    }
}
