package com.josem.monopulcro.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.josem.monopulcro.data.MonkeyStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WIDGET_HOURLY_UPDATE) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val glanceManager = GlanceAppWidgetManager(appContext)
                val glanceIds = glanceManager.getGlanceIds(MonkeyWidget::class.java)
                if (glanceIds.isEmpty()) {
                    WidgetUpdateScheduler.cancel(appContext)
                    return@launch
                }

                val manager = MonkeyStateManager(appContext)
                manager.checkAndResetForNewDay()
                manager.syncDustSpawns()

                val widget = MonkeyWidget()
                glanceIds.forEach { id -> widget.update(appContext, id) }

                WidgetUpdateScheduler.schedule(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_HOURLY_UPDATE = "com.josem.monopulcro.WIDGET_HOURLY_UPDATE"
    }
}
