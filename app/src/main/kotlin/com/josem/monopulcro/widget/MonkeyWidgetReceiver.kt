package com.josem.monopulcro.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MonkeyWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = MonkeyWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.schedule(context)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdateScheduler.cancel(context)
        super.onDisabled(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdateScheduler.schedule(context)
    }

    companion object {
        /**
         * Llama esto desde el ViewModel cada vez que cambie el estado
         * para que el widget de la pantalla de inicio se refresque al instante.
         */
        fun updateWidget(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(MonkeyWidget::class.java)
                glanceIds.forEach { id ->
                    MonkeyWidget().update(context, id)
                }
            }
        }
    }
}
