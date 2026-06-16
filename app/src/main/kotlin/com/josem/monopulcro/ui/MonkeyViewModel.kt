package com.josem.monopulcro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.josem.monopulcro.data.MonkeyStateManager
import com.josem.monopulcro.widget.MonkeyWidgetReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── Estado inmutable de la UI ─────────────────────────────────────────────────

data class MonkeyUiState(
    val streak: Int = 0,
    val bananas: Int = 0,
    val hasGlasses: Boolean = false,
    val isCleanToday: Boolean = false,
    val streakBroken: Boolean = false,
    val missedDaysCount: Int = 0,
    val taskStates: List<Boolean> = List(MonkeyStateManager.TASKS.size) { false },
    /** Evento puntual: se acaba de ganar una banana (para mostrar feedback) */
    val justEarnedBanana: Boolean = false
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class MonkeyViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = MonkeyStateManager(application)

    private val _uiState = MutableStateFlow(MonkeyUiState())
    val uiState: StateFlow<MonkeyUiState> = _uiState.asStateFlow()

    init {
        manager.checkAndResetForNewDay()
        refreshState()
    }

    // ─── Acciones públicas ─────────────────────────────────────────────────────

    fun toggleTask(index: Int) {
        viewModelScope.launch {
            val earnedBanana = manager.toggleTask(index)
            refreshState(justEarnedBanana = earnedBanana)
            updateWidget()
        }
    }

    fun buyGlasses() {
        viewModelScope.launch {
            manager.buyGlasses()
            refreshState()
            updateWidget()
        }
    }

    /** Descarta el evento puntual de banana ganada */
    fun consumeBananaEvent() {
        _uiState.update { it.copy(justEarnedBanana = false) }
    }

    // ─── Helpers privados ──────────────────────────────────────────────────────

    private fun refreshState(justEarnedBanana: Boolean = false) {
        _uiState.update {
            MonkeyUiState(
                streak = manager.streakCount,
                bananas = manager.bananas,
                hasGlasses = manager.hasGlasses,
                isCleanToday = manager.isCleanToday,
                streakBroken = manager.streakBroken,
                missedDaysCount = manager.missedDaysCount,
                taskStates = manager.taskStates,
                justEarnedBanana = justEarnedBanana
            )
        }
    }

    // ─── DEBUG ─────────────────────────────────────────────────────────────────
    fun debugMissedDay() { viewModelScope.launch { manager.debugSimulateMissedDay(); refreshState(); updateWidget() } }
    fun debugCompletedDay() { viewModelScope.launch { manager.debugSimulateCompletedDay(); refreshState(); updateWidget() } }
    fun debugReset() { viewModelScope.launch { manager.debugReset(); refreshState(); updateWidget() } }

    private fun updateWidget() {
        MonkeyWidgetReceiver.updateWidget(getApplication())
    }

}
