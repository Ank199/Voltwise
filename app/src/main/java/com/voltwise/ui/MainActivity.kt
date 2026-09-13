package com.voltwise.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.voltwise.service.BatteryMonitorService
import com.voltwise.ui.screens.*
import com.voltwise.ui.theme.VoltwiseTheme
import com.voltwise.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        startForegroundService(Intent(this, BatteryMonitorService::class.java))
        setContent { 
            VoltwiseTheme { 
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1200L)
                    showSplash = false
                }
                if (showSplash) {
                    SplashScreen()
                } else {
                    VoltwiseApp() 
                }
            } 
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(24.dp),
                color = Panel,
                border = BorderStroke(2.dp, Mint)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = Mint,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "voltwise",
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = (-1).sp,
                    color = Color.White
                )
                Text(
                    "X",
                    color = Mint,
                    fontWeight = FontWeight.Light,
                    fontSize = 28.sp
                )
            }
            Text(
                "Battery Intelligence Lab",
                color = Muted,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object CommandCenter : Screen("command_center", "Overview", Icons.Rounded.GridView)
    object Lab : Screen("lab", "Lab", Icons.Rounded.Science)
    object Timeline : Screen("timeline", "Activity", Icons.Rounded.Timeline)
    object Forecast : Screen("forecast", "Forecast", Icons.Rounded.AutoGraph)
    object History : Screen("history", "History", Icons.Rounded.History)
    object Insights : Screen("insights", "Insights", Icons.Rounded.BarChart)
    object Doctor : Screen("doctor", "Doctor", Icons.Rounded.HealthAndSafety)
    object Missions : Screen("missions", "Missions", Icons.Rounded.EmojiEvents)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Tune)
}
val bottomNavItems = listOf(Screen.CommandCenter, Screen.Lab, Screen.Timeline, Screen.Insights, Screen.Settings)

@Composable
fun VoltwiseApp() {
    val settings: SettingsViewModel = viewModel()
    val languageState by settings.uiState.collectAsState()
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val currentScreen = listOf(
        Screen.CommandCenter,
        Screen.Lab,
        Screen.Timeline,
        Screen.Forecast,
        Screen.History,
        Screen.Insights,
        Screen.Doctor,
        Screen.Missions,
        Screen.Settings
    ).firstOrNull { it.route == route }
    Scaffold(containerColor = Ink, contentColor = Color(0xFFF0F4F8), topBar = {
        if (route != null && route != Screen.CommandCenter.route) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = {
                        if (!navController.navigateUp()) {
                            navController.navigate(Screen.CommandCenter.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Mint)
                }
                Text(
                    screenTitle(languageState.languageCode, route, currentScreen?.title ?: ""),
                    color = Color(0xFFF0F4F8),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }
        }
    }, bottomBar = {
        Surface(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            color = Color(0xE6141C27), // Sleek frosted glass transparent look
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, PanelBorder),
            shadowElevation = 12.dp
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = route == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, null, modifier = Modifier.size(22.dp)) },
                        label = { Text(screenTitle(languageState.languageCode, screen.route, screen.title), fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Mint,
                            selectedTextColor = Mint,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }) { padding ->
        var swipeAmount by remember { mutableStateOf(0f) }
        NavHost(
            navController,
            startDestination = Screen.CommandCenter.route,
            modifier = Modifier
                .padding(padding)
                .pointerInput(route) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeAmount = 0f },
                        onHorizontalDrag = { _, dragAmount -> swipeAmount += dragAmount },
                        onDragEnd = {
                            val currentIndex = bottomNavItems.indexOfFirst { it.route == route }
                            if (currentIndex != -1 && kotlin.math.abs(swipeAmount) > 90f) {
                                val targetIndex = if (swipeAmount < 0) currentIndex + 1 else currentIndex - 1
                                bottomNavItems.getOrNull(targetIndex)?.let { screen ->
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                            swipeAmount = 0f
                        },
                        onDragCancel = { swipeAmount = 0f }
                    )
                }
        ) {
            composable(Screen.CommandCenter.route) { CommandCenterScreen(navController) }
            composable(Screen.Lab.route) { LabScreen(navController) }
            composable(Screen.Timeline.route) { TimelineScreen(navController) }
            composable(Screen.Forecast.route) { ForecastScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Insights.route) { InsightsScreen() }
            composable(Screen.Doctor.route) { DoctorScreen() }
            composable(Screen.Missions.route) { MissionsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
