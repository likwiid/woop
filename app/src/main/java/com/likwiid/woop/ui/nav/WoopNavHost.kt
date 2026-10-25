// SPDX-License-Identifier: GPL-3.0-or-later

@file:OptIn(ExperimentalLayoutApi::class)

package com.likwiid.woop.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.likwiid.woop.AppContainer
import com.likwiid.woop.reminders.Notifications
import com.likwiid.woop.ui.create.CreateWoopScreen
import com.likwiid.woop.ui.detail.GoalDetailScreen
import com.likwiid.woop.ui.learn.LearnScreen
import com.likwiid.woop.ui.onboarding.OnboardingScreen
import com.likwiid.woop.ui.reminders.RemindersScreen
import com.likwiid.woop.ui.review.ReviewScreen
import com.likwiid.woop.ui.settings.SettingsScreen
import com.likwiid.woop.ui.theme.LocalReduceMotion
import com.likwiid.woop.ui.today.TodayScreen
import kotlinx.coroutines.flow.MutableStateFlow

object Routes {
    const val ONBOARDING = "onboarding"
    const val TODAY = "today"
    const val REMINDERS = "reminders"
    const val LEARN = "learn"
    const val SETTINGS = "settings"
    const val CREATE = "create?draftId={draftId}"
    const val GOAL = "goal/{goalId}"
    const val REVIEW = "review/{goalId}"

    fun create(draftId: Long? = null) = "create?draftId=${draftId ?: -1}"
    fun goal(id: Long) = "goal/$id"
    fun review(id: Long) = "review/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.TODAY, "Today", Icons.Rounded.WbSunny),
    Tab(Routes.REMINDERS, "Reminders", Icons.Rounded.NotificationsActive),
    Tab(Routes.LEARN, "Science", Icons.Rounded.Science),
)

@Composable
fun WoopNavHost(container: AppContainer, pendingDest: MutableStateFlow<String?>) {
    val nav = rememberNavController()
    val startDestination = remember { if (container.prefs.onboardingDone.value) Routes.TODAY else Routes.ONBOARDING }
    val dest by pendingDest.collectAsStateWithLifecycle()
    val reduceMotion = LocalReduceMotion.current

    LaunchedEffect(dest) {
        val target = dest ?: return@LaunchedEffect
        pendingDest.value = null
        if (!container.prefs.onboardingDone.value) return@LaunchedEffect
        when {
            target == Notifications.DEST_CREATE -> nav.navigate(Routes.create())
            // Clear whatever was left open, so Back from a shortcut always lands on Today.
            target.startsWith("review/") || target.startsWith("goal/") -> nav.navigate(target) { popUpTo(Routes.TODAY) }
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showTabs = tabs.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (showTabs) WoopTabs(nav, currentRoute) },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = startDestination,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
            enterTransition = { if (reduceMotion) EnterTransition.None else fadeIn(tween(200)) },
            exitTransition = { if (reduceMotion) ExitTransition.None else fadeOut(tween(150)) },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = { startWoop ->
                        container.prefs.setOnboardingDone(true)
                        nav.navigate(Routes.TODAY) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                        if (startWoop) nav.navigate(Routes.create())
                    },
                )
            }
            composable(Routes.TODAY) {
                TodayScreen(
                    onNewWoop = { nav.navigate(Routes.create()) },
                    onContinueDraft = { nav.navigate(Routes.create(it)) },
                    onOpenGoal = { nav.navigate(Routes.goal(it)) },
                    onCheckIn = { nav.navigate(Routes.review(it)) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                    onLearn = { nav.switchTab(Routes.LEARN) },
                )
            }
            composable(Routes.REMINDERS) {
                RemindersScreen(onOpenGoal = { nav.navigate(Routes.goal(it)) })
            }
            composable(Routes.LEARN) {
                LearnScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    onBack = { nav.popBackStack() },
                    onReplayIntro = {
                        container.prefs.setOnboardingDone(false)
                        nav.navigate(Routes.ONBOARDING) { popUpTo(nav.graph.id) { inclusive = true } }
                    },
                )
            }
            composable(
                Routes.CREATE,
                arguments = listOf(navArgument("draftId") { type = NavType.LongType; defaultValue = -1L }),
            ) {
                CreateWoopScreen(
                    onClose = { nav.popBackStack() },
                    onOpenGoal = { id ->
                        nav.popBackStack()
                        nav.navigate(Routes.goal(id))
                    },
                    onStartAnother = {
                        nav.popBackStack()
                        nav.navigate(Routes.create())
                    },
                )
            }
            composable(Routes.GOAL, arguments = listOf(navArgument("goalId") { type = NavType.LongType })) {
                GoalDetailScreen(
                    onBack = { nav.popBackStack() },
                    onCheckIn = { nav.navigate(Routes.review(it)) },
                )
            }
            composable(Routes.REVIEW, arguments = listOf(navArgument("goalId") { type = NavType.LongType })) {
                ReviewScreen(onClose = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(Routes.TODAY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun WoopTabs(nav: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { if (currentRoute != tab.route) nav.switchTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}
