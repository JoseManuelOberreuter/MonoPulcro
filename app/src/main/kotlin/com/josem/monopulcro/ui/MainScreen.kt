package com.josem.monopulcro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.josem.monopulcro.R
import com.josem.monopulcro.audio.SoundManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val WaveColor = Color(0xFF7DD3FC)
private val TaskRowHeight = 72.dp
private val HeaderIconSize = 52.dp

private val taskMoveSpec = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

private val TIPS_PHRASES = listOf(
    "7 días seguidos te dan 3 bananas extra",
    "Un hogar limpio reduce el estrés en un 30%",
    "Limpiar 15 min al día evita limpiezas de 3 horas",
    "La cocina limpia después de cocinar evita plagas",
    "Completa todas las tareas hoy y suma a tu racha",
    "Los baños deben limpiarse al menos 1 vez por semana",
    "Una cama tendida mejora tu productividad del día",
    "Saca la basura antes de que huela: cada 2-3 días",
    "Los gérmenes de la cocina superan a los del baño",
    "Un espacio ordenado = una mente más clara",
    "Pequeñas tareas diarias construyen grandes hábitos",
    "Cuando el mono está sucio, no le gusta usar sus accesorios"

)

private val SUCIO1_PHRASES = listOf(
    "Hoy no fue, mañana sí puede ser",
    "El mono te está esperando, no lo decepciones!",
    "Un día perdido no arruina un buen hábito",
    "Tu mono necesita un baño, completa las tareas",
    "La limpieza es un hábito, los hábitos se construyen",
    "Cuando el mono está sucio, no le gusta usar sus accesorios",
    "7 días seguidos te dan 3 bananas extra"
)

private val SUCIO2_PHRASES = listOf(
    "Tu mono te extraña, dale una razón para sonreír",
    "El mono lleva días triste, hoy puedes cambiarlo",
    "Un pequeño esfuerzo hoy alegra al mono mañana",
    "Míralo, está esperando que vuelvas. Tú puedes!",
    "El mono confía en ti, no lo dejes más tiempo así",
    "Cuando el mono está sucio, no le gusta usar sus accesorios"

)

