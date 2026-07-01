package com.josem.monopulcro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import com.josem.monopulcro.data.DustMote
import com.josem.monopulcro.ui.MonkeyImageResolver
import kotlin.math.roundToInt

private val WidgetBackground = widgetColor(0xFFFFF8F0)
private val StreakOrange     = widgetColor(0xFFFF6D00)
private val CleanGreen       = widgetColor(0xFF4CAF50)
private val DirtyBrown       = widgetColor(0xFF9E6B00)

private const val WIDGET_MONKEY_DP = 72f
private const val MAIN_MONKEY_DP = 240f

private fun widgetColor(argb: Long) = ColorProvider(Color(argb))

class MonkeyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = com.josem.monopulcro.data.MonkeyStateManager(context)
        manager.syncDustSpawns()

        val streak            = manager.streakCount
        val equippedAccessory = manager.equippedAccessory
        val isClean           = manager.isCleanToday
        val streakBroken      = manager.streakBroken
        val missedDays        = manager.missedDaysCount
        val dustMotes         = manager.dustMotes
        val imageRes          = MonkeyImageResolver.resolve(isClean, equippedAccessory, streakBroken, missedDays)

        provideContent {
            WidgetContent(
                streak = streak,
                isClean = isClean,
                monkeyImageRes = imageRes,
                dustMotes = dustMotes
            )
        }
    }

    @Composable
    private fun WidgetContent(
        streak: Int,
        isClean: Boolean,
        monkeyImageRes: Int,
        dustMotes: List<DustMote>
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetBackground)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier.size(WIDGET_MONKEY_DP.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(monkeyImageRes),
                    contentDescription = "Estado del mono",
                    modifier = GlanceModifier.size(WIDGET_MONKEY_DP.dp)
                )
                dustMotes.forEach { mote ->
                    WidgetDustMote(mote)
                }
            }
            Spacer(GlanceModifier.height(8.dp))
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
                        color      = StreakOrange
                    )
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = if (isClean) "Pulcro!" else "Tareas pendientes",
                style = TextStyle(
                    fontSize = 11.sp,
                    color    = if (isClean) CleanGreen else DirtyBrown
                )
            )
        }
    }
}

@Composable
private fun WidgetDustMote(mote: DustMote) {
    val scale = WIDGET_MONKEY_DP / MAIN_MONKEY_DP
    val sizeDp = (mote.sizeDp * scale).coerceAtLeast(10f)
    val half = sizeDp / 2f
    val startDp = (WIDGET_MONKEY_DP * mote.xFrac - half)
        .coerceIn(0f, WIDGET_MONKEY_DP - sizeDp)
    val topDp = (WIDGET_MONKEY_DP * mote.yFrac - half)
        .coerceIn(0f, WIDGET_MONKEY_DP - sizeDp)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        Image(
            provider = ImageProvider(R.drawable.mota_polvo),
            contentDescription = null,
            modifier = GlanceModifier
                .size(sizeDp.dp)
                .padding(
                    start = startDp.roundToInt().dp,
                    top = topDp.roundToInt().dp
                )
        )
    }
}
