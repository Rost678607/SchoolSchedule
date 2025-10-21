package com.rostik.schoolapp.ui

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.MaterialTheme
import com.rostik.schoolapp.model.data.LessonManager
import com.rostik.schoolapp.model.data.SpecificLessonManager
import com.rostik.schoolapp.model.data.TimeSchemeManager

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Главная", Icons.Outlined.Home)
    object Schedule : Screen("schedule", "Расписание", Icons.Outlined.DateRange)
    object Settings : Screen("settings", "Настройки", Icons.Outlined.Settings)
    object LessonsListSettings : Screen("settings/lessons_list", "Список уроков")
    object LessonsSettings : Screen("settings/lessons", "Расписание уроков")
    object TimeSchemeSettings : Screen("settings/time_scheme", "Расписание звонков")
    object Homework : Screen("homework", "Домашнее задание")
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigableApp(context: Context) {
    // Инициализация менеджеров данных
    val lessonManager = LessonManager(context)
    val specificLessonManager = SpecificLessonManager(context)
    val timeSchemeManager = TimeSchemeManager(context)

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Определение заголовка в зависимости от текущего маршрута
    val title = when {
        currentRoute == Screen.Home.route -> Screen.Home.title
        currentRoute == Screen.Schedule.route -> Screen.Schedule.title
        currentRoute == Screen.Settings.route -> Screen.Settings.title
        currentRoute == Screen.LessonsListSettings.route -> Screen.LessonsListSettings.title
        currentRoute == Screen.LessonsSettings.route -> Screen.LessonsSettings.title
        currentRoute == Screen.TimeSchemeSettings.route -> Screen.TimeSchemeSettings.title
        currentRoute == Screen.Homework.route -> Screen.Homework.title
        else -> { "" }
    }

    Scaffold(
        topBar = {
            if (currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Schedule.route,
                    Screen.Settings.route,
                    Screen.LessonsListSettings.route,
                    Screen.LessonsSettings.route,
                    Screen.TimeSchemeSettings.route,
                    Screen.Homework.route
                )
            ) {
                TopAppBar(
                    title = { Text(text = title) },
                    navigationIcon = {
                        if (currentRoute !in listOf(Screen.Home.route, Screen.Schedule.route, Screen.Settings.route)) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Назад",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        bottomBar = { BottomNavigationBar(navController) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(
                route = Screen.Home.route,
                exitTransition = { fadeOut(tween(0)) },
                enterTransition = { fadeIn(tween(0)) },
                popEnterTransition = { fadeIn(tween(0)) },
                popExitTransition = { fadeOut(tween(0)) }
            ) { HomeScreen(navController) }
            composable(
                route = Screen.Schedule.route,
                enterTransition = { fadeIn(tween(0)) },
                exitTransition = { fadeOut(tween(0)) },
                popEnterTransition = { fadeIn(tween(0)) },
                popExitTransition = { fadeOut(tween(0)) }
            ) { ScheduleScreen() }
            composable(
                route = Screen.Settings.route,
                enterTransition = { fadeIn(tween(0)) },
                exitTransition = { fadeOut(tween(0)) },
                popEnterTransition = { fadeIn(tween(0)) },
                popExitTransition = { fadeOut(tween(0)) }
            ) { SettingsScreen(navController) }
            composable(
                route = Screen.LessonsListSettings.route,
                enterTransition = { fadeIn(tween(0)) },
                exitTransition = { fadeOut(tween(0)) },
                popEnterTransition = { fadeIn(tween(0)) },
                popExitTransition = { fadeOut(tween(0)) }
            ) { LessonsListSettingsScreen(lessonManager = lessonManager) }
            composable(
                route = Screen.LessonsSettings.route,
                enterTransition = { fadeIn(tween(0)) },
                exitTransition = { fadeOut(tween(0)) },
                popEnterTransition = { fadeIn(tween(0)) },
                popExitTransition = { fadeOut(tween(0)) }
            ) { LessonsSettingsScreen(specificLessonManager = specificLessonManager, lessonManager = lessonManager) }
            composable(
                route = Screen.TimeSchemeSettings.route,
                enterTransition = { fadeIn(tween(0)) },
                exitTransition = { fadeOut(tween(0)) },
                popEnterTransition = { fadeIn(tween(0)) },
                popExitTransition = { fadeOut(tween(0)) }
            ) { TimeSchemeSettingsScreen(timeSchemeManager = timeSchemeManager) }
            composable(
                route = Screen.Homework.route,
                enterTransition = { fadeIn(tween(0)) },
                exitTransition = { fadeOut(tween(0)) },
                popEnterTransition = { fadeIn(tween(0)) },
                popExitTransition = { fadeOut(tween(0)) }
            ) { HomeworkScreen(lessonManager = lessonManager) }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Home, Screen.Schedule, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon!!,
                        contentDescription = screen.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}