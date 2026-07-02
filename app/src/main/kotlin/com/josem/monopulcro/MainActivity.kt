package com.josem.monopulcro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.josem.monopulcro.data.MonkeyStateManager
import com.josem.monopulcro.notifications.NotificationHelper
import com.josem.monopulcro.notifications.NotificationScheduler
import com.josem.monopulcro.notifications.TaskNotificationScheduler
import com.josem.monopulcro.ui.MainScreen
import com.josem.monopulcro.ui.OnboardingScreen
import com.josem.monopulcro.ui.ShopScreen
import com.josem.monopulcro.ui.SplashScreen
import com.josem.monopulcro.ui.TaskEditScreen
import com.josem.monopulcro.ui.theme.MonoPulcroTheme

private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_MAIN = "main"
private const val ROUTE_TASK_NEW = "task_new"
private const val ROUTE_TASK_EDIT = "task_edit/{taskId}"
private const val ROUTE_SHOP = "shop"

class MainActivity : ComponentActivity() {

    private val stateManager by lazy { MonkeyStateManager(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scheduleNotificationsSafely()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupNotifications()

        setContent {
            MonoPulcroTheme {
                var showSplash by remember { mutableStateOf(true) }
                val startOnboarding = remember { !stateManager.onboardingCompleted }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 500),
                    label = "splashTransition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        AppNavigation(
                            startOnboarding = startOnboarding,
                            onOnboardingComplete = { stateManager.completeOnboarding() },
                        )
                    }
                }
            }
        }
    }

    private fun setupNotifications() {
        try {
            NotificationHelper.createChannels(this)
        } catch (_: Exception) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    scheduleNotificationsSafely()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            scheduleNotificationsSafely()
        }
    }

    private fun scheduleNotificationsSafely() {
        try {
            NotificationScheduler.schedule(this)
            TaskNotificationScheduler.scheduleAll(this)
        } catch (_: Exception) {
            // Evita crash al programar alarmas en dispositivos restrictivos
        }
    }
}

@Composable
private fun AppNavigation(
    startOnboarding: Boolean,
    onOnboardingComplete: () -> Unit,
) {
    val navController = rememberNavController()
    val startDestination = if (startOnboarding) ROUTE_ONBOARDING else ROUTE_MAIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    onOnboardingComplete()
                    navController.navigate(ROUTE_MAIN) {
                        popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onAddFirstTask = {
                    onOnboardingComplete()
                    navController.navigate(ROUTE_MAIN) {
                        popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                    navController.navigate(ROUTE_TASK_NEW) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(ROUTE_MAIN) {
            MainScreen(
                onAddTask = {
                    navController.navigate(ROUTE_TASK_NEW) {
                        launchSingleTop = true
                    }
                },
                onEditTask = { taskId ->
                    navController.navigate("task_edit/$taskId") {
                        launchSingleTop = true
                    }
                },
                onOpenShop = {
                    navController.navigate(ROUTE_SHOP) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(ROUTE_TASK_NEW) {
            TaskEditScreen(
                taskId = "new",
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(ROUTE_TASK_EDIT) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            if (taskId != null) {
                TaskEditScreen(
                    taskId = taskId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
        composable(ROUTE_SHOP) {
            ShopScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
