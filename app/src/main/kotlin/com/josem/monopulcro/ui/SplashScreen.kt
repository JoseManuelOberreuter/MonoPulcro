package com.josem.monopulcro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.josem.monopulcro.R
import com.josem.monopulcro.audio.SoundManager
import kotlinx.coroutines.delay

private val SplashWaveColor = Color(0xFF7DD3FC)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val sounds  = remember { SoundManager.get(context) }

    // Fade-in del contenido
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.88f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 700))
        scale.animateTo(1f, animationSpec = tween(durationMillis = 600))
        sounds.playIntroJingle()
        delay(1_600L)
        onFinished()
    }   

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Ondas decorativas en la parte inferior (idénticas a MainScreen)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSplashWaves(SplashWaveColor)
        }

        // Contenido central con fade + scale
        Column(
            modifier = Modifier
                .graphicsLayer {
                    this.alpha = alpha.value
                    this.scaleX = scale.value
                    this.scaleY = scale.value
                }
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(MonkeyImageResolver.DEFAULT_PULCRO),
                contentDescription = "Mono Pulcro",
                modifier = Modifier.size(190.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Mono Pulcro",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tu mono siempre pulcro",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Ondas sutiles splash ─────────────────────────────────────────────────────

private fun DrawScope.drawSplashWaves(color: Color) {
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
