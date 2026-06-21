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
import com.josem.monopulcro.ui.MonkeyImageResolver

class MonkeyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = com.josem.monopulcro.data.MonkeyStateManager(context)

        val streak            = manager.streakCount
        val equippedAccessory = manager.equippedAccessory
        val isClean           = manager.isCleanToday
        val streakBroken      = manager.streakBroken
        val missedDays        = manager.missedDaysCount
        val imageRes          = MonkeyImageResolver.resolve(isClean, equippedAccessory, streakBroken, missedDays)

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
                Image(
                    provider = ImageProvider(com.josem.monopulcro.R.drawable.fuego),
                    contentDescription = null,
                    modifier = GlanceModifier.size(18.dp)
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = "$streak dias",
                    style = TextStyle(
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = ColorProvider(android.graphics.Color.parseColor("#FF6D00"))
                    )
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = if (isClean) "Pulcro!" else "Tareas pendientes",
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
}
