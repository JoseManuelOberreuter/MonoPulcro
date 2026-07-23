package com.josem.monopulcro.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class MonkeyStateManager(
    context: Context,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
    prefsOverride: SharedPreferences? = null,
) {

    private val prefs: SharedPreferences =
        prefsOverride ?: context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        migrateRemoveGoldAccessory()
        ensureShieldsInitialized()
    }

    private fun migrateRemoveGoldAccessory() {
        if (prefs.getString(KEY_EQUIPPED_ACCESSORY, "") == "gold") {
            prefs.edit().remove(KEY_EQUIPPED_ACCESSORY).apply()
        }
        val owned = prefs.getStringSet(KEY_OWNED_ACCESSORIES, emptySet()) ?: emptySet()
        if ("gold" in owned) {
            prefs.edit().putStringSet(KEY_OWNED_ACCESSORIES, owned - "gold").apply()
        }
    }

    /** One-shot: otorga 3 escudos a usuarios nuevos y existentes. Idempotente. */
    fun ensureShieldsInitialized() {
        if (prefs.getBoolean(KEY_SHIELDS_INITIALIZED, false)) return
        prefs.edit()
            .putInt(KEY_SHIELDS_COUNT, minOf(INITIAL_SHIELDS, MAX_SHIELDS))
            .putBoolean(KEY_SHIELDS_INITIALIZED, true)
            .commit()
    }

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

    /** Fecha de juego (hoy real + offset de debug). */
    fun currentGameDate(): LocalDate =
        todayProvider().plusDays(prefs.getInt(KEY_DEBUG_DAY_OFFSET, 0).toLong())

    val todayTasks: List<Task>
        get() {
            val dow = currentGameDate().dayOfWeek.value   // 1=Lun … 7=Dom
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

    val shieldsCount: Int get() = prefs.getInt(KEY_SHIELDS_COUNT, 0)
    val shieldsInitialized: Boolean get() = prefs.getBoolean(KEY_SHIELDS_INITIALIZED, false)
    val claimedShieldMilestones: Set<String>
        get() = prefs.getStringSet(KEY_SHIELD_MILESTONES_CLAIMED, emptySet()) ?: emptySet()

    val ownedAccessories: Set<String>
        get() = prefs.getStringSet(KEY_OWNED_ACCESSORIES, emptySet()) ?: emptySet()
    val equippedAccessory: String?
        get() = prefs.getString(KEY_EQUIPPED_ACCESSORY, "").takeIf { it?.isNotEmpty() == true }

    val onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    val shouldShowMainTour: Boolean
        get() = prefs.getBoolean(KEY_MAIN_TOUR_PENDING, false) && !onboardingCompleted

    fun markMainTourPending() {
        prefs.edit().putBoolean(KEY_MAIN_TOUR_PENDING, true).apply()
    }

    fun clearMainTourPending() {
        prefs.edit().remove(KEY_MAIN_TOUR_PENDING).apply()
    }

    fun completeMainTour() {
        prefs.edit()
            .remove(KEY_MAIN_TOUR_PENDING)
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .apply()
    }

    // ─── Vista de tareas (lista / semana) ───────────────────────────────────────

    val tasksViewMode: String
        get() = prefs.getString(KEY_TASKS_VIEW_MODE, VIEW_MODE_TODAY) ?: VIEW_MODE_TODAY

    fun setTasksViewMode(mode: String) {
        prefs.edit().putString(KEY_TASKS_VIEW_MODE, mode).apply()
    }

    // ─── Escudos de Pulcritud ──────────────────────────────────────────────────

    /**
     * Otorga escudos por hitos de racha one-shot. Respeta MAX_SHIELDS.
     * Marca el hito aunque grant=0 (inventario lleno) para no reintentar.
     * @return escudos efectivamente añadidos
     */
    fun claimShieldMilestonesIfNeeded(newStreak: Int): Int {
        val claimed = claimedShieldMilestones.toMutableSet()
        var current = shieldsCount
        var granted = 0
        var changed = false

        for ((threshold, amount) in SHIELD_MILESTONES) {
            val key = threshold.toString()
            if (newStreak < threshold || key in claimed) continue
            val room = MAX_SHIELDS - current
            val grant = minOf(amount, room).coerceAtLeast(0)
            if (grant > 0) {
                current += grant
                granted += grant
            }
            claimed.add(key)
            changed = true
        }

        if (changed) {
            prefs.edit()
                .putInt(KEY_SHIELDS_COUNT, current)
                .putStringSet(KEY_SHIELD_MILESTONES_CLAIMED, HashSet(claimed))
                .commit()
        }
        return granted
    }

    /** True si hay mensaje pendiente de “escudo protegió la racha”. */
    fun consumePendingShieldUsedMessage(): Boolean {
        if (!prefs.getBoolean(KEY_PENDING_SHIELD_USED_MESSAGE, false)) return false
        prefs.edit().putBoolean(KEY_PENDING_SHIELD_USED_MESSAGE, false).commit()
        return true
    }

    val hasPendingShieldUsedMessage: Boolean
        get() = prefs.getBoolean(KEY_PENDING_SHIELD_USED_MESSAGE, false)

    // ─── Reset diario ──────────────────────────────────────────────────────────

    fun checkAndResetForNewDay() {
        val today = currentGameDate().toString()
        val lastReset = prefs.getString(KEY_LAST_RESET, "") ?: ""
        if (lastReset == today) return

        val streakAlreadyCounted = prefs.getBoolean(KEY_STREAK_COUNTED, false)
        val allTasks = loadTasks()
        val hadPreviousDay = lastReset.isNotEmpty()

        val editor = prefs.edit()
        if (hadPreviousDay) {
            val evaluatedDate = LocalDate.parse(lastReset)
            val lastDow = evaluatedDate.dayOfWeek.value
            val tasksForLastDay = allTasks.filter { lastDow in it.scheduledDays }

            when {
                streakAlreadyCounted -> {
                    // Completó ayer → racha sana
                    editor.putBoolean(KEY_STREAK_BROKEN, false)
                    editor.putInt(KEY_MISSED_DAYS, 0)
                }
                allTasks.isEmpty() -> {
                    // Sin tareas → mono se ensucia igual (incentivo); no rompe racha
                    editor.putInt(KEY_MISSED_DAYS, missedDaysCount + 1)
                }
                tasksForLastDay.isEmpty() -> {
                    // Día de descanso ayer → racha intacta, sin cambios
                }
                else -> {
                    val wasClean = tasksForLastDay.all { prefs.getBoolean(taskKey(it.id), false) }
                    if (!wasClean) {
                        applyShieldOrBreakStreak(editor, evaluatedDate.toString())
                    }
                }
            }
        }
        // Limpiar estados de completado del día anterior
        allTasks.forEach { editor.remove(taskKey(it.id)) }
        editor.putBoolean(KEY_REWARD_GIVEN, false)
        editor.putBoolean(KEY_STREAK_COUNTED, false)
        editor.putBoolean(KEY_STREAK_BONUS_GIVEN, false)
        editor.putInt(KEY_REWARD_BANANAS, 0)
        editor.putBoolean(KEY_REWARD_DOUBLED, false)
        editor.putString(KEY_LAST_RESET, today)
        editor.commit()
    }

    /**
     * Si hay escudo disponible y el día no fue ya protegido: consume 1 y conserva la racha.
     * Si no: rompe la racha según reglas actuales.
     */
    private fun applyShieldOrBreakStreak(editor: SharedPreferences.Editor, protectedKey: String) {
        val alreadyProtected = prefs.getString(KEY_LAST_SHIELD_PROTECTED_DATE, "") == protectedKey
        if (alreadyProtected) {
            // Idempotencia: ya se protegió este día; no romper ni re-consumir
            editor.putBoolean(KEY_STREAK_BROKEN, false)
            return
        }

        val currentShields = prefs.getInt(KEY_SHIELDS_COUNT, 0)
        if (currentShields > 0) {
            editor.putInt(KEY_SHIELDS_COUNT, currentShields - 1)
            editor.putString(KEY_LAST_SHIELD_PROTECTED_DATE, protectedKey)
            editor.putBoolean(KEY_STREAK_BROKEN, false)
            editor.putBoolean(KEY_PENDING_SHIELD_USED_MESSAGE, true)
            // streakCount intacto; missedDays no incrementa
        } else {
            editor.putInt(KEY_STREAK, 0)
            editor.putBoolean(KEY_STREAK_BROKEN, true)
            editor.putInt(KEY_MISSED_DAYS, missedDaysCount + 1)
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
                // Loot del cofre: 1 a 3 bananas al azar (+ bono de hito x7)
                val chestBananas = (1..3).random() + bonusBananas
                prefs.edit()
                    .putInt(KEY_BANANAS, bananas + chestBananas)
                    .putInt(KEY_REWARD_BANANAS, chestBananas)
                    .putInt(KEY_STREAK, newStreak)
                    .putBoolean(KEY_REWARD_GIVEN, true)
                    .putBoolean(KEY_STREAK_COUNTED, true)
                    .putBoolean(KEY_STREAK_BROKEN, false)
                    .putInt(KEY_MISSED_DAYS, 0)
                    .putBoolean(KEY_STREAK_BONUS_GIVEN, isMilestone)
                    .commit()
                claimShieldMilestonesIfNeeded(newStreak)
                true
            }
            !newState && rewardAlreadyGiven -> {
                // Revierte exactamente lo que pagó el cofre de hoy
                val awarded = prefs.getInt(KEY_REWARD_BANANAS, 1)
                prefs.edit()
                    .putInt(KEY_BANANAS, maxOf(0, bananas - awarded))
                    .putInt(KEY_STREAK, maxOf(0, streakCount - 1))
                    .putInt(KEY_REWARD_BANANAS, 0)
                    .putBoolean(KEY_REWARD_GIVEN, false)
                    .putBoolean(KEY_STREAK_COUNTED, false)
                    .putBoolean(KEY_STREAK_BONUS_GIVEN, false)
                    .apply()
                false
            }
            else -> false
        }
    }

    /** Bananas pagadas por el cofre de hoy (0 si aún no se completó el día). */
    val lastRewardBananas: Int
        get() = prefs.getInt(KEY_REWARD_BANANAS, 0)

    val rewardDoubledToday: Boolean
        get() = prefs.getBoolean(KEY_REWARD_DOUBLED, false)

    /** Duplica el loot del cofre de hoy (+X bananas extra). */
    fun doubleChestReward(): Boolean {
        if (!prefs.getBoolean(KEY_REWARD_GIVEN, false)) return false
        if (prefs.getBoolean(KEY_REWARD_DOUBLED, false)) return false
        val awarded = prefs.getInt(KEY_REWARD_BANANAS, 0)
        if (awarded <= 0) return false
        prefs.edit()
            .putInt(KEY_BANANAS, bananas + awarded)
            .putInt(KEY_REWARD_BANANAS, awarded * 2)
            .putBoolean(KEY_REWARD_DOUBLED, true)
            .apply()
        return true
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

    /** Compra 1 Escudo de Pulcritud en la tienda (100 bananas, máx. MAX_SHIELDS). */
    fun buyShield(): Boolean {
        if (bananas < SHIELD_SHOP_PRICE) return false
        if (shieldsCount >= MAX_SHIELDS) return false
        prefs.edit()
            .putInt(KEY_BANANAS, bananas - SHIELD_SHOP_PRICE)
            .putInt(KEY_SHIELDS_COUNT, shieldsCount + 1)
            .commit()
        return true
    }

    fun useAccessory(accessoryId: String) {
        prefs.edit().putString(KEY_EQUIPPED_ACCESSORY, accessoryId).apply()
    }

    // ─── Hint tienda (primera vez que alcanza el accesorio más barato) ─────────

    fun shouldShowShopAffordHint(): Boolean {
        if (!onboardingCompleted || shopAffordHintConsumed) return false
        return canAffordCheapestUnownedAccessory()
    }

    fun consumeShopAffordHint() {
        prefs.edit().putBoolean(KEY_SHOP_AFFORD_HINT_CONSUMED, true).apply()
    }

    private val shopAffordHintConsumed: Boolean
        get() = prefs.getBoolean(KEY_SHOP_AFFORD_HINT_CONSUMED, false)

    private fun canAffordCheapestUnownedAccessory(): Boolean {
        val cheapest = ACCESSORIES
            .filter { it.id !in ownedAccessories }
            .minByOrNull { it.price }
            ?: return false
        return bananas >= cheapest.price
    }

    // ─── Motas de polvo ────────────────────────────────────────────────────────

    val dustMotes: List<DustMote> get() = loadDustMotes()

    val dustCount: Int get() = dustMotes.size

    /** Sincroniza motas según tiempo transcurrido (máx. 5, +1 cada 2 horas). */
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
            val nextSpawn = lastSpawn + DUST_SPAWN_INTERVAL_MS
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

    // ─── Debug ─────────────────────────────────────────────────────────────────

    val lastResetDate: String get() = prefs.getString(KEY_LAST_RESET, "") ?: ""
    val streakCountedToday: Boolean get() = prefs.getBoolean(KEY_STREAK_COUNTED, false)
    val debugDayOffset: Int get() = prefs.getInt(KEY_DEBUG_DAY_OFFSET, 0)
    val lastShieldProtectedDate: String
        get() = prefs.getString(KEY_LAST_SHIELD_PROTECTED_DATE, "") ?: ""

    /**
     * Avanza un día de juego y ejecuta el reset.
     * @param markPreviousCompleted true = día ganado; false = incompleto (prueba escudos);
     *        null = deja el estado actual de streakCounted/done_*.
     */
    fun debugAdvanceDay(markPreviousCompleted: Boolean? = null) {
        val today = currentGameDate().toString()
        if (lastResetDate.isEmpty()) {
            prefs.edit().putString(KEY_LAST_RESET, today).commit()
        } else if (lastResetDate != today) {
            // Alinear lastReset al día actual antes de marcar y avanzar
            prefs.edit().putString(KEY_LAST_RESET, today).commit()
        }

        when (markPreviousCompleted) {
            true -> prefs.edit()
                .putBoolean(KEY_STREAK_COUNTED, true)
                .putBoolean(KEY_STREAK_BROKEN, false)
                .commit()
            false -> {
                // Día incompleto: quitar conteo y desmarcar tareas de hoy
                val editor = prefs.edit().putBoolean(KEY_STREAK_COUNTED, false)
                todayTasks.forEach { editor.putBoolean(taskKey(it.id), false) }
                editor.commit()
            }
            null -> Unit
        }

        prefs.edit()
            .putInt(KEY_DEBUG_DAY_OFFSET, debugDayOffset + 1)
            .commit()
        checkAndResetForNewDay()
    }

    fun debugAddShields(delta: Int) {
        val next = (shieldsCount + delta).coerceIn(0, MAX_SHIELDS)
        prefs.edit().putInt(KEY_SHIELDS_COUNT, next).commit()
    }

    fun debugSetStreak(value: Int) {
        prefs.edit().putInt(KEY_STREAK, value.coerceAtLeast(0)).commit()
    }

    fun debugAddBananas(amount: Int) {
        prefs.edit().putInt(KEY_BANANAS, maxOf(0, bananas + amount)).commit()
    }

    /** Retrocede el reloj de motas para forzar spawn al sincronizar. */
    fun debugAdvanceDustHours(hours: Int) {
        val last = prefs.getLong(KEY_DUST_LAST_SPAWN_MS, currentTimeMs())
        val shifted = last - hours * 3_600_000L
        prefs.edit().putLong(KEY_DUST_LAST_SPAWN_MS, shifted).commit()
        syncDustSpawns()
    }

    fun debugClearDayOffset() {
        prefs.edit().putInt(KEY_DEBUG_DAY_OFFSET, 0).commit()
        checkAndResetForNewDay()
    }

    fun debugResetAllPrefs() {
        prefs.edit().clear().commit()
        ensureShieldsInitialized()
        checkAndResetForNewDay()
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
        const val KEY_REWARD_BANANAS     = "rewardBananasToday"
        const val KEY_REWARD_DOUBLED     = "rewardDoubledToday"
        const val KEY_ONBOARDING_DONE    = "onboardingDone"
        const val KEY_MAIN_TOUR_PENDING  = "mainTourPending"
        const val KEY_SHOP_AFFORD_HINT_CONSUMED = "shopAffordHintConsumed"
        const val KEY_DUST_COUNT         = "dustCount"
        const val KEY_DUST_MOTES         = "dustMotesJson"
        const val KEY_DUST_LAST_SPAWN_MS = "dustLastSpawnMs"
        const val KEY_TASKS_VIEW_MODE    = "tasksViewMode"

        const val KEY_SHIELDS_COUNT = "shieldsCount"
        const val KEY_SHIELDS_INITIALIZED = "shieldsInitialized"
        const val KEY_SHIELD_MILESTONES_CLAIMED = "shieldMilestonesClaimed"
        const val KEY_LAST_SHIELD_PROTECTED_DATE = "lastShieldProtectedDate"
        const val KEY_PENDING_SHIELD_USED_MESSAGE = "pendingShieldUsedMessage"
        const val KEY_DEBUG_DAY_OFFSET = "debugDayOffset"

        const val VIEW_MODE_TODAY = "today"
        const val VIEW_MODE_WEEK  = "week"

        const val MAX_DUST_MOTES = 5
        const val DUST_SPAWN_INTERVAL_MS = 7_200_000L // 2 horas

        const val MAX_SHIELDS = 3
        const val INITIAL_SHIELDS = 3
        const val SHIELD_SHOP_PRICE = 100

        /** Hitos one-shot de racha → escudos. Independiente del bonus banana % 7. */
        val SHIELD_MILESTONES: Map<Int, Int> = linkedMapOf(
            7 to 1,
            30 to 1,
            60 to 1,
            90 to 2,
            180 to 2,
            365 to 3,
        )

        data class AccessoryItem(val id: String, val name: String, val price: Int)

        val ACCESSORIES = listOf(
            AccessoryItem("glasses",   "Lentes",      5),
            AccessoryItem("hat",       "Gorro",      12),
            AccessoryItem("chaleco",   "Chaleco",    20),
            AccessoryItem("crown",     "Corona",     30),
            AccessoryItem("payaso",    "Payaso",     40),
            AccessoryItem("astronaut", "Astronauta", 60)
        )
    }
}
