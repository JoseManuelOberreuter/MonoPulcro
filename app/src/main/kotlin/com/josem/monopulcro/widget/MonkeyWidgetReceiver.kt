package com.josem.monopulcro.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MonkeyWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = MonkeyWidget()

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
