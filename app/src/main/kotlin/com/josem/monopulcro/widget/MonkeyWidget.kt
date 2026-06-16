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
        // Leemos SharedPreferences directamente; estamos en una coroutine
        val prefs = context.getSharedPreferences(
            MonkeyStateManager.PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val streak = prefs.getInt(MonkeyStateManager.KEY_STREAK, 0)
        val hasHat = prefs.getBoolean(MonkeyStateManager.KEY_HAS_GLASSES, false)
        val isClean = MonkeyStateManager.TASK_KEYS.all { prefs.getBoolean(it, false) }
        val streakBroken = prefs.getBoolean(MonkeyStateManager.KEY_STREAK_BROKEN, false)
        val missedDays = prefs.getInt(MonkeyStateManager.KEY_MISSED_DAYS, 0)

        val monkeyImageRes = resolveMonkeyImage(isClean = isClean, hasHat = hasHat, streakBroken = streakBroken, missedDays = missedDays)

        provideContent {
            WidgetContent(
                streak = streak,
                isClean = isClean,
                monkeyImageRes = monkeyImageRes
            )
        }
    }

    // ─── Contenido visual del widget ──────────────────────────────────────────

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del mono
            Image(
                provider = ImageProvider(monkeyImageRes),
                contentDescription = "Estado del mono",
                modifier = GlanceModifier.size(72.dp)
            )

            Spacer(GlanceModifier.height(8.dp))

            // Racha
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔥",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = "$streak días",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(android.graphics.Color.parseColor("#FF6D00"))
                    )
                )
            }

            Spacer(GlanceModifier.height(4.dp))

            // Estado del mono
            Text(
                text = if (isClean) "¡Limpio! ✨" else "Tareas pendientes",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(
                        if (isClean) android.graphics.Color.parseColor("#388E3C")
                        else android.graphics.Color.parseColor("#9E6B00")
                    )
                )
            )
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private fun resolveMonkeyImage(isClean: Boolean, hasHat: Boolean, streakBroken: Boolean = false, missedDays: Int = 0): Int = when {
        isClean && hasHat -> R.drawable.mono_cool
        isClean           -> R.drawable.mono_pulcro
        missedDays >= 3   -> R.drawable.mono_sucio_3
        streakBroken      -> R.drawable.mono_sucio_2
        hasHat            -> R.drawable.mono_sucio_2
        else              -> R.drawable.mono_sucio_1
    }
}