@Composable
fun MainScreen(
    onAddTask: () -> Unit,
    onEditTask: (taskId: String) -> Unit,
    onOpenShop: () -> Unit,
    vm: MonkeyViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var celebration by remember { mutableStateOf<StreakCelebration?>(null) }
    var isMonkeyCleaning by remember { mutableStateOf(false) }
    var dustAtCleanStart by remember { mutableStateOf(emptyList<com.josem.monopulcro.data.DustMote>()) }
    var showDustBananaReward by remember { mutableStateOf(false) }
    var selectedDayOfWeek by remember { mutableStateOf<Int?>(null) }
    val selectedWeekDay = selectedDayOfWeek?.let { dow ->
        state.weekDays.find { it.dayOfWeek == dow }
    }
    val context = LocalContext.current
    val sounds  = remember { SoundManager.get(context) }
    val tipIndex    = remember { TIPS_PHRASES.indices.random() }
    val sucio1Index = remember { SUCIO1_PHRASES.indices.random() }
    val sucio2Index = remember { SUCIO2_PHRASES.indices.random() }
    val monkeyInteractionSource = remember { MutableInteractionSource() }
    val isMonkeyPressed by monkeyInteractionSource.collectIsPressedAsState()
    val monkeyPressScale by animateFloatAsState(
        targetValue = if (isMonkeyPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "monkeyPressScale"
    )
    // En pantallas bajas el mono cede espacio a la lista; en el resto se mantiene igual.
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val monkeyBoxSize = if (screenHeightDp < 700) 168.dp else 240.dp
    val monkeyImageSize = if (screenHeightDp < 700) 154.dp else 220.dp

    val showTour = state.showMainTour
    var tourStep by remember { mutableIntStateOf(0) }
    val tourBounds = remember { mutableStateMapOf<MainTourStep, Rect>() }
    val tourScrollY = remember { mutableStateMapOf<MainTourStep, Float>() }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(showTour) {
        if (showTour) tourStep = 0
    }

    LaunchedEffect(showTour, tourStep) {
        if (!showTour) return@LaunchedEffect
        val step = MainTourStep.entries.getOrNull(tourStep) ?: return@LaunchedEffect
        delay(50)
        val targetY = tourScrollY[step] ?: return@LaunchedEffect
        val offsetPx = with(density) { 72.dp.toPx() }
        scrollState.animateScrollTo((targetY - offsetPx).coerceAtLeast(0f).toInt())
    }

    // Refresca el estado al volver a esta pantalla (ej. después de editar/borrar una tarea)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.celebration) {
        state.celebration?.let {
            celebration = it
            vm.consumeCelebration()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawBottomWaves(WaveColor) }

        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Encabezado ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.mainTourAnchor(
                            MainTourStep.BANANAS,
                            tourBounds,
                            tourScrollY
                        )
                    ) {
                        BananaCounter(count = state.bananas)
                    }
                    Box(
                        modifier = Modifier.mainTourAnchor(
                            MainTourStep.SHOP,
                            tourBounds,
                            tourScrollY
                        )
                    ) {
                        ShopButton(
                            onClick = {
                                if (showTour) return@ShopButton
                                if (state.showShopAffordHint) vm.onShopOpened()
                                onOpenShop()
                            },
                            highlighted = state.showShopAffordHint && !showTour
                        )
                    }
                    Box(
                        modifier = Modifier.mainTourAnchor(
                            MainTourStep.STREAK,
                            tourBounds,
                            tourScrollY
                        )
                    ) {
                        StreakCounter(streak = state.streak)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Imagen del mono ────────────────────────────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(monkeyBoxSize)
                        .mainTourAnchor(MainTourStep.MONKEY, tourBounds, tourScrollY)
                        .clickable(
                            interactionSource = monkeyInteractionSource,
                            indication = null,
                            enabled = !isMonkeyCleaning && !showTour,
                            onClick = {
                                dustAtCleanStart = vm.dustMotesForCleaning()
                                isMonkeyCleaning = true
                                sounds.playCleaningSequence()
                            }
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.graphicsLayer {
                            scaleX = monkeyPressScale
                            scaleY = monkeyPressScale
                        }
                    ) {
                        Image(
                            painter = painterResource(
                                id = MonkeyImageResolver.resolve(
                                    state.isCleanToday,
                                    state.equippedAccessory,
                                    state.streakBroken,
                                    state.missedDaysCount
                                )
                            ),
                            contentDescription = "Mono Pulcro",
                            modifier = Modifier.size(monkeyImageSize)
                        )
                        if (!isMonkeyCleaning && state.dustMotes.isNotEmpty()) {
                            DustMotesOverlay(motes = state.dustMotes)
                        }
                    }
                    if (isMonkeyCleaning) {
                        MonkeyCleaningOverlay(
                            dustMotesAtStart = dustAtCleanStart,
                            onCleaningFinished = {
                                isMonkeyCleaning = false
                                if (dustAtCleanStart.isNotEmpty()) {
                                    vm.completeDustCleaning()
                                    showDustBananaReward = true
                                }
                            }
                        )
                    }
                }

                Text(
                    text = when {
                        state.allTasks.isEmpty()                          -> "¡Agrega tareas para empezar!"
                        state.todayTasks.isEmpty()                        -> "¡Hoy es día de descanso!"
                        state.isCleanToday                                -> TIPS_PHRASES[tipIndex]
                        state.missedDaysCount >= 2 || state.streakBroken  -> SUCIO2_PHRASES[sucio2Index]
                        state.missedDaysCount == 1                        -> SUCIO1_PHRASES[sucio1Index]
                        else                                              -> "Hay tareas pendientes..."
                    },
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = when {
                        state.allTasks.isEmpty()                          -> Color(0xFF7C3AED)
                        state.todayTasks.isEmpty()                        -> Color(0xFF0369A1)
                        state.isCleanToday                                -> Color(0xFF16A34A)
                        state.missedDaysCount >= 2 || state.streakBroken  -> Color(0xFFB91C1C)
                        state.missedDaysCount == 1                        -> Color(0xFF92400E)
                        else                                              -> Color(0xFF92400E)
                    },
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Sección tareas ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.tasksViewMode == TasksViewMode.WEEK)
                            "Esta semana" else "Tareas de hoy",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.allTasks.isNotEmpty() || showTour) {
                            Box(
                                modifier = Modifier.mainTourAnchor(
                                    MainTourStep.VIEW_MODE,
                                    tourBounds,
                                    tourScrollY
                                )
                            ) {
                                TasksViewModeToggle(
                                    mode = state.tasksViewMode,
                                    enabled = !showTour,
                                    onModeChange = { vm.setTasksViewMode(it) }
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .mainTourAnchor(MainTourStep.ADD_TASK, tourBounds, tourScrollY)
                                .background(WaveColor, RoundedCornerShape(10.dp))
                                .clickable(enabled = !showTour) { onAddTask() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar tarea",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .mainTourAnchor(MainTourStep.TASKS, tourBounds, tourScrollY)
                ) {
                    if (state.allTasks.isEmpty()) {
                        EmptyTasksHint(onAddTask = { if (!showTour) onAddTask() })
                    } else {
                        AnimatedContent(
                            targetState = state.tasksViewMode,
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(160))
                            },
                            label = "tasksViewMode"
                        ) { mode ->
                            when (mode) {
                                TasksViewMode.TODAY -> {
                                    if (state.todayTasks.isEmpty()) {
                                        RestDayCard()
                                    } else {
                                        AnimatedTaskList(
                                            tasks = state.todayTasks,
                                            onToggle = { if (!showTour) vm.toggleTask(it) },
                                            onEdit = { if (!showTour) onEditTask(it) }
                                        )
                                    }
                                }
                                TasksViewMode.WEEK -> {
                                    WeekCalendarView(
                                        days = state.weekDays,
                                        onDayClick = { day ->
                                            if (!showTour) selectedDayOfWeek = day.dayOfWeek
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        selectedWeekDay?.let { day ->
            WeekDayDetailSheet(
                day = day,
                onDismiss = { selectedDayOfWeek = null },
                onToggle = { if (!showTour) vm.toggleTask(it) },
                onEdit = { taskId ->
                    selectedDayOfWeek = null
                    if (!showTour) onEditTask(taskId)
                }
            )
        }

        celebration?.let { event ->
            StreakCelebrationOverlay(
                event = event,
                onFinished = { celebration = null }
            )
        }
        if (showDustBananaReward) {
            BananaRewardOverlay(onFinished = { showDustBananaReward = false })
        }
        if (showTour) {
            BackHandler { /* bloquea navegación atrás durante el tour */ }
            val currentTourStep = MainTourStep.entries.getOrNull(tourStep)
            MainScreenTourOverlay(
                stepIndex = tourStep,
                targetBounds = currentTourStep?.let { tourBounds[it] },
                onNext = { tourStep++ },
                onFinish = { vm.completeMainTour() }
            )
        }
    }
}

private fun Modifier.mainTourAnchor(
    step: MainTourStep,
    bounds: MutableMap<MainTourStep, Rect>,
    scrollY: MutableMap<MainTourStep, Float>,
): Modifier = onGloballyPositioned { coordinates ->
    bounds[step] = coordinates.boundsInWindow()
    scrollY[step] = coordinates.positionInParent().y
}

// ─── Estados vacíos ───────────────────────────────────────────────────────────

@Composable
private fun EmptyTasksHint(onAddTask: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F9FF), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(MonkeyImageResolver.DEFAULT_PULCRO),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Aún no tienes tareas. ¡Agrega la primera!",
            fontSize = 14.sp,
            color = Color(0xFF0369A1),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        TextButton(onClick = onAddTask) {
            Text("+ Agregar tarea", color = Color(0xFF0EA5E9), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RestDayCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0FDF4), RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay tareas programadas para hoy. ¡Disfruta tu descanso!",
            fontSize = 14.sp,
            color = Color(0xFF16A34A),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TasksViewModeToggle(
    mode: TasksViewMode,
    enabled: Boolean,
    onModeChange: (TasksViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ViewModeIconButton(
            selected = mode == TasksViewMode.TODAY,
            enabled = enabled,
            icon = Icons.Default.Menu,
            contentDescription = "Vista lista",
            onClick = { onModeChange(TasksViewMode.TODAY) }
        )
        ViewModeIconButton(
            selected = mode == TasksViewMode.WEEK,
            enabled = enabled,
            icon = Icons.Default.DateRange,
            contentDescription = "Vista semanal",
            onClick = { onModeChange(TasksViewMode.WEEK) }
        )
    }
}

@Composable
private fun ViewModeIconButton(
    selected: Boolean,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color = if (selected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Color(0xFF0EA5E9) else Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun WeekCalendarView(
    days: List<WeekDayUi>,
    onDayClick: (WeekDayUi) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            WeekDayRow(day = day, onClick = { onDayClick(day) })
        }
    }
}

@Composable
private fun WeekDayRow(
    day: WeekDayUi,
    onClick: () -> Unit
) {
    val bg = when {
        day.isToday && day.allDone -> Color(0xFFDCFCE7)
        day.isToday -> Color(0xFFE0F2FE)
        day.isRestDay -> Color(0xFFF8FAFC)
        day.allDone -> Color(0xFFF0FDF4)
        else -> Color(0xFFF8FAFC)
    }
    val borderColor = when {
        day.isToday -> WaveColor
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(14.dp))
            .then(
                if (day.isToday) Modifier.border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp)
        ) {
            Text(
                text = day.shortLabel,
                fontSize = 12.sp,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                color = if (day.isToday) Color(0xFF0369A1) else Color(0xFF64748B)
            )
            Text(
                text = "${day.dayOfMonth}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (day.isToday) Color(0xFF0EA5E9) else Color(0xFF1E293B)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            when {
                day.isRestDay -> {
                    Text(
                        text = "Descanso",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }
                else -> {
                    if (day.isToday) {
                        Text(
                            text = "${day.doneCount}/${day.totalCount} completadas",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (day.allDone) Color(0xFF16A34A) else Color(0xFF0369A1)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        WeekDayProgressBar(
                            progress = day.doneCount.toFloat() / day.totalCount.toFloat(),
                            done = day.allDone
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    } else {
                        Text(
                            text = "${day.totalCount} tarea${if (day.totalCount == 1) "" else "s"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    day.previewTitles.forEach { title ->
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (day.totalCount > 2) {
                        Text(
                            text = "+${day.totalCount - 2} más",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        if (day.isToday && day.allDone) {
            Text("✓", fontSize = 18.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
        } else if (day.isToday) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF0EA5E9), CircleShape)
            )
        }
    }
}

@Composable
private fun WeekDayProgressBar(progress: Float, done: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .background(
                    if (done) Color(0xFF16A34A) else WaveColor,
                    RoundedCornerShape(2.dp)
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekDayDetailSheet(
    day: WeekDayUi,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    onEdit: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BackHandler(onBack = onDismiss)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (day.isToday) "Hoy · ${day.shortLabel} ${day.dayOfMonth}"
                       else "${day.shortLabel} ${day.dayOfMonth}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    day.isRestDay -> "Día de descanso"
                    day.isToday -> "${day.doneCount} de ${day.totalCount} completadas"
                    else -> "${day.totalCount} tarea${if (day.totalCount == 1) "" else "s"} programada${if (day.totalCount == 1) "" else "s"}"
                },
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (day.isRestDay) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0FDF4), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "No hay tareas este día. ¡Aprovecha para descansar!",
                        fontSize = 14.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (day.isToday) {
                day.tasks.forEach { taskState ->
                    TaskCheckboxRow(
                        taskName = taskState.task.name,
                        isChecked = taskState.isCompleted,
                        onChecked = { onToggle(taskState.task.id) },
                        onEdit = { onEdit(taskState.task.id) },
                        modifier = Modifier.height(TaskRowHeight)
                    )
                }
            } else {
                day.tasks.forEach { taskState ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = taskState.task.name,
                            fontSize = 16.sp,
                            color = Color(0xFF334155),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onEdit(taskState.task.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Solo puedes marcar tareas el día de hoy",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

// ─── Lista animada de tareas ──────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedTaskList(
    tasks: List<TaskUiState>,
    onToggle: (String) -> Unit,
    onEdit: (String) -> Unit
) {
    val sortedTasks = tasks.sortedBy { it.isCompleted }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(TaskRowHeight * sortedTasks.size),
        userScrollEnabled = false
    ) {
        items(
            items = sortedTasks,
            key = { it.task.id }
        ) { taskState ->
            TaskCheckboxRow(
                modifier = Modifier
                    .height(TaskRowHeight)
                    .animateItemPlacement(animationSpec = taskMoveSpec),
                taskName  = taskState.task.name,
                isChecked = taskState.isCompleted,
                onChecked = { onToggle(taskState.task.id) },
                onEdit    = { onEdit(taskState.task.id) }
            )
        }
    }
}

// ─── Fila de tarea ────────────────────────────────────────────────────────────

@Composable
private fun TaskCheckboxRow(
    taskName: String,
    isChecked: Boolean,
    onChecked: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isChecked) Color(0xFFDCFCE7) else Color(0xFFF8FAFC),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "taskRowBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isChecked) Color(0xFF15803D) else Color(0xFF334155),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "taskRowText"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked  = isChecked,
            onCheckedChange = { onChecked() },
            colors = CheckboxDefaults.colors(
                checkedColor   = Color(0xFF16A34A),
                uncheckedColor = Color(0xFFCBD5E1)
            )
        )
        Text(
            text = taskName,
            fontSize = 16.sp,
            color  = textColor,
            fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier
                .weight(1f)
                .clickable { onChecked() }
                .padding(vertical = 4.dp)
        )
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editar",
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Partículas de fuego ──────────────────────────────────────────────────────

private data class FireParticle(
    val xFrac: Float,
    val sizeDp: Float,
    val delayMs: Long,
    val durationMs: Int
)

private val FIRE_PARTICLES = listOf(
    FireParticle(xFrac = 0.05f, sizeDp = 90f,  delayMs = 0L,   durationMs = 950),
    FireParticle(xFrac = 0.20f, sizeDp = 115f, delayMs = 110L, durationMs = 1050),
    FireParticle(xFrac = 0.35f, sizeDp = 135f, delayMs = 55L,  durationMs = 1000),
    FireParticle(xFrac = 0.52f, sizeDp = 105f, delayMs = 190L, durationMs = 920),
    FireParticle(xFrac = 0.66f, sizeDp = 125f, delayMs = 75L,  durationMs = 1080),
    FireParticle(xFrac = 0.80f, sizeDp = 98f,  delayMs = 145L, durationMs = 960),
    FireParticle(xFrac = 0.92f, sizeDp = 110f, delayMs = 220L, durationMs = 1020),
)

private val StreakBgTop    = Color(0xFFFF9F1C)
private val StreakBgBottom = Color(0xFFF25C05)
private val StreakGlow      = Color(0xFFFFE29A)

@Composable
private fun StreakCelebrationOverlay(
    event: StreakCelebration,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val sounds  = remember { SoundManager.get(context) }
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()

    val overlayAlpha = remember { Animatable(0f) }
    val iconScale    = remember { Animatable(0.3f) }
    val counter      = remember { Animatable(event.previousStreak.toFloat()) }
    val headlineAlpha = remember { Animatable(0f) }
    val headlineY     = remember { Animatable(28f) }
    val rewardAlpha   = remember { Animatable(0f) }
    val rewardScale   = remember { Animatable(0.6f) }
    val ctaAlpha      = remember { Animatable(0f) }

    // Partículas del burst (reutiliza el layout de FIRE_PARTICLES)
    val travels = remember { FIRE_PARTICLES.map { Animatable(0f) } }
    val alphas  = remember { FIRE_PARTICLES.map { Animatable(0f) } }

    var dismissing by remember { mutableStateOf(false) }
    fun dismiss() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            overlayAlpha.animateTo(0f, tween(300))
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        // 1) Entrada del fondo
        overlayAlpha.animateTo(1f, tween(250, easing = FastOutSlowInEasing))

        // 2) Entrada del ícono con rebote
        iconScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )

        // 3) Conteo ascendente previous → new
        counter.animateTo(
            event.newStreak.toFloat(),
            tween(
                durationMillis = if (event.previousStreak == event.newStreak) 0 else 650,
                easing = FastOutSlowInEasing
            )
        )

        // 4) Punch + partículas + sonido + haptic en el clímax
        sounds.playMonkeyCheer()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        coroutineScope {
            launch {
                iconScale.animateTo(1.28f, tween(120, easing = EaseOut))
                iconScale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )
            }
            FIRE_PARTICLES.forEachIndexed { i, p ->
                launch {
                    delay(p.delayMs)
                    alphas[i].animateTo(1f, tween(80))
                    travels[i].animateTo(-720f, tween(p.durationMs, easing = EaseOut))
                    alphas[i].animateTo(0f, tween(220))
                }
            }
        }

        // 5) Titular
        launch { headlineAlpha.animateTo(1f, tween(240)) }
        headlineY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))

        // 6) Recompensa (bananas)
        launch { rewardAlpha.animateTo(1f, tween(180)) }
        rewardScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )

        // 7) CTA
        ctaAlpha.animateTo(1f, tween(220))
    }

    val bananasEarned = 1 + event.bonusBananas

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(Brush.verticalGradient(listOf(StreakBgTop, StreakBgBottom)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (ctaAlpha.value > 0.5f) dismiss() }
    ) {
        val widthDp = LocalConfiguration.current.screenWidthDp.dp
        val density = LocalDensity.current

        // Partículas de fuego subiendo desde la base
        FIRE_PARTICLES.forEachIndexed { i, p ->
            val sizeDp = p.sizeDp.dp
            val xOffset = widthDp * p.xFrac - sizeDp / 2
            Image(
                painter = painterResource(R.drawable.fuego),
                contentDescription = null,
                modifier = Modifier
                    .size(sizeDp)
                    .align(Alignment.BottomStart)
                    .offset(x = xOffset)
                    .graphicsLayer {
                        translationY = with(density) { travels[i].value.dp.toPx() }
                        alpha = alphas[i].value
                    }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícono + número protagonista
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to StreakGlow.copy(alpha = 0.55f),
                                0.6f to StreakGlow.copy(alpha = 0.12f),
                                1.0f to Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension / 2
                        )
                    )
                }
                Image(
                    painter = painterResource(R.drawable.fuego),
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                        }
                )
            }

            Text(
                text = "${counter.value.roundToInt()}",
                fontSize = 96.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = if (event.newStreak == 1) "día de racha" else "días de racha",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Titular
            Text(
                text = if (event.isMilestone) "¡Meta alcanzada! Tu mono está orgulloso"
                       else "¡Tu mono sigue impecable!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier.graphicsLayer {
                    alpha = headlineAlpha.value
                    translationY = with(density) { headlineY.value.dp.toPx() }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Recompensa de bananas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = rewardAlpha.value
                        scaleX = rewardScale.value
                        scaleY = rewardScale.value
                    }
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.banana),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+$bananasEarned",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (event.isMilestone) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "¡bonus x7!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFF3C4)
                    )
                }
            }
        }

        // CTA inferior
        Button(
            onClick = { dismiss() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = StreakBgBottom
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 40.dp)
                .height(54.dp)
                .graphicsLayer { alpha = ctaAlpha.value }
        ) {
            Text("¡Seguir!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun BananaCounter(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.banana),
            contentDescription = "Bananas",
            modifier = Modifier.size(HeaderIconSize)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$count",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEA580C)
        )
    }
}

