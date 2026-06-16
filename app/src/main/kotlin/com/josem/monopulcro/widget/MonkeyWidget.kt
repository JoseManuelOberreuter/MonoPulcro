package com.josem.monopulcro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.josem.monopulcro.MainActivity
import com.josem.monopulcro.R
import com.josem.monopulcro.data.MonkeyStateManager

class MonkeyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = MonkeyStateManager(context)

        val streak       = manager.streakCount
        val hasGlasses   = manager.hasGlasses
        val isClean      = manager.isCleanToday
        val streakBroken = manager.streakBroken
        val missedDays   = manager.missedDaysCount
        val imageRes     = resolveMonkeyImage(isClean, hasGlasses, streakBroken, missedDays)

        provideContent {
            WidgetContent(streak = streak, isClean = isClean, monkeyImageRes = imageRes)
        }
    }

    @Composable
    private fun WidgetContent(
        streak: Int,
        isClean: Boolean,
        monkeyImageRes: Int
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(android.graphics.Color.parseColor("#FFF8F0")))
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(monkeyImageRes),
                contentDescription = "Estado del mono",
                modifier = GlanceModifier.size(72.dp)
            )
            Spacer(GlanceModifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔥", style = TextStyle(fontSize = 16.sp))
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = "$streak días",
                    style = TextStyle(
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = ColorProvider(android.graphics.Color.parseColor("#FF6D00"))
                    )
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = if (isClean) "¡Limpio! ✨" else "Tareas pendientes",
                style = TextStyle(
                    fontSize = 11.sp,
                    color    = ColorProvider(
                        if (isClean) android.graphics.Color.parseColor("#388E3C")
                        else         android.graphics.Color.parseColor("#9E6B00")
                    )
                )
            )
        }
    }

    private fun resolveMonkeyImage(
        isClean: Boolean,
        hasGlasses: Boolean,
        streakBroken: Boolean = false,
        missedDays: Int = 0
    ): Int = when {
        isClean && hasGlasses -> R.drawable.mono_cool
        isClean               -> R.drawable.mono_pulcro
        missedDays >= 3       -> R.drawable.mono_sucio_3
        streakBroken          -> R.drawable.mono_sucio_2
        hasGlasses            -> R.drawable.mono_sucio_2
        else                  -> R.drawable.mono_sucio_1
    }
}
