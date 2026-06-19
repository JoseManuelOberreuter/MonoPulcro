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
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.josem.monopulcro.notifications.NotificationHelper
import com.josem.monopulcro.notifications.NotificationScheduler
import com.josem.monopulcro.ui.MainScreen
import com.josem.monopulcro.ui.ShopScreen
import com.josem.monopulcro.ui.SplashScreen
import com.josem.monopulcro.ui.TaskEditScreen
import com.josem.monopulcro.ui.theme.MonoPulcroTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permiso concedido: crear canales y programar el alarm
            NotificationHelper.createChannels(this)
            NotificationScheduler.schedule(this)
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

                Crossfade(
                    targetState  = showSplash,
                    animationSpec = tween(durationMillis = 500),
                    label        = "splashTransition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        AppNavigation()
                    }
                }
            }
        }
    }

    private fun setupNotifications() {
        NotificationHelper.createChannels(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: pedir permiso en tiempo de ejecución
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    NotificationScheduler.schedule(this)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 y anteriores: no requiere permiso en runtime
            NotificationScheduler.schedule(this)
        }
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onAddTask  = { navController.navigate("task_edit/new") },
                onEditTask = { taskId -> navController.navigate("task_edit/$taskId") },
                onOpenShop = { navController.navigate("shop") }
            )
        }
        composable("task_edit/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: "new"
            TaskEditScreen(
                taskId         = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("shop") {
            ShopScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
