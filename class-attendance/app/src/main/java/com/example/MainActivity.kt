package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.AddClassScreen
import com.example.ui.AttendanceOverviewScreen
import com.example.ui.MainScreen
import com.example.ui.ReviewScreen
import com.example.ui.ScannerScreen
import com.example.ui.Screen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AttendanceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AttendanceApp()
                }
            }
        }
    }
}

@Composable
fun AttendanceApp(
    viewModel: AttendanceViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToAddClass = {
                    navController.navigate(Screen.AddClass.route)
                },
                onResumeActiveSession = {
                    navController.navigate(Screen.AttendanceOverview.route)
                }
            )
        }

        composable(Screen.AddClass.route) {
            BackHandler {
                navController.popBackStack()
            }
            AddClassScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartAttendance = {
                    navController.navigate(Screen.AttendanceOverview.route)
                }
            )
        }

        composable(Screen.AttendanceOverview.route) {
            BackHandler {
                navController.popBackStack(Screen.Home.route, inclusive = false)
            }
            AttendanceOverviewScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onOpenScanner = {
                    navController.navigate(Screen.Scanner.route)
                },
                onNavigateToReview = {
                    navController.navigate(Screen.Review.route)
                }
            )
        }

        composable(Screen.Scanner.route) {
            BackHandler {
                navController.popBackStack()
            }
            ScannerScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDoneScanning = {
                    navController.navigate(Screen.Review.route) {
                        popUpTo(Screen.AttendanceOverview.route)
                    }
                }
            )
        }

        composable(Screen.Review.route) {
            BackHandler {
                navController.popBackStack(Screen.AttendanceOverview.route, inclusive = false)
            }
            ReviewScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onBackToScanner = {
                    navController.navigate(Screen.Scanner.route)
                },
                onFinishAndSave = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
    }
}
