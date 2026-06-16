package com.josem.monopulcro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.josem.monopulcro.data.MonkeyStateManager
import com.josem.monopulcro.data.Task
import com.josem.monopulcro.widget.MonkeyWidgetReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── Modelos de estado ────────────────────────────────────────────────────────

data class TaskUiState(
    val task: Task,
    val isCompleted: Boolean
)

data class MonkeyUiState(
    val streak: Int = 0,
    val bananas: Int = 0,
    val isCleanToday: Boolean = false,
    val streakBroken: Boolean = false,
    val missedDaysCount: Int = 0,
    val ownedAccessories: Set<String> = emptySet(),
    val equippedAccessory: String? = null,
    /** Tareas programadas para HOY con su estado de completado */
    val todayTasks: List<TaskUiState> = emptyList(),
    /** Todas las tareas para la pantalla de gestión */
    val allTasks: List<Task> = emptyList(),
    /** Evento puntual: se acaba de ganar una banana (dispara animación) */
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

    // ─── Tareas ────────────────────────────────────────────────────────────────

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            val earned = manager.toggleTask(taskId)
            refreshState(justEarnedBanana = earned)
            updateWidget()
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            manager.addTask(task)
            refreshState()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            manager.updateTask(task)
            refreshState()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            manager.deleteTask(taskId)
            refreshState()
        }
    }

    // ─── Tienda ───────────────────────────────────────────────────────────────

    fun buyAccessory(accessoryId: String) {
        viewModelScope.launch {
            manager.buyAccessory(accessoryId)
            refreshState()
            updateWidget()
        }
    }

    fun useAccessory(accessoryId: String) {
        viewModelScope.launch {
            manager.useAccessory(accessoryId)
            refreshState()
            updateWidget()
        }
    }

    fun consumeBananaEvent() {
        _uiState.update { it.copy(justEarnedBanana = false) }
    }

    /** Fuerza una re-lectura del estado. Llamar al volver a esta pantalla. */
    fun refresh() = refreshState()

    // ─── DEBUG ─────────────────────────────────────────────────────────────────

    fun debugMissedDay()    { viewModelScope.launch { manager.debugSimulateMissedDay();    refreshState(); updateWidget() } }
    fun debugCompletedDay() { viewModelScope.launch { manager.debugSimulateCompletedDay(); refreshState(); updateWidget() } }
    fun debugReset()        { viewModelScope.launch { manager.debugReset();                refreshState(); updateWidget() } }

    // ─── Helpers privados ──────────────────────────────────────────────────────

    private fun refreshState(justEarnedBanana: Boolean = false) {
        _uiState.update {
            MonkeyUiState(
                streak            = manager.streakCount,
                bananas           = manager.bananas,
                isCleanToday      = manager.isCleanToday,
                streakBroken      = manager.streakBroken,
                missedDaysCount   = manager.missedDaysCount,
                ownedAccessories  = manager.ownedAccessories,
                equippedAccessory = manager.equippedAccessory,
                todayTasks        = manager.todayTaskStates.map { (task, done) ->
                    TaskUiState(task, done)
                },
                allTasks          = manager.loadTasks(),
                justEarnedBanana  = justEarnedBanana
            )
        }
    }

    private fun updateWidget() {
        MonkeyWidgetReceiver.updateWidget(getApplication())
    }
}