@Composable
private fun StreakCounter(streak: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.fuego),
            contentDescription = null,
            modifier = Modifier.size(HeaderIconSize)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$streak",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEA580C)
        )
    }
}

@Composable
private fun ShopButton(
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(HeaderIconSize)
            .graphicsLayer { clip = false }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (highlighted) {
            ShopButtonHighlighted()
        } else {
            Image(
                painter = painterResource(R.drawable.cara_mono),
                contentDescription = "Tienda",
                modifier = Modifier.size(HeaderIconSize)
            )
        }
    }
}

@Composable
private fun BoxScope.ShopButtonHighlighted() {
    val transition = rememberInfiniteTransition(label = "shopHint")
    // Un solo ciclo: icono crece ↔ flecha avanza hacia el icono
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shopBreath"
    )

    val pulseScale = 1f + breath * 0.12f

    // Flecha abajo-izquierda; asset apunta a la derecha → -45° = diagonal arriba-derecha
    val sway = 12f
    val diag = 0.70710678f
    val arrowX = -40f + breath * sway * diag
    val arrowY = 36f - breath * sway * diag

    Image(
        painter = painterResource(R.drawable.cara_mono),
        contentDescription = "Tienda",
        modifier = Modifier
            .size(HeaderIconSize)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
    )
    Image(
        painter = painterResource(R.drawable.flecha_celeste),
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(x = arrowX.dp, y = arrowY.dp)
            .size(64.dp)
            .graphicsLayer { rotationZ = -45f }
    )
}

