package com.josem.monopulcro.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class MonkeyStateManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ─── Tareas ────────────────────────────────────────────────────────────────

    fun loadTasks(): List<Task> {
        val json = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Task>>() {}.type
            gson.fromJson<List<Task>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveTasks(tasks: List<Task>) {
        prefs.edit().putString(KEY_TASKS, gson.toJson(tasks)).apply()
    }

    fun addTask(task: Task) {
        val tasks = loadTasks().toMutableList()
        tasks.add(task)
        saveTasks(tasks)
    }

    fun updateTask(task: Task) {
        val tasks = loadTasks().toMutableList()
        val idx = tasks.indexOfFirst { it.id == task.id }
        if (idx >= 0) tasks[idx] = task
        saveTasks(tasks)
    }

    fun deleteTask(taskId: String) {
        saveTasks(loadTasks().filter { it.id != taskId })
        prefs.edit().remove(taskKey(taskId)).apply()
    }

    val todayTasks: List<Task>
        get() {
            val dow = LocalDate.now().dayOfWeek.value   // 1=Lun … 7=Dom
            return loadTasks().filter { dow in it.scheduledDays }
        }

    fun isTaskCompleted(taskId: String): Boolean =
        prefs.getBoolean(taskKey(taskId), false)

    val todayTaskStates: List<Pair<Task, Boolean>>
        get() = todayTasks.map { it to isTaskCompleted(it.id) }

    // ─── Estado derivado ───────────────────────────────────────────────────────

    val isCleanToday: Boolean
        get() {
            val all = loadTasks()
            if (all.isEmpty()) return false          // 0 tareas → siempre sucio
            val today = todayTasks
            if (today.isEmpty()) return true         // día de descanso → limpio
            return today.all { isTaskCompleted(it.id) }
        }

    val streakCount: Int  get() = prefs.getInt(KEY_STREAK, 0)
    val bananas: Int      get() = prefs.getInt(KEY_BANANAS, 0)
    val streakBroken: Boolean get() = prefs.getBoolean(KEY_STREAK_BROKEN, false)
    val missedDaysCount: Int  get() = prefs.getInt(KEY_MISSED_DAYS, 0)

    val ownedAccessories: Set<String>
        get() = prefs.getStringSet(KEY_OWNED_ACCESSORIES, emptySet()) ?: emptySet()
    val equippedAccessory: String?
        get() = prefs.getString(KEY_EQUIPPED_ACCESSORY, "").takeIf { it?.isNotEmpty() == true }

    val onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    // ─── Reset diario ──────────────────────────────────────────────────────────

    fun checkAndResetForNewDay() {
        val today = LocalDate.now().toString()
        val lastReset = prefs.getString(KEY_LAST_RESET, "") ?: ""
        if (lastReset == today) return

        val streakAlreadyCounted = prefs.getBoolean(KEY_STREAK_COUNTED, false)
        val allTasks = loadTasks()
        val hadPreviousDay = lastReset.isNotEmpty()

        prefs.edit().apply {
            if (hadPreviousDay) {
                val lastDow = LocalDate.parse(lastReset).dayOfWeek.value
                val tasksForLastDay = allTasks.filter { lastDow in it.scheduledDays }

                when {
                    streakAlreadyCounted -> {
                        // Completó ayer → racha sana
                        putBoolean(KEY_STREAK_BROKEN, false)
                        putInt(KEY_MISSED_DAYS, 0)
                    }
                    allTasks.isEmpty() -> {
                        // Sin tareas → mono se ensucia igual (incentivo)
                        putInt(KEY_MISSED_DAYS, missedDaysCount + 1)
                    }
                    tasksForLastDay.isEmpty() -> {
                        // Día de descanso ayer → racha intacta, sin cambios
                    }
                    else -> {
                        val wasClean = tasksForLastDay.all { prefs.getBoolean(taskKey(it.id), false) }
                        if (!wasClean) {
                            putInt(KEY_STREAK, 0)
                            putBoolean(KEY_STREAK_BROKEN, true)
                            putInt(KEY_MISSED_DAYS, missedDaysCount + 1)
                        }
                    }
                }
            }
            // Limpiar estados de completado del día anterior
            allTasks.forEach { remove(taskKey(it.id)) }
            putBoolean(KEY_REWARD_GIVEN, false)
            putBoolean(KEY_STREAK_COUNTED, false)
            putBoolean(KEY_STREAK_BONUS_GIVEN, false)
            putString(KEY_LAST_RESET, today)
            apply()
        }
    }

    // ─── Acciones del usuario ──────────────────────────────────────────────────

    /**
     * Marca/desmarca una tarea. Devuelve true si se ganó recompensa (todas completadas).
     */
    fun toggleTask(taskId: String): Boolean {
        val rewardAlreadyGiven = prefs.getBoolean(KEY_REWARD_GIVEN, false)
        val newState = !prefs.getBoolean(taskKey(taskId), false)
        prefs.edit().putBoolean(taskKey(taskId), newState).apply()

        val allTodayDone = todayTasks.isNotEmpty() && todayTasks.all { isTaskCompleted(it.id) }

        return when {
            newState && allTodayDone && !rewardAlreadyGiven -> {
                val newStreak = streakCount + 1
                val isMilestone = newStreak % 7 == 0
                val bonusBananas = if (isMilestone) 3 else 0
                prefs.edit()
                    .putInt(KEY_BANANAS, bananas + 1 + bonusBananas)
                    .putInt(KEY_STREAK, newStreak)
                    .putBoolean(KEY_REWARD_GIVEN, true)
                    .putBoolean(KEY_STREAK_COUNTED, true)
                    .putBoolean(KEY_STREAK_BROKEN, false)
                    .putInt(KEY_MISSED_DAYS, 0)
                    .putBoolean(KEY_STREAK_BONUS_GIVEN, isMilestone)
                    .apply()
                true
            }
            !newState && rewardAlreadyGiven -> {
                val hadBonus = prefs.getBoolean(KEY_STREAK_BONUS_GIVEN, false)
                prefs.edit()
                    .putInt(KEY_BANANAS, maxOf(0, bananas - 1 - if (hadBonus) 3 else 0))
                    .putInt(KEY_STREAK, maxOf(0, streakCount - 1))
                    .putBoolean(KEY_REWARD_GIVEN, false)
                    .putBoolean(KEY_STREAK_COUNTED, false)
                    .putBoolean(KEY_STREAK_BONUS_GIVEN, false)
                    .apply()
                false
            }
            else -> false
        }
    }

    fun buyAccessory(accessoryId: String): Boolean {
        val item = ACCESSORIES.find { it.id == accessoryId } ?: return false
        if (bananas < item.price || accessoryId in ownedAccessories) return false
        val newOwned = ownedAccessories.toMutableSet().also { it.add(accessoryId) }
        prefs.edit()
            .putInt(KEY_BANANAS, bananas - item.price)
            .putStringSet(KEY_OWNED_ACCESSORIES, newOwned)
            .apply()
        return true
    }

    fun useAccessory(accessoryId: String) {
        prefs.edit().putString(KEY_EQUIPPED_ACCESSORY, accessoryId).apply()
    }

    // ─── Motas de polvo ────────────────────────────────────────────────────────

    val dustMotes: List<DustMote> get() = loadDustMotes()

    val dustCount: Int get() = dustMotes.size

    /** Sincroniza motas según horas transcurridas (máx. 5, +1 por hora). */
    fun syncDustSpawns() {
        val now = currentTimeMs()
        val motes = loadDustMotes().toMutableList()
        var lastSpawn = prefs.getLong(KEY_DUST_LAST_SPAWN_MS, 0L)

        if (lastSpawn == 0L) {
            prefs.edit().putLong(KEY_DUST_LAST_SPAWN_MS, now).apply()
            return
        }

        var changed = false
        while (motes.size < MAX_DUST_MOTES) {
            val nextSpawn = lastSpawn + HOUR_MS
            if (now < nextSpawn) break
            motes.add(DustMote.SLOTS[motes.size])
            lastSpawn = nextSpawn
            changed = true
        }

        if (changed) {
            saveDustMotes(motes)
            prefs.edit().putLong(KEY_DUST_LAST_SPAWN_MS, lastSpawn).apply()
        }
    }

    /** Limpia motas y otorga 1 banana. Devuelve true si había motas. */
    fun rewardDustCleaning(): Boolean {
        if (loadDustMotes().isEmpty()) return false
        prefs.edit()
            .putString(KEY_DUST_MOTES, "[]")
            .remove(KEY_DUST_COUNT)
            .putInt(KEY_BANANAS, bananas + 1)
            .putLong(KEY_DUST_LAST_SPAWN_MS, currentTimeMs())
            .apply()
        return true
    }

    private fun loadDustMotes(): List<DustMote> {
        val json = prefs.getString(KEY_DUST_MOTES, null)
        val count = when {
            json != null -> {
                try {
                    val type = object : TypeToken<List<DustMote>>() {}.type
                    gson.fromJson<List<DustMote>>(json, type)?.size ?: 0
                } catch (_: Exception) {
                    0
                }
            }
            else -> prefs.getInt(KEY_DUST_COUNT, 0)
        }.coerceIn(0, MAX_DUST_MOTES)

        val motes = dustMotesForCount(count, MAX_DUST_MOTES)
        if (json != null || count > 0) {
            val savedJson = prefs.getString(KEY_DUST_MOTES, null)
            val needsSave = savedJson == null ||
                savedJson != gson.toJson(motes)
            if (needsSave) {
                saveDustMotes(motes)
                if (json == null) prefs.edit().remove(KEY_DUST_COUNT).apply()
            }
        }
        return motes
    }

    private fun saveDustMotes(motes: List<DustMote>) {
        val canonical = dustMotesForCount(motes.size, MAX_DUST_MOTES)
        prefs.edit().putString(KEY_DUST_MOTES, gson.toJson(canonical)).apply()
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun currentTimeMs(): Long = System.currentTimeMillis()

    private fun taskKey(taskId: String) = "done_$taskId"

    // ─── Constantes ────────────────────────────────────────────────────────────

    companion object {
        const val PREFS_NAME           = "monkey_prefs"
        const val KEY_STREAK           = "streakCount"
        const val KEY_BANANAS          = "bananas"
        const val KEY_LAST_RESET       = "lastResetDate"
        const val KEY_REWARD_GIVEN     = "rewardGivenToday"
        const val KEY_STREAK_COUNTED   = "streakCountedToday"
        const val KEY_STREAK_BROKEN    = "streakBroken"
        const val KEY_MISSED_DAYS      = "missedDaysCount"
        const val KEY_TASKS            = "tasksJson"
        const val KEY_OWNED_ACCESSORIES  = "ownedAccessories"
        const val KEY_EQUIPPED_ACCESSORY = "equippedAccessory"
        const val KEY_STREAK_BONUS_GIVEN = "streakBonusGiven"
        const val KEY_ONBOARDING_DONE    = "onboardingDone"
        const val KEY_DUST_COUNT         = "dustCount"
        const val KEY_DUST_MOTES         = "dustMotesJson"
        const val KEY_DUST_LAST_SPAWN_MS = "dustLastSpawnMs"

        const val MAX_DUST_MOTES = 5
        const val HOUR_MS = 3_600_000L

        data class AccessoryItem(val id: String, val name: String, val price: Int)

        val ACCESSORIES = listOf(
            AccessoryItem("glasses",   "Lentes",      3),
            AccessoryItem("hat",       "Gorro",       7),
            AccessoryItem("crown",     "Corona",     14),
            AccessoryItem("chaleco",   "Chaleco",    10),
            AccessoryItem("astronaut", "Astronauta", 30),
            AccessoryItem("gold",      "Mono de oro", 50)
        )
    }
}
