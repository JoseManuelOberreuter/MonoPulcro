package com.josem.monopulcro.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

class MonkeyStateManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─── Propiedades ───────────────────────────────────────────────────────────

    val streakCount: Int get() = prefs.getInt(KEY_STREAK, 0)
    val bananas: Int get() = prefs.getInt(KEY_BANANAS, 0)
    val hasGlasses: Boolean get() = prefs.getBoolean(KEY_HAS_GLASSES, false)
    val isCleanToday: Boolean get() = taskStates.all { it }

    /** true cuando la racha fue rota al no completar tareas el día anterior */
    val streakBroken: Boolean get() = prefs.getBoolean(KEY_STREAK_BROKEN, false)

    /** Días consecutivos sin completar tareas (racha negativa) */
    val missedDaysCount: Int get() = prefs.getInt(KEY_MISSED_DAYS, 0)

    val taskStates: List<Boolean>
        get() = TASK_KEYS.map { prefs.getBoolean(it, false) }

    // ─── Lógica de reset diario ────────────────────────────────────────────────

    fun checkAndResetForNewDay() {
        val today = LocalDate.now().toString()
        val lastReset = prefs.getString(KEY_LAST_RESET, "") ?: ""

        if (lastReset == today) return

        val streakAlreadyCounted = prefs.getBoolean(KEY_STREAK_COUNTED, false)
        val wasCleanYesterday = taskStates.all { it }
        val hadPreviousDay = lastReset.isNotEmpty()

        prefs.edit().apply {
            if (hadPreviousDay) {
                if (streakAlreadyCounted) {
                    // Completó ayer → racha sana, reset días perdidos
                    putBoolean(KEY_STREAK_BROKEN, false)
                    putInt(KEY_MISSED_DAYS, 0)
                } else if (!wasCleanYesterday) {
                    // No completó ayer → racha a 0, incrementar días perdidos
                    putInt(KEY_STREAK, 0)
                    putBoolean(KEY_STREAK_BROKEN, true)
                    putInt(KEY_MISSED_DAYS, missedDaysCount + 1)
                }
            }
            TASK_KEYS.forEach { key -> putBoolean(key, false) }
            putBoolean(KEY_REWARD_GIVEN, false)
            putBoolean(KEY_STREAK_COUNTED, false)
            putString(KEY_LAST_RESET, today)
            apply()
        }
    }

    // ─── Acciones del usuario ──────────────────────────────────────────────────

    fun toggleTask(taskIndex: Int): Boolean {
        require(taskIndex in TASK_KEYS.indices) { "Índice de tarea inválido: $taskIndex" }

        val rewardAlreadyGiven = prefs.getBoolean(KEY_REWARD_GIVEN, false)
        val newState = !prefs.getBoolean(TASK_KEYS[taskIndex], false)

        prefs.edit().putBoolean(TASK_KEYS[taskIndex], newState).apply()

        return when {
            // Marcó la última tarea → recompensa + limpiar streakBroken
            newState && isCleanToday && !rewardAlreadyGiven -> {
                prefs.edit()
                    .putInt(KEY_BANANAS, bananas + 1)
                    .putInt(KEY_STREAK, streakCount + 1)
                    .putBoolean(KEY_REWARD_GIVEN, true)
                    .putBoolean(KEY_STREAK_COUNTED, true)
                    .putBoolean(KEY_STREAK_BROKEN, false)
                    .putInt(KEY_MISSED_DAYS, 0)            // se redimió hoy
                    .apply()
                true
            }
            // Desmarcó estando todo completo → devolver recompensa
            !newState && rewardAlreadyGiven -> {
                prefs.edit()
                    .putInt(KEY_BANANAS, maxOf(0, bananas - 1))
                    .putInt(KEY_STREAK, maxOf(0, streakCount - 1))
                    .putBoolean(KEY_REWARD_GIVEN, false)
                    .putBoolean(KEY_STREAK_COUNTED, false)
                    .apply()
                false
            }
            else -> false
        }
    }

    // ─── DEBUG (quitar antes de publicar) ─────────────────────────────────────

    fun debugSimulateMissedDay() {
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()
        val newMissed = missedDaysCount + 1
        prefs.edit()
            .putString(KEY_LAST_RESET, yesterday)
            .also { TASK_KEYS.forEach { key -> it.putBoolean(key, false) } }
            .putBoolean(KEY_REWARD_GIVEN, false)
            .putBoolean(KEY_STREAK_COUNTED, false)
            .putInt(KEY_STREAK, 0)
            .putBoolean(KEY_STREAK_BROKEN, true)
            .putInt(KEY_MISSED_DAYS, newMissed)   // incremento directo
            .putString(KEY_LAST_RESET, java.time.LocalDate.now().toString())
            .apply()
    }

    fun debugSimulateCompletedDay() {
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()
        prefs.edit()
            .putString(KEY_LAST_RESET, yesterday)
            .also { TASK_KEYS.forEach { key -> it.putBoolean(key, true) } }
            .putBoolean(KEY_REWARD_GIVEN, true)
            .putBoolean(KEY_STREAK_COUNTED, true)
            .putBoolean(KEY_STREAK_BROKEN, false)
            .apply()
        checkAndResetForNewDay()
    }

    fun debugReset() {
        prefs.edit().clear().apply()
    }

    // ──────────────────────────────────────────────────────────────────────────

    fun buyGlasses(): Boolean {
        if (bananas < 10 || hasGlasses) return false

        prefs.edit()
            .putInt(KEY_BANANAS, bananas - 10)
            .putBoolean(KEY_HAS_GLASSES, true)
            .apply()

        return true
    }

    // ─── Constantes ────────────────────────────────────────────────────────────

    companion object {
        const val PREFS_NAME = "monkey_prefs"

        const val KEY_STREAK         = "streakCount"
        const val KEY_BANANAS        = "bananas"
        const val KEY_HAS_GLASSES    = "hasGlasses"
        const val KEY_LAST_RESET     = "lastResetDate"
        const val KEY_REWARD_GIVEN   = "rewardGivenToday"
        const val KEY_STREAK_COUNTED = "streakCountedToday"
        const val KEY_STREAK_BROKEN  = "streakBroken"
        const val KEY_MISSED_DAYS    = "missedDaysCount"

        val TASKS = listOf("Lavar platos", "Hacer la cama", "Sacar la basura", "Limpiar el baño")
        val TASK_KEYS = List(TASKS.size) { index -> "task_$index" }
    }
}
