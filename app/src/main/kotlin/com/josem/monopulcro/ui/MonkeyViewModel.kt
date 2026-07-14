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
import java.time.LocalDate

// ─── Modelos de estado ────────────────────────────────────────────────────────

data class TaskUiState(
    val task: Task,
    val isCompleted: Boolean
)

/** Evento de celebración al completar todas las tareas del día. */
data class StreakCelebration(
    val previousStreak: Int,
    val newStreak: Int,
    val bonusBananas: Int,
    val isMilestone: Boolean
)

enum class TasksViewMode { TODAY, WEEK }

data class WeekDayUi(
    val dayOfWeek: Int,          // 1=Lun … 7=Dom
    val shortLabel: String,      // Lun, Mar…
    val dayOfMonth: Int,
    val isToday: Boolean,
    val tasks: List<TaskUiState>,
    val doneCount: Int,
    val totalCount: Int
) {
    val isRestDay: Boolean get() = totalCount == 0
    val allDone: Boolean get() = totalCount > 0 && doneCount == totalCount
    val previewTitles: List<String> get() = tasks.take(2).map { it.task.name }
}

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
    val weekDays: List<WeekDayUi> = emptyList(),
    val tasksViewMode: TasksViewMode = TasksViewMode.TODAY,
    val dustMotes: List<DustMote> = emptyList(),
    val celebration: StreakCelebration? = null,
    val showShopAffordHint: Boolean = false,
    val showMainTour: Boolean = false
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

        val previousStreak = manager.streakCount
        val earned = manager.toggleTask(taskId)
        val newStreak = manager.streakCount

        val celebration = if (earned) {
            StreakCelebration(
                previousStreak = previousStreak,
                newStreak = newStreak,
                bonusBananas = if (newStreak % 7 == 0) 3 else 0,
                isMilestone = newStreak % 7 == 0
            )
        } else null

        refreshState(celebration = celebration)
        updateWidget()

        if (earned) {
            NotificationHelper.showCelebrationNotification(getApplication())
        }
    }

    fun setTasksViewMode(mode: TasksViewMode) {
        val stored = when (mode) {
            TasksViewMode.TODAY -> MonkeyStateManager.VIEW_MODE_TODAY
            TasksViewMode.WEEK  -> MonkeyStateManager.VIEW_MODE_WEEK
        }
        manager.setTasksViewMode(stored)
        _uiState.update { it.copy(tasksViewMode = mode) }
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
            val wasFirstTaskDuringOnboarding =
                !manager.onboardingCompleted && manager.loadTasks().isEmpty()
            manager.addTask(task)
            if (wasFirstTaskDuringOnboarding) {
                manager.markMainTourPending()
            }
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

    fun consumeCelebration() {
        _uiState.update { it.copy(celebration = null) }
    }

    /** Marca el hint de tienda como visto (solo ocurre una vez en la vida de la app). */
    fun onShopOpened() {
        manager.consumeShopAffordHint()
        _uiState.update { it.copy(showShopAffordHint = false) }
    }

    fun completeMainTour() {
        manager.completeMainTour()
        refreshState()
    }

    fun refresh() = refreshState()

    // ─── Helpers privados ──────────────────────────────────────────────────────

    private fun refreshState(celebration: StreakCelebration? = null) {
        manager.syncDustSpawns()
        val allTasks = manager.loadTasks()
        val viewMode = when (manager.tasksViewMode) {
            MonkeyStateManager.VIEW_MODE_WEEK -> TasksViewMode.WEEK
            else -> TasksViewMode.TODAY
        }
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
                allTasks          = allTasks,
                weekDays          = buildWeekDays(allTasks),
                tasksViewMode     = viewMode,
                dustMotes         = manager.dustMotes,
                celebration       = celebration,
                showShopAffordHint = manager.shouldShowShopAffordHint(),
                showMainTour      = manager.shouldShowMainTour
            )
        }
    }

    private fun buildWeekDays(allTasks: List<Task>): List<WeekDayUi> {
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        return (1..7).map { dow ->
            val date = monday.plusDays((dow - 1).toLong())
            val isToday = dow == today.dayOfWeek.value
            val dayTasks = allTasks
                .filter { dow in it.scheduledDays }
                .map { task ->
                    TaskUiState(
                        task = task,
                        isCompleted = if (isToday) manager.isTaskCompleted(task.id) else false
                    )
                }
            val done = if (isToday) dayTasks.count { it.isCompleted } else 0
            WeekDayUi(
                dayOfWeek = dow,
                shortLabel = DAY_LABELS[dow - 1],
                dayOfMonth = date.dayOfMonth,
                isToday = isToday,
                tasks = dayTasks,
                doneCount = done,
                totalCount = dayTasks.size
            )
        }
    }

    private fun updateWidget() {
        MonkeyWidgetReceiver.updateWidget(getApplication())
    }

    companion object {
        private val DAY_LABELS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    }
}
