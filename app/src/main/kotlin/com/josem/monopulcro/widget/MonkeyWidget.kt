package com.josem.monopulcro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.josem.monopulcro.MainActivity
import com.josem.monopulcro.R
import com.josem.monopulcro.data.DustMote
import com.josem.monopulcro.ui.MonkeyImageResolver
import kotlin.math.min
import kotlin.math.roundToInt

private val WidgetBackground = widgetColor(0xFFFFF8F0)
private val StreakOrange     = widgetColor(0xFFFF6D00)

private const val MAIN_MONKEY_DP = 240f
private const val BASE_MONKEY_DP = 72f
private const val MAX_MONKEY_DP = 128f

private val SIZE_COMPACT     = DpSize(180.dp, 140.dp)
private val SIZE_WIDE        = DpSize(260.dp, 140.dp)
private val SIZE_TALL        = DpSize(180.dp, 260.dp)
private val SIZE_LARGE       = DpSize(260.dp, 260.dp)

private fun widgetColor(argb: Long) = ColorProvider(Color(argb))

private data class WidgetLayoutMetrics(
    val padding: Dp,
    val monkeyDp: Float,
    val fireSize: Dp,
    val streakSize: TextUnit,
    val monkeyToStreakGap: Dp,
    val fireToNumberGap: Dp,
)

/** Escala mono + racha según el espacio útil; mismo layout minimalista en todos los tamaños. */
private fun widgetLayoutMetrics(width: Dp, height: Dp): WidgetLayoutMetrics {
    val pad = 12f
    val contentMin = min(width.value, height.value) - pad * 2
    val monkey = (contentMin * 0.58f).coerceIn(BASE_MONKEY_DP, MAX_MONKEY_DP)
    val scale = monkey / BASE_MONKEY_DP
    return WidgetLayoutMetrics(
        padding = pad.dp,
        monkeyDp = monkey,
        fireSize = (18f * scale).dp,
        streakSize = (16f * scale).sp,
        monkeyToStreakGap = (8f * scale).dp,
        fireToNumberGap = (4f * scale).dp,
    )
}

class MonkeyWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_COMPACT, SIZE_WIDE, SIZE_TALL, SIZE_LARGE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = com.josem.monopulcro.data.MonkeyStateManager(context)
        manager.checkAndResetForNewDay()
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
                monkeyImageRes = imageRes,
                dustMotes = dustMotes
            )
        }
    }

    @Composable
    private fun WidgetContent(
        streak: Int,
        monkeyImageRes: Int,
        dustMotes: List<DustMote>
    ) {
        val metrics = widgetLayoutMetrics(LocalSize.current.width, LocalSize.current.height)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetBackground)
                .clickable(actionStartActivity<MainActivity>())
                .padding(metrics.padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier.size(metrics.monkeyDp.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(monkeyImageRes),
                    contentDescription = "Estado del mono",
                    modifier = GlanceModifier.size(metrics.monkeyDp.dp)
                )
                dustMotes.forEach { mote ->
                    WidgetDustMote(mote, metrics.monkeyDp)
                }
            }
            Spacer(GlanceModifier.height(metrics.monkeyToStreakGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.fuego),
                    contentDescription = null,
                    modifier = GlanceModifier.size(metrics.fireSize)
                )
                Spacer(GlanceModifier.width(metrics.fireToNumberGap))
                Text(
                    text = "$streak",
                    style = TextStyle(
                        fontSize   = metrics.streakSize,
                        fontWeight = FontWeight.Bold,
                        color      = StreakOrange
                    )
                )
            }
        }
    }
}

@Composable
private fun WidgetDustMote(mote: DustMote, monkeyDp: Float) {
    val scale = monkeyDp / MAIN_MONKEY_DP
    val sizeDp = (mote.sizeDp * scale).coerceAtLeast(10f)
    val half = sizeDp / 2f
    val startDp = (monkeyDp * mote.xFrac - half)
        .coerceIn(0f, monkeyDp - sizeDp)
    val topDp = (monkeyDp * mote.yFrac - half)
        .coerceIn(0f, monkeyDp - sizeDp)

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
