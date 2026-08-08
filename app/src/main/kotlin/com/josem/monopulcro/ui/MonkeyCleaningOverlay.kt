package com.josem.monopulcro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.josem.monopulcro.R
import com.josem.monopulcro.data.Achievement
import com.josem.monopulcro.data.DustMote
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val SPRAY_DURATION_MS = 3_000L
private const val SPRAY_START_DELAY_MS = 500L
private const val SPRAY_BURST_MS = 600L
private const val CLOTH_START_DELAY_MS = 1_200L
private const val SPRAY_BURST_COUNT = 5
private const val CLOTH_DURATION_MS = 1_500
private const val FADE_OUT_MS = 450

private data class ClothPose(val x: Float, val y: Float, val rotation: Float)

/** Recorrido en zigzag: arriba→abajo, alternando izquierda/derecha (5 trazos en 1 s). */
private val CLOTH_ZIGZAG_KEYS = listOf(
    ClothPose(-72f, -44f, -26f),
    ClothPose(72f, -44f, -26f),
    ClothPose(-72f, 0f, 26f),
    ClothPose(72f, 0f, 26f),
    ClothPose(-72f, 44f, -26f),
    ClothPose(72f, 44f, -26f),
)

private suspend fun animateClothZigzag(
    clothX: Animatable<Float, *>,
    clothY: Animatable<Float, *>,
    clothRotation: Animatable<Float, *>,
) {
    val strokeMs = CLOTH_DURATION_MS / (CLOTH_ZIGZAG_KEYS.size - 1)
    val first = CLOTH_ZIGZAG_KEYS.first()
    clothX.snapTo(first.x)
    clothY.snapTo(first.y)
    clothRotation.snapTo(first.rotation)

    for (i in 1 until CLOTH_ZIGZAG_KEYS.size) {
        val pose = CLOTH_ZIGZAG_KEYS[i]
        coroutineScope {
            launch {
                clothX.animateTo(pose.x, tween(strokeMs, easing = LinearEasing))
            }
            launch {
                clothY.animateTo(pose.y, tween(strokeMs, easing = LinearEasing))
            }
            launch {
                clothRotation.animateTo(pose.rotation, tween(strokeMs, easing = LinearEasing))
            }
        }
    }
}

private data class SprayDrop(
    val burstIndex: Int,
    val xFrac: Float,
    val yFrac: Float,
    val sizeDp: Float,
    val fallStartDp: Float,
)

private data class SprayDropTemplate(
    val xFrac: Float,
    val yFrac: Float,
    val sizeDp: Float,
    val fallStartDp: Float,
)

/**
 * Gotas de agua al limpiar (spray_bottle ≈ 3 s, 5 ráfagas).
 * Cada lista = una ráfaga (~0,5 s + N×600 ms). sizeDp = diámetro de la gota.
 */
private val SPRAY_BURST_DROPS: List<List<SprayDropTemplate>> = listOf(
    // Ráfaga 1 (~0,5 s) — zona cabeza / frente
    listOf(
        SprayDropTemplate(0.18f, 0.14f, 26f, 20f), // frente izq. (grande)
        SprayDropTemplate(0.42f, 0.10f, 12f, 12f), // centro frente (pequeña)
        SprayDropTemplate(0.68f, 0.18f, 16f, 16f), // frente der. (mediana)
        SprayDropTemplate(0.82f, 0.26f, 10f, 10f), // sien der. (pequeña)
    ),
    // Ráfaga 2 (~1,1 s) — hombros y laterales altos
    listOf(
        SprayDropTemplate(0.12f, 0.28f, 22f, 18f), // hombro izq. (grande)
        SprayDropTemplate(0.30f, 0.22f, 11f, 11f), // clavícula izq. (pequeña)
        SprayDropTemplate(0.55f, 0.16f, 18f, 15f), // nuca / cuello (mediana)
        SprayDropTemplate(0.78f, 0.12f, 13f, 12f), // hombro der. (pequeña)
    ),
    // Ráfaga 3 (~1,7 s) — pecho y brazos
    listOf(
        SprayDropTemplate(0.22f, 0.40f, 24f, 18f), // pecho izq. (grande)
        SprayDropTemplate(0.48f, 0.36f, 14f, 12f), // esternón (mediana)
        SprayDropTemplate(0.72f, 0.34f, 20f, 16f), // pecho der. (grande)
        SprayDropTemplate(0.88f, 0.44f, 10f, 10f), // brazo der. (pequeña)
        SprayDropTemplate(0.36f, 0.48f, 11f, 11f), // abdomen alto (pequeña)
    ),
    // Ráfaga 4 (~2,3 s) — vientre y costados medios
    listOf(
        SprayDropTemplate(0.14f, 0.52f, 18f, 14f), // flanco izq. (mediana)
        SprayDropTemplate(0.38f, 0.58f, 26f, 20f), // ombligo (grande)
        SprayDropTemplate(0.62f, 0.50f, 12f, 11f), // costado der. (pequeña)
        SprayDropTemplate(0.80f, 0.58f, 15f, 13f), // cadera der. (mediana)
    ),
    // Ráfaga 5 (~2,9 s) — piernas y pies
    listOf(
        SprayDropTemplate(0.26f, 0.68f, 22f, 16f), // muslo izq. (grande)
        SprayDropTemplate(0.50f, 0.72f, 28f, 22f), // entre piernas (grande)
        SprayDropTemplate(0.70f, 0.76f, 13f, 12f), // muslo der. (pequeña)
        SprayDropTemplate(0.34f, 0.84f, 10f, 10f), // pie izq. (pequeña)
        SprayDropTemplate(0.58f, 0.86f, 11f, 10f), // pie der. (pequeña)
    ),
)

