package com.example.dbms_shubham_application.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dbms_shubham_application.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = navController)
        }
        composable("role_selection") {
            RoleSelectionScreen(navController = navController)
        }
        composable("login/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "student"
            LoginScreen(navController = navController, role = role)
        }
        composable("signup/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "student"
            SignUpScreen(navController = navController, role = role)
        }
        composable("forgot_password") {
            ForgotPasswordScreen(navController = navController)
        }
        composable("dashboard/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "student"
            DashboardScreen(navController = navController, role = role)
        }
        composable("mark_attendance") {
            MarkAttendanceScreen(navController = navController)
        }
        composable("attendance_history") {
            AttendanceHistoryScreen(navController = navController)
        }
        composable("profile") {
            ProfileScreen(navController = navController)
        }
        composable("alerts") {
            AlertsScreen(navController = navController)
        }
        composable("faculty_classes") {
            FacultyClassesScreen(navController = navController)
        }
        composable("reports") {
            ReportsScreen(navController = navController)
        }
        composable("faculty_history") {
            FacultyHistoryScreen(navController = navController)
        }
        composable("manage_schedule") {
            ManageScheduleScreen(navController = navController)
        }
        composable(
            route = "start_session?subject_id={subject_id}&classroom_id={classroom_id}",
            arguments = listOf(
                androidx.navigation.navArgument("subject_id") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("classroom_id") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val subId = backStackEntry.arguments?.getString("subject_id")
            val roomId = backStackEntry.arguments?.getString("classroom_id")
            StartSessionScreen(navController = navController, prefillSubjectId = subId, prefillClassroomId = roomId)
        }
        composable("start_session") {
            StartSessionScreen(navController = navController)
        }
    }
}
