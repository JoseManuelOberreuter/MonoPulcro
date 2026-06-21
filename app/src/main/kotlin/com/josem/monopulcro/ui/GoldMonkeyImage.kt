package com.josem.monopulcro.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.josem.monopulcro.R

@Composable
fun GoldMonkeyImage(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    showGoldStack: Boolean = false
) {
    if (showGoldStack) {
        BoxWithConstraints(
            modifier = modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.pila_de_oro),
                contentDescription = null,
                modifier = Modifier
                    .size(maxWidth * 0.58f)
                    .align(Alignment.BottomEnd)
                    .offset(x = -maxWidth * 0.7f, y = maxHeight * -0.02f)
            )
            GoldMonkeyCore(
                modifier = Modifier.fillMaxSize(),
                contentDescription = contentDescription
            )
        }
    } else {
        GoldMonkeyCore(modifier = modifier, contentDescription = contentDescription)
    }
}

@Composable
private fun GoldMonkeyCore(
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.mono_de_oro),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )
        GoldSparkle(
            modifier = Modifier
                .size(maxWidth * 0.34f)
                .align(Alignment.TopStart)
                .offset(x = maxWidth * 0.02f, y = maxHeight * 0.05f),
            durationMs = 1200
        )
        GoldSparkle(
            modifier = Modifier
                .size(maxWidth * 0.30f)
                .align(Alignment.TopEnd)
                .offset(x = maxWidth * 0.06f, y = maxHeight * 0.06f),
            durationMs = 1500
        )
        GoldSparkle(
            modifier = Modifier
                .size(maxWidth * 0.32f)
                .align(Alignment.BottomEnd)
                .offset(x = -maxWidth * 0.12f, y = -maxHeight * 0.18f),
            durationMs = 1350
        )
    }
}

@Composable
private fun GoldSparkle(modifier: Modifier, durationMs: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "goldSparkle")
    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha"
    )
    Image(
        painter = painterResource(R.drawable.brillo_de_oro),
        contentDescription = null,
        modifier = modifier.alpha(sparkleAlpha)
    )
}
