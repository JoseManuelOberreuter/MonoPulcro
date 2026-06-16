package com.josem.monopulcro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.josem.monopulcro.ui.MainScreen
import com.josem.monopulcro.ui.SplashScreen
import com.josem.monopulcro.ui.TaskEditScreen
import com.josem.monopulcro.ui.theme.MonoPulcroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onAddTask  = { navController.navigate("task_edit/new") },
                onEditTask = { taskId -> navController.navigate("task_edit/$taskId") }
            )
        }
        composable("task_edit/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: "new"
            TaskEditScreen(
                taskId        = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
