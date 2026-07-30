package com.josem.monopulcro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.josem.monopulcro.audio.SoundManager
import kotlinx.coroutines.launch

private val PurchaseBgTop = Color(0xFFFB923C)
private val PurchaseBgBottom = Color(0xFFEA580C)
private val PurchaseGlow = Color(0xFFFFF3C4)

@Composable
fun OutfitPurchaseOverlay(
    accessoryId: String,
    accessoryName: String,
    onUse: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sounds = remember { SoundManager.get(context) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val previewRes = remember(accessoryId) {
        MonkeyImageResolver.previewForAccessory(accessoryId)
    }

    val overlayAlpha = remember { Animatable(0f) }
    val monkeyScale = remember { Animatable(0.35f) }
    val headlineAlpha = remember { Animatable(0f) }
    val headlineY = remember { Animatable(28f) }
    val ctaAlpha = remember { Animatable(0f) }

    var closing by remember { mutableStateOf(false) }
    fun dismiss(action: () -> Unit) {
        if (closing || ctaAlpha.value < 0.5f) return
        closing = true
        scope.launch {
            overlayAlpha.animateTo(0f, tween(280))
            action()
        }
    }

    BackHandler { dismiss(onDismiss) }

    LaunchedEffect(Unit) {
        overlayAlpha.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
        monkeyScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        sounds.playMonkeyCheer()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch {
            headlineAlpha.animateTo(1f, tween(220))
            headlineY.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        }
        ctaAlpha.animateTo(1f, tween(280, delayMillis = 350))
    }

    val breath = rememberInfiniteTransition(label = "purchaseBreath")
    val breathScale by breath.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "purchaseBreathScale"
    )

    Box(
        modifier = Modifier
            .modalOverlayScrim(onBackgroundTap = { dismiss(onDismiss) })
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(Brush.verticalGradient(listOf(PurchaseBgTop, PurchaseBgBottom))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(320.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    PurchaseGlow.copy(alpha = 0.55f),
                                    PurchaseGlow.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension * 0.5f
                        )
                    }
                    Image(
                        painter = painterResource(previewRes),
                        contentDescription = accessoryName,
                        modifier = Modifier
                            .size(240.dp)
                            .graphicsLayer {
                                val s = monkeyScale.value * breathScale
                                scaleX = s
                                scaleY = s
                            }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "¡Nuevo atuendo!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = headlineAlpha.value
                        translationY = headlineY.value
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = accessoryName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = headlineAlpha.value }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tu mono ya puede lucirlo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = headlineAlpha.value }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = ctaAlpha.value },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { dismiss(onUse) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = PurchaseBgBottom
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text("Usar ahora", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { dismiss(onDismiss) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Más tarde",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