// ─── Ondas decorativas ────────────────────────────────────────────────────────

private fun DrawScope.drawBottomWaves(color: Color) {
    val w = size.width
    val h = size.height

    drawPath(
        path = Path().apply {
            moveTo(0f, h * 0.80f)
            cubicTo(w * 0.20f, h * 0.74f, w * 0.55f, h * 0.84f, w * 0.80f, h * 0.77f)
            cubicTo(w * 0.90f, h * 0.74f, w * 0.95f, h * 0.78f, w, h * 0.76f)
            lineTo(w, h); lineTo(0f, h); close()
        },
        color = color.copy(alpha = 0.10f)
    )
    drawPath(
        path = Path().apply {
            moveTo(0f, h * 0.87f)
            cubicTo(w * 0.25f, h * 0.81f, w * 0.60f, h * 0.91f, w * 0.85f, h * 0.84f)
            cubicTo(w * 0.93f, h * 0.81f, w * 0.97f, h * 0.86f, w, h * 0.84f)
            lineTo(w, h); lineTo(0f, h); close()
        },
        color = color.copy(alpha = 0.20f)
    )
    drawPath(
        path = Path().apply {
            moveTo(0f, h * 0.93f)
            cubicTo(w * 0.30f, h * 0.87f, w * 0.65f, h * 0.96f, w * 0.88f, h * 0.90f)
            cubicTo(w * 0.94f, h * 0.88f, w * 0.97f, h * 0.92f, w, h * 0.90f)
            lineTo(w, h); lineTo(0f, h); close()
        },
        color = color.copy(alpha = 0.35f)
    )
}
