package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.EduCorpTheme
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EduCorpTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TrainingAppNavigation()
                }
            }
        }
    }
}

@Composable
fun TrainingAppNavigation(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val startDestination = if (authState is AuthState.Authenticated) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            val user = currentUser
            if (user == null) {
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            } else {
                HomeScreen(
                    viewModel = viewModel,
                    user = user,
                    onSelectCourse = { courseId ->
                        navController.navigate("course_detail/$courseId")
                    },
                    onOpenDashboard = { navController.navigate("student_dashboard") },
                    onOpenAdminPanel = { navController.navigate("admin_dashboard") },
                    onOpenProfile = { navController.navigate("profile") }
                )
            }
        }

        composable(
            route = "course_detail/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.IntType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getInt("courseId") ?: 0
            val user = currentUser
            if (user == null) {
                navController.navigate("login")
            } else {
                CourseDetailScreen(
                    viewModel = viewModel,
                    courseId = courseId,
                    userId = user.id,
                    onBack = { navController.popBackStack() },
                    onOpenModule = { moduleId ->
                        navController.navigate("module_player/$moduleId")
                    }
                )
            }
        }

        composable(
            route = "module_player/{moduleId}",
            arguments = listOf(navArgument("moduleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getInt("moduleId") ?: 0
            ModulePlayerScreen(
                viewModel = viewModel,
                moduleId = moduleId,
                onBack = { navController.popBackStack() },
                onStartQuiz = { targetModuleId ->
                    navController.navigate("quiz/$targetModuleId")
                }
            )
        }

        composable(
            route = "quiz/{moduleId}",
            arguments = listOf(navArgument("moduleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getInt("moduleId") ?: 0
            QuizScreen(
                viewModel = viewModel,
                moduleId = moduleId,
                onBack = { navController.popBackStack() },
                onQuizCompleted = { navController.popBackStack() }
            )
        }

        composable("student_dashboard") {
            val user = currentUser
            if (user != null) {
                StudentDashboardScreen(
                    viewModel = viewModel,
                    user = user,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("admin_dashboard") {
            AdminDashboardScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenUserManagement = { navController.navigate("admin_user_crud") },
                onEditCourse = { courseId ->
                    navController.navigate("admin_course_editor/$courseId")
                }
            )
        }

        composable("admin_user_crud") {
            AdminUserManagementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "admin_course_editor/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.IntType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getInt("courseId") ?: 0
            AdminCourseEditorScreen(
                viewModel = viewModel,
                courseId = courseId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("profile") {
            val user = currentUser
            if (user != null) {
                ProfileScreen(
                    viewModel = viewModel,
                    user = user,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
