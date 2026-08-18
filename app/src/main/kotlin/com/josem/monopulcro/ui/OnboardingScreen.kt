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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.josem.monopulcro.R
import com.josem.monopulcro.analytics.AnalyticsLogger
import kotlinx.coroutines.launch

private val WaveColor = Color(0xFF7DD3FC)

private data class OnboardingPage(
    val imageRes: Int,
    val title: String,
    val description: String
)

private val ONBOARDING_PAGES = listOf(
    OnboardingPage(
        imageRes = R.drawable.cara_mono,
        title = "Bienvenido a Mono Pulcro",
        description = "La app que te ayuda con los hábitos de tu hogar."
    ),
    OnboardingPage(
        imageRes = R.drawable.cofre_abierto,
        title = "Un hábito que se disfruta",
        description = "Convierte tus tareas del hogar en un hábito entretenido y lleno de recompensas."
    ),
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    vm: MonkeyViewModel = viewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == ONBOARDING_PAGES.lastIndex

    LaunchedEffect(Unit) {
        AnalyticsLogger.log(AnalyticsLogger.Events.ONBOARDING_START)
    }

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
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isLastPage) {
                    TextButton(
                        onClick = {
                            AnalyticsLogger.log(AnalyticsLogger.Events.ONBOARDING_SKIPPED)
                            vm.seedFirstTasks()
                            onFinished()
                        },
                    ) {
                        Text(
                            text = "Saltar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B),
                        )
                    }
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
                    onClick = {
                        AnalyticsLogger.log(AnalyticsLogger.Events.ONBOARDING_COMPLETE)
                        vm.seedFirstTasks()
                        onFinished()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) {
                    Text(
                        text = "Empezar",
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
