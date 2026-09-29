package com.example.ui

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddClass : Screen("add_class")
    data object AttendanceOverview : Screen("attendance_overview")
    data object Scanner : Screen("scanner")
    data object Review : Screen("review")
}
