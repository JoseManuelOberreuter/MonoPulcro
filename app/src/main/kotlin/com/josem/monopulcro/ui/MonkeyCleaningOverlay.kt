package com.josem.monopulcro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import com.josem.monopulcro.data.DustMote
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                painter = painterResource(R.drawable.mota_polvo),
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

/** +1 banana al final de limpiar motas. */
@Composable
fun BananaRewardOverlay(onFinished: () -> Unit) {
    val overlayAlpha = remember { Animatable(0f) }
    val plusOneY = remember { Animatable(0f) }
    val plusOneAlpha = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        overlayAlpha.animateTo(1f, tween(150))
        delay(80L)
        plusOneAlpha.animateTo(1f, tween(140))
        plusOneY.animateTo(-220f, tween(950, easing = FastOutSlowInEasing))
        delay(200L)
        overlayAlpha.animateTo(0f, tween(300))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 48.dp)
                .graphicsLayer {
                    translationY = with(density) { plusOneY.value.dp.toPx() }
                    alpha = plusOneAlpha.value
                }
        ) {
            Image(
                painter = painterResource(R.drawable.banana),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Text(
                "+1",
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFEA580C)
            )
        }
    }
}