private fun buildSprayDrops(): List<SprayDrop> =
    SPRAY_BURST_DROPS.flatMapIndexed { burstIndex, templates ->
        templates.map { t ->
            SprayDrop(
                burstIndex = burstIndex,
                xFrac = t.xFrac,
                yFrac = t.yFrac,
                sizeDp = t.sizeDp,
                fallStartDp = t.fallStartDp,
            )
        }
    }

/** Motas persistentes encima del mono (posiciones fijas por slot). */
@Composable
fun DustMotesOverlay(
    motes: List<DustMote>,
    modifier: Modifier = Modifier,
    moteAlpha: Float = 1f
) {
    if (motes.isEmpty()) return
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        motes.forEach { mote ->
            val half = mote.sizeDp / 2f
            val x = (maxWidth.value * mote.xFrac - half)
                .coerceIn(0f, maxWidth.value - mote.sizeDp)
            val y = (maxHeight.value * mote.yFrac - half)
                .coerceIn(0f, maxHeight.value - mote.sizeDp)
            Image(
                painter = painterResource(R.drawable.polvo_mota),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(mote.sizeDp.dp)
                    .offset(x = x.dp, y = y.dp)
                    .graphicsLayer { alpha = moteAlpha }
            )
        }
    }
}

@Composable
private fun FallingSprayDrop(
    drop: SprayDrop,
    containerWidthDp: Float,
    containerHeightDp: Float,
    fadeOutAlpha: Float = 1f,
) {
    val dropAlpha = remember { Animatable(0f) }
    val fallOffset = remember { Animatable(-drop.fallStartDp) }
    val dropScale = remember { Animatable(0.45f) }

    LaunchedEffect(drop) {
        delay(SPRAY_START_DELAY_MS + drop.burstIndex * SPRAY_BURST_MS)
        launch {
            dropAlpha.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
        }
        launch {
            dropScale.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
        }
        fallOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(380, easing = LinearOutSlowInEasing)
        )
    }

    val half = drop.sizeDp / 2f
    val x = (containerWidthDp * drop.xFrac - half)
        .coerceIn(0f, containerWidthDp - drop.sizeDp)
    val y = (containerHeightDp * drop.yFrac - half)
        .coerceIn(0f, containerHeightDp - drop.sizeDp)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.gota_de_agua),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(drop.sizeDp.dp)
                .offset(x = x.dp, y = (y + fallOffset.value).dp)
                .graphicsLayer {
                    alpha = dropAlpha.value * fadeOutAlpha
                    scaleX = dropScale.value
                    scaleY = dropScale.value
                }
        )
    }
}

/**
 * Spray (3 s, 5 ráfagas) → gotas cayendo → paño → motas fuera.
 * [onCleaningFinished] al terminar el paño (antes del +1 banana).
 */
