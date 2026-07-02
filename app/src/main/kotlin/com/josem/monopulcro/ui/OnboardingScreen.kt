package com.josem.monopulcro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.josem.monopulcro.R
import kotlinx.coroutines.launch

private val WaveColor = Color(0xFF7DD3FC)

private data class OnboardingPage(
    val imageRes: Int,
    val title: String,
    val description: String
)

private val ONBOARDING_PAGES = listOf(
    OnboardingPage(
        imageRes = MonkeyImageResolver.DEFAULT_PULCRO,
        title = "Bienvenido a Mono Pulcro",
        description = "Tu mono te ayuda a mantener tus hábitos de limpieza cada día"
    ),
    OnboardingPage(
        imageRes = R.drawable.banana,
        title = "Gana bananas",
        description = "Completa todas tus tareas del día y gana 1 banana como recompensa"
    ),
    OnboardingPage(
        imageRes = R.drawable.fuego,
        title = "Construye tu racha",
        description = "Cada día que completes tus tareas aumenta tu racha. ¡No la rompas!"
    ),
    OnboardingPage(
        imageRes = R.drawable.mota_polvo,
        title = "Pelusas en tu mono",
        description = "Con el tiempo aparecen motas de polvo sobre tu mono (1 por hora, hasta 5). " +
            "Tócalo para limpiarlas con spray y paño y gana 1 banana extra."
    ),
    OnboardingPage(
        imageRes = R.drawable.cara_mono,
        title = "La tienda",
        description = "Usa tus bananas en la tienda para comprarle accesorios a tu mono"
    )
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onAddFirstTask: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == ONBOARDING_PAGES.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawBottomWaves(WaveColor) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinished) {
                    Text(
                        text = "Omitir",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPageContent(page = ONBOARDING_PAGES[page])
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                ONBOARDING_PAGES.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                            .background(
                                color = if (pagerState.currentPage == index)
                                    Color(0xFF0EA5E9) else Color(0xFFCBD5E1),
                                shape = CircleShape
                            )
                    )
                }
            }

            if (isLastPage) {
                Button(
                    onClick = onAddFirstTask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) {
                    Text(
                        text = "Agregar mi primera tarea",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) {
                    Text(
                        text = "Siguiente",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(page.imageRes),
            contentDescription = null,
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.description,
            fontSize = 15.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

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
