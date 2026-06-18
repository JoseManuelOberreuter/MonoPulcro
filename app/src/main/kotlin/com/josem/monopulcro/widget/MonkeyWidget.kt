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

        val streak            = manager.streakCount
        val equippedAccessory = manager.equippedAccessory
        val isClean           = manager.isCleanToday
        val streakBroken      = manager.streakBroken
        val missedDays        = manager.missedDaysCount
        val imageRes          = resolveMonkeyImage(isClean, equippedAccessory, streakBroken, missedDays)

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

            // Racha: ícono fuego + número (sin emoji para evitar crashes en OEM)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.fuego),
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

            // Estado (sin emoji)
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

    private fun resolveMonkeyImage(
        isClean: Boolean,
        equippedAccessory: String?,
        streakBroken: Boolean = false,
        missedDays: Int = 0
    ): Int {
        if (isClean && equippedAccessory == "glasses")   return R.drawable.mono_cool
        if (isClean && equippedAccessory == "hat")       return R.drawable.mono_gorro
        if (isClean && equippedAccessory == "crown")     return R.drawable.mono_corona
        if (isClean && equippedAccessory == "astronaut") return R.drawable.mono_astronauta
        if (isClean) return R.drawable.mono_pulcro
        if (missedDays >= 4) {
            val states = listOf(
                R.drawable.mono_sucio_cansado,
                R.drawable.mono_sucio_enfermo,
                R.drawable.mono_sucio_frustrado,
                R.drawable.mono_sucio_llorando,
            )
            return states[java.util.Random(missedDays.toLong()).nextInt(states.size)]
        }
        if (missedDays == 3) return R.drawable.mono_sucio_3
        if (streakBroken) return R.drawable.mono_sucio_2
        return R.drawable.mono_sucio_1
    }
}
