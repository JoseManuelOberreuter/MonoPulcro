package com.josem.monopulcro.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Punto único de acceso a Firebase Analytics. Ver docs/eventos_firebase.md
 * para el catálogo de eventos y su justificación.
 */
object AnalyticsLogger {

    object Events {
        const val ONBOARDING_START = "onboarding_start"
        const val ONBOARDING_COMPLETE = "onboarding_complete"
        const val TASK_CREATED = "task_created"
        const val TASK_COMPLETED = "task_completed"
        const val FIRST_TASK_COMPLETED = "first_task_completed"
        const val CHEST_OPENED = "chest_opened"
        const val COSMETIC_UNLOCKED = "cosmetic_unlocked"
        const val STORE_OPENED = "store_opened"
        const val PURCHASE_STARTED = "purchase_started"
    }

    object Params {
        const val DAYS_COUNT = "days_count"
        const val TASK_ID = "task_id"
        const val BANANAS = "bananas"
        const val ACCESSORY_ID = "accessory_id"
        const val PRICE = "price"
        const val SOURCE = "source"
        const val PRODUCT_ID = "product_id"
    }

    private const val PREFS_NAME = "analytics_prefs"
    private const val KEY_FIRST_TASK_LOGGED = "first_task_completed_logged"

    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        firebaseAnalytics = FirebaseAnalytics.getInstance(app)
    }

    fun log(event: String, params: Map<String, Any> = emptyMap()) {
        val analytics = firebaseAnalytics ?: return
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        analytics.logEvent(event, bundle)
    }

    /**
     * Loguea [Events.TASK_COMPLETED] siempre, y [Events.FIRST_TASK_COMPLETED]
     * la primera vez que el usuario completa una tarea en la vida de la app
     * (flag propio, independiente de `monkey_prefs`).
     */
    fun logTaskCompleted(taskId: String) {
        log(Events.TASK_COMPLETED, mapOf(Params.TASK_ID to taskId))
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_FIRST_TASK_LOGGED, false)) {
            prefs.edit().putBoolean(KEY_FIRST_TASK_LOGGED, true).apply()
            log(Events.FIRST_TASK_COMPLETED)
        }
    }
}