@Composable
fun MonkeyCleaningOverlay(
    dustMotesAtStart: List<DustMote>,
    onCleaningFinished: () -> Unit
) {
    val sprayDrops = remember { buildSprayDrops() }
    val clothX = remember { Animatable(CLOTH_ZIGZAG_KEYS.first().x) }
    val clothY = remember { Animatable(CLOTH_ZIGZAG_KEYS.first().y) }
    val clothRotation = remember { Animatable(CLOTH_ZIGZAG_KEYS.first().rotation) }
    val clothAlpha = remember { Animatable(0f) }
    val dropsAlpha = remember { Animatable(1f) }
    val dustAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(SPRAY_DURATION_MS + CLOTH_START_DELAY_MS)

        clothAlpha.animateTo(1f, tween(220, easing = FastOutSlowInEasing))

        coroutineScope {
            if (dustMotesAtStart.isNotEmpty()) {
                launch {
                    dustAlpha.animateTo(
                        0f,
                        tween(CLOTH_DURATION_MS, easing = LinearOutSlowInEasing)
                    )
                }
            }
            launch {
                dropsAlpha.animateTo(
                    0f,
                    tween(CLOTH_DURATION_MS, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                animateClothZigzag(clothX, clothY, clothRotation)
            }
        }

        clothAlpha.animateTo(0f, tween(FADE_OUT_MS, easing = LinearOutSlowInEasing))
        onCleaningFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (dustMotesAtStart.isNotEmpty()) {
            DustMotesOverlay(
                motes = dustMotesAtStart,
                moteAlpha = dustAlpha.value
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            sprayDrops.forEach { drop ->
                FallingSprayDrop(
                    drop = drop,
                    containerWidthDp = maxWidth.value,
                    containerHeightDp = maxHeight.value,
                    fadeOutAlpha = dropsAlpha.value,
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.esponja_limpia),
            contentDescription = null,
            modifier = Modifier
                .size(88.dp)
                .align(Alignment.Center)
                .offset(x = clothX.value.dp, y = clothY.value.dp)
                .graphicsLayer {
                    alpha = clothAlpha.value
                    rotationZ = clothRotation.value
                }
        )
    }
}

private data class BananaExplosionParticle(
    val xFrac: Float,
    val driftDp: Float,
    val travelDp: Float,
    val sizeDp: Float,
    val delayMs: Long,
    val durationMs: Int,
    val spinDeg: Float,
)

/** Ráfaga de bananas explotando desde abajo (cantidad visual acotada para que se lea bien). */
private fun bananaExplosionParticles(amount: Int): List<BananaExplosionParticle> {
    val visualCount = amount.coerceIn(6, 14)
    return List(visualCount) {
        BananaExplosionParticle(
            xFrac = 0.08f + Random.nextFloat() * 0.84f,
            driftDp = (Random.nextFloat() - 0.5f) * 180f,
            travelDp = 260f + Random.nextFloat() * 240f,
            sizeDp = 30f + Random.nextFloat() * 26f,
            delayMs = (Random.nextFloat() * 180f).toLong(),
            durationMs = 700 + (Random.nextFloat() * 300f).toInt(),
            spinDeg = (Random.nextFloat() - 0.5f) * 480f,
        )
    }
}

/** Explosión de bananas desde abajo + "+N bananas" en grande al centro. */
@Composable
fun BananaRewardOverlay(amount: Int = 1, onFinished: () -> Unit) {
    val density = LocalDensity.current
    val particles = remember(amount) { bananaExplosionParticles(amount) }
    val travel = remember(particles) { particles.map { Animatable(0f) } }
    val burstAlpha = remember(particles) { particles.map { Animatable(0f) } }

    val textAlpha = remember { Animatable(0f) }
    val textScale = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            particles.forEachIndexed { i, p ->
                launch {
                    delay(p.delayMs)
                    burstAlpha[i].animateTo(1f, tween(80))
                    travel[i].animateTo(1f, tween(p.durationMs, easing = EaseOut))
                    burstAlpha[i].animateTo(0f, tween(220))
                }
            }
            launch {
                textAlpha.animateTo(1f, tween(160))
                textScale.animateTo(1.15f, tween(180, easing = EaseOut))
                textScale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )
            }
        }
        delay(500L)
        textAlpha.animateTo(0f, tween(280))
        onFinished()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthDp = maxWidth.value

        particles.forEachIndexed { i, p ->
            val progress = travel[i].value
            val xOffset = (widthDp * p.xFrac - p.sizeDp / 2).dp
            Image(
                painter = painterResource(R.drawable.banana),
                contentDescription = null,
                modifier = Modifier
                    .size(p.sizeDp.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = xOffset)
                    .graphicsLayer {
                        translationX = with(density) { (p.driftDp * progress).dp.toPx() }
                        translationY = with(density) { (-p.travelDp * progress).dp.toPx() }
                        rotationZ = p.spinDeg * progress
                        alpha = burstAlpha[i].value
                    }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = textAlpha.value
                    scaleX = textScale.value
                    scaleY = textScale.value
                }
        ) {
            Text(
                text = "+$amount",
                fontSize = 76.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFEA580C)
            )
            Text(
                text = if (amount == 1) "banana" else "bananas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEA580C)
            )
        }
    }
}

/** Insignia de "logro desbloqueado": aparece al centro con un rebote y se desvanece sola. */
@Composable
fun AchievementUnlockedOverlay(achievement: Achievement, onFinished: () -> Unit) {
    val cardAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.6f) }

    LaunchedEffect(achievement) {
        cardAlpha.animateTo(1f, tween(160))
        cardScale.animateTo(1.08f, tween(180, easing = EaseOut))
        cardScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
        delay(1_600L)
        cardAlpha.animateTo(0f, tween(280))
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    alpha = cardAlpha.value
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                }
                .background(Color(0xFFFFF7ED), RoundedCornerShape(20.dp))
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            if (achievement.iconRes != null) {
                Image(
                    painter = painterResource(achievement.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Text(text = achievement.emoji, fontSize = 56.sp)
            }
            Text(
                text = "¡Logro desbloqueado!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEA580C)
            )
            Text(
                text = achievement.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = achievement.description,
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )
        }
    }
}
