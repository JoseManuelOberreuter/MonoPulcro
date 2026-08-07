package com.josem.monopulcro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.josem.monopulcro.BuildConfig
import com.josem.monopulcro.R
import com.josem.monopulcro.data.Achievement
import com.josem.monopulcro.data.Achievements
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Misma "forma" visual que ShopScreen (TopAppBar + tabs con pill + pager), en paleta cálida. */
private val StreakWaveColor = Color(0xFFFB923C)
private val StreakWaveSoft = Color(0xFFFFEDD5)
private val StreakAccent = Color(0xFFDC2626)

private enum class StreakTab(val title: String) {
    STREAK("Racha"),
    ACHIEVEMENTS("Logros"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakScreen(
    onNavigateBack: () -> Unit,
    vm: MonkeyViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val tabs = StreakTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Racha y logros",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.fuego),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${state.streak}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = StreakAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StreakSegmentedTabs(
                tabs = tabs,
                pagerState = pagerState,
                onSelect = { tab ->
                    scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs[page]) {
                    StreakTab.STREAK -> StreakStatsPage(
                        streak = state.streak,
                        bestStreak = state.bestStreakCount,
                        missedDaysCount = state.missedDaysCount,
                        shieldsCount = state.shieldsCount,
                        maxShields = state.maxShields,
                    )
                    StreakTab.ACHIEVEMENTS -> AchievementsPage(
                        unlockedAchievements = state.unlockedAchievements,
                        onDebugUnlockAll = { vm.debugUnlockAllAchievements() },
                        onDebugReset = { vm.debugResetAchievements() },
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakSegmentedTabs(
    tabs: List<StreakTab>,
    pagerState: PagerState,
    onSelect: (StreakTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val gapPx = with(density) { 4.dp.toPx() }
    val tabCount = tabs.size.coerceAtLeast(1)
    val pillWidthPx = if (trackWidthPx > 0f) {
        (trackWidthPx - gapPx * (tabCount - 1)) / tabCount
    } else 0f

    val pageProgress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, (tabCount - 1).toFloat())
    val pillOffsetPx = pageProgress * (pillWidthPx + gapPx)

    Box(
        modifier = modifier
            .height(44.dp)
            .background(StreakWaveSoft, RoundedCornerShape(14.dp))
            .border(1.5.dp, StreakWaveColor, RoundedCornerShape(14.dp))
            .padding(4.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
    ) {
        if (pillWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(pillOffsetPx.roundToInt(), 0) }
                    .width(with(density) { pillWidthPx.toDp() })
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StreakWaveColor)
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selectedAmount = (1f - abs(pageProgress - index)).coerceIn(0f, 1f)
                val textColor = lerp(Color(0xFF9A3412), Color.White, selectedAmount)
                val weight = FontWeight(
                    (FontWeight.SemiBold.weight +
                        ((FontWeight.Bold.weight - FontWeight.SemiBold.weight) * selectedAmount))
                        .roundToInt()
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(tab) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        fontSize = 15.sp,
                        fontWeight = weight,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakStatsPage(
    streak: Int,
    bestStreak: Int,
    missedDaysCount: Int,
    shieldsCount: Int,
    maxShields: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StreakStatCard(
            icon = R.drawable.fuego,
            title = "Racha actual",
            value = if (streak == 1) "1 día" else "$streak días",
        )
        StreakStatCard(
            icon = R.drawable.fuego,
            title = "Mejor racha histórica",
            value = if (bestStreak == 1) "1 día" else "$bestStreak días",
        )
        StreakStatCard(
            icon = R.drawable.escudo_pulcritud,
            title = "Escudos de Pulcritud",
            value = "$shieldsCount/$maxShields",
        )
        if (missedDaysCount > 0) {
            StreakStatCard(
                icon = R.drawable.fuego,
                title = "Días perdidos seguidos",
                value = "$missedDaysCount",
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StreakStatCard(icon: Int, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StreakWaveSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF7C2D12),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = StreakAccent
        )
    }
}

@Composable
private fun AchievementsPage(
    unlockedAchievements: Set<String>,
    onDebugUnlockAll: () -> Unit,
    onDebugReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "${unlockedAchievements.size} / ${Achievements.ALL.size} desbloqueados",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = Color(0xFF7C2D12),
        )
        if (BuildConfig.DEBUG) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onDebugUnlockAll, modifier = Modifier.weight(1f)) {
                    Text("Desbloquear todos", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onDebugReset, modifier = Modifier.weight(1f)) {
                    Text("Bloquear todos", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(Achievements.ALL, key = { it.id }) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    unlocked = achievement.id in unlockedAchievements,
                )
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement, unlocked: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (unlocked) StreakWaveSoft else Color(0xFFF1F5F9),
                RoundedCornerShape(16.dp),
            )
            .border(
                1.dp,
                if (unlocked) StreakWaveColor else Color(0xFFE2E8F0),
                RoundedCornerShape(16.dp),
            )
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        when {
            !unlocked -> Image(
                painter = painterResource(R.drawable.candado),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            achievement.iconRes != null -> Image(
                painter = painterResource(achievement.iconRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            else -> Text(text = achievement.emoji, fontSize = 34.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = achievement.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (unlocked) Color(0xFF7C2D12) else Color(0xFF94A3B8),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = achievement.description,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = if (unlocked) Color(0xFF9A3412) else Color(0xFF94A3B8),
        )
    }
}
