package com.josem.monopulcro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.josem.monopulcro.audio.SoundManager
import com.josem.monopulcro.data.DustMote
import com.josem.monopulcro.data.MonkeyStateManager
import com.josem.monopulcro.data.Task
import com.josem.monopulcro.notifications.NotificationHelper
import com.josem.monopulcro.notifications.TaskNotificationScheduler
import com.josem.monopulcro.widget.MonkeyWidgetReceiver
import com.josem.monopulcro.widget.WidgetUpdateScheduler
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.josem.monopulcro.widget.MonkeyWidget
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
    val todayTasks: List<TaskUiState> = emptyList(),
    val allTasks: List<Task> = emptyList(),
    val dustMotes: List<DustMote> = emptyList(),
    val justEarnedBanana: Boolean = false
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class MonkeyViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = MonkeyStateManager(application)
    private val sounds  = SoundManager.get(application)

    private val _uiState = MutableStateFlow(MonkeyUiState())
    val uiState: StateFlow<MonkeyUiState> = _uiState.asStateFlow()

    init {
        manager.checkAndResetForNewDay()
        refreshState()
        viewModelScope.launch {
            val hasWidget = GlanceAppWidgetManager(getApplication())
                .getGlanceIds(MonkeyWidget::class.java)
                .isNotEmpty()
            if (hasWidget) WidgetUpdateScheduler.schedule(getApplication())
        }
        try {
            TaskNotificationScheduler.scheduleAll(application)
        } catch (_: Exception) {
            // No bloquear el arranque si falla la programación de alarmas
        }
    }

    // ─── Tareas ────────────────────────────────────────────────────────────────

    fun toggleTask(taskId: String) {
        val wasDone = manager.isTaskCompleted(taskId)
        if (!wasDone) sounds.playTaskPop()

        val earned = manager.toggleTask(taskId)

        refreshState(justEarnedBanana = earned)
        updateWidget()

        if (earned) {
            NotificationHelper.showCelebrationNotification(getApplication())
        }
    }

    /** Devuelve cuántas motas había al tocar (para animación y recompensa). */
    fun dustMotesForCleaning(): List<DustMote> = _uiState.value.dustMotes

    fun completeDustCleaning() {
        manager.rewardDustCleaning()
        refreshState()
        updateWidget()
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            manager.addTask(task)
            TaskNotificationScheduler.scheduleAll(getApplication())
            refreshState()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            manager.updateTask(task)
            TaskNotificationScheduler.scheduleAll(getApplication())
            refreshState()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            manager.deleteTask(taskId)
            TaskNotificationScheduler.cancelTask(getApplication(), taskId)
            TaskNotificationScheduler.scheduleAll(getApplication())
            refreshState()
        }
    }

    // ─── Tienda ───────────────────────────────────────────────────────────────

    fun buyAccessory(accessoryId: String) {
        if (manager.buyAccessory(accessoryId)) {
            sounds.playCashRegister()
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

    fun refresh() = refreshState()

    // ─── Helpers privados ──────────────────────────────────────────────────────

    private fun refreshState(justEarnedBanana: Boolean = false) {
        manager.syncDustSpawns()
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
                dustMotes         = manager.dustMotes,
                justEarnedBanana  = justEarnedBanana
            )
        }
    }

    private fun updateWidget() {
        MonkeyWidgetReceiver.updateWidget(getApplication())
    }
}
